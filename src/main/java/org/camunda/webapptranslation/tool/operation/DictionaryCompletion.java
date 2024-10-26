package org.camunda.webapptranslation.tool.operation;

import org.camunda.webapptranslation.tool.SynchroParams;
import org.camunda.webapptranslation.tool.WebApplication;
import org.camunda.webapptranslation.tool.app.AppDictionary;
import org.camunda.webapptranslation.tool.app.AppPilot;
import org.camunda.webapptranslation.tool.app.AppTimeTracker;
import org.camunda.webapptranslation.tool.report.ReportInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/* -------------------------------------------------------------------- */
/*                                                                      */
/* Operation Completion                                  */
/*                                                                      */
/* Check all missing keys, and add a key/value in the dictionary.*/
/*                                                                      */
/* -------------------------------------------------------------------- */

public class DictionaryCompletion extends Operation {

    /**
     * Do the completion on each dictionary
     *
     * @param synchroParams parameter object
     * @param report        report object
     */
    public void completion(Set<String> expectedLanguages,
                           WebApplication webApplication,
                           AppDictionary referenceDictionary,
                           EncyclopediaUniversal encyclopediaUniversal,
                           List<Proposal> listProposals,
                           SynchroParams synchroParams,
                           ReportInt report) {

        // check each dictionary
        report.info(AppPilot.class, "----- Application " + webApplication.applicationName);


        for (String language : expectedLanguages.stream().sorted().collect(Collectors.toList())) {
            if (synchroParams.getOnlyCompleteOneLanguage() != null && !synchroParams.getOnlyCompleteOneLanguage().equals(language))
                continue;

            if (language.equals(referenceDictionary.getLanguage())) {
                report.info(AppPilot.class, headerLanguage(language) + "Referential");
                continue;
            }

            AppDictionary appDictionary = new AppDictionary(webApplication.translationFolder, language);
            report.info(AppPilot.class, "  sourceFile [" + referenceDictionary.getFileName()+"]");
            report.info(AppPilot.class, "  destinationFile [" + appDictionary.getFileName()+"]");


            //----------------  Read and complete
            AppTimeTracker timeTrackerReadDictionary = AppTimeTracker.getTimeTracker("readDictionary");

            // read the dictionary
            if (appDictionary.existFile()) {
                timeTrackerReadDictionary.start();
                boolean allIsOk = appDictionary.read(report);
                timeTrackerReadDictionary.stop();
                if (!allIsOk) {

                    // file exist, but not possible to read: better to have a look here
                    report.severe(AppPilot.class, "File [" + appDictionary.getFile().getAbsolutePath() + "] exist, but impossible to read it: check it");
                    continue;
                }
            }
            int beforePurge = appDictionary.getDictionary().size();
            // purge all TRANSLATE key
            AppTimeTracker timeTrackerDictionaryPreparation = AppTimeTracker.getTimeTracker("dictionaryPreparation");
            timeTrackerDictionaryPreparation.start();
            appDictionary.getDictionary()
                    .entrySet()
                    .removeIf(entry -> (entry.getKey().endsWith(SynchroParams.PLEASE_TRANSLATE_THE_SENTENCE)
                            || entry.getKey().endsWith(SynchroParams.PLEASE_VERIFY_THE_SENTENCE)
                            || entry.getKey().endsWith(SynchroParams.PLEASE_VERIFY_THE_SENTENCE_REFERENCE)
                    ));

            List<String> listReports = new ArrayList<>();
            if (appDictionary.getDictionary().size() != beforePurge) {
                listReports.add((beforePurge - appDictionary.getDictionary().size()) + " label keys purged");
                appDictionary.setModified();
            }

            // check and complete
            AppTimeTracker timeTrackerDictionaryCheckKeys = AppTimeTracker.getTimeTracker("dictionaryCheckKeys");
            timeTrackerDictionaryCheckKeys.start();
            DictionaryStatus dictionaryStatus = checkKeys(appDictionary, referenceDictionary);
            timeTrackerDictionaryCheckKeys.stop();

            listProposals.forEach(proposal -> proposal.setDictionaries(appDictionary, referenceDictionary, encyclopediaUniversal));

            if (dictionaryStatus.nbMissingKeys > 0) {

                dictionaryStatus.missingKeys
                        .forEach(key -> manageAddKey(dictionaryStatus, key, appDictionary, referenceDictionary, listProposals, report));

                listReports.add("Add " + dictionaryStatus.nbMissingKeys + " keys / proposition ( "
                        + dictionaryStatus.statisticPerProposer.entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue())
                        .collect(Collectors.joining(", ")) + ")");

                listReports.add(" Check in the dictionary keys ( "
                        + dictionaryStatus.statisticPerKeyAdditions.entrySet()
                        .stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue())
                        .collect(Collectors.joining(", ")) + ")");
            }

            if (dictionaryStatus.nbTooMuchKeys > 0) {
                listReports.add("Remove " + dictionaryStatus.nbMissingKeys + " keys");
                dictionaryStatus.tooMuchKeys.forEach(key -> appDictionary.removeKey(key));
            }
            if (dictionaryStatus.nbIncorrectKeyClass > 0) {
                listReports.add("Replace " + dictionaryStatus.nbMissingKeys + " keys");
                dictionaryStatus.incorrectClass
                        .forEach((key -> {
                            appDictionary.removeKey(key);
                            manageAddKey(dictionaryStatus, key, appDictionary, referenceDictionary, listProposals, report);
                        }));
            }
            if (listReports.isEmpty())
                listReports.add("Nothing done.");
            report.info(AppPilot.class,
                    headerLanguage(language)
                            + String.join(",", listReports));


            // -----------Write it
            if (appDictionary.isModified()) {
                AppTimeTracker timeTracker = AppTimeTracker.getTimeTracker("dictionaryWrite");
                timeTracker.start();
                boolean statusWrite = appDictionary.write(report);
                timeTracker.stop();

                if (statusWrite)
                    report.info(AppPilot.class, INDENTATION + "   " + "Dictionary written with success.");
                else
                    report.severe(AppPilot.class, INDENTATION + "   " + "Error writing dictionary.");
            }
        }
    }

    /**
     * Manage the way to add a key in the dictionary.
     *
     * @param key                 key to add
     * @param appDictionary       dictionary to add the key
     * @param referenceDictionary referential dictionary, then the value can be accessed
     * @param report              to report any issue
     */
    private void manageAddKey(DictionaryStatus dictionaryStatus, String key, AppDictionary appDictionary, AppDictionary referenceDictionary, List<Proposal> listProposals, ReportInt report) {
        AppTimeTracker timeTracker = AppTimeTracker.getTimeTracker("dictionaryAddKeys");
        timeTracker.start();

        Object valueReference = referenceDictionary.getDictionary().get(key);

        if (valueReference instanceof Long || valueReference instanceof Integer || valueReference instanceof Double) {
            // we add the key, no translation is needed
            if (valueReference instanceof Double) {
                Double valueReferenceDouble = (Double) valueReference;
                if (Math.round(valueReferenceDouble) == valueReferenceDouble)
                    valueReference = valueReferenceDouble.intValue();
            }
            appDictionary.addKey(key, valueReference);


        } else if (valueReference instanceof String) {
            String defaultProposition = (String) valueReference;
            String proposition = null;
            String referenceTranslation = null;

            if (!listProposals.isEmpty()) {
                proposition = getProposition(dictionaryStatus, key, listProposals, report);
                if (proposition != null) {
                    referenceTranslation = (String) valueReference;
                    defaultProposition = null;
                }
            }
            if (defaultProposition != null) {
                appDictionary.addKey(key + SynchroParams.PLEASE_TRANSLATE_THE_SENTENCE, defaultProposition);
                dictionaryStatus.addKey(SynchroParams.PLEASE_TRANSLATE_THE_SENTENCE);
            }
            if (proposition != null) {
                appDictionary.addKey(key + SynchroParams.PLEASE_VERIFY_THE_SENTENCE, proposition);
                dictionaryStatus.addKey(SynchroParams.PLEASE_VERIFY_THE_SENTENCE);
            }
            if (referenceTranslation != null) {
                appDictionary.addKey(key + SynchroParams.PLEASE_VERIFY_THE_SENTENCE_REFERENCE, referenceTranslation);
                dictionaryStatus.addKey(SynchroParams.PLEASE_VERIFY_THE_SENTENCE_REFERENCE);
            }

        } else {
            appDictionary.addKey(key + SynchroParams.PLEASE_TRANSLATE_THE_SENTENCE, referenceDictionary.getDictionary().get(key));
            dictionaryStatus.addKey(SynchroParams.PLEASE_TRANSLATE_THE_SENTENCE);
        }
        timeTracker.stop();

    }


    /**
     * Get a proposition.
     *
     * @param dictionaryStatus status to collect statistics
     * @param key              key to have a proposition
     * @return the proposition, null if no proposition can be done
     */
    private String getProposition(DictionaryStatus dictionaryStatus, String key, List<Proposal> listProposals, ReportInt report) {

        AppTimeTracker timeTracker = AppTimeTracker.getTimeTracker("dictionaryGetProposition");
        timeTracker.start();
        try {
            for (Proposal proposal : listProposals) {
                String proposition = proposal.calculateProposition(key, report);
                if (proposition != null) {
                    dictionaryStatus.addProposition(proposal.getName());
                    return proposition;
                }
            }
            return null;
        } catch (Exception e) {
            report.severe(DictionaryCompletion.class, "Error during getProposition " + e.toString());
            return null;
        } finally {
            timeTracker.stop();
        }
    }


}
