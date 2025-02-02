package org.camunda.webapptranslation.tool.operation;

import org.camunda.webapptranslation.tool.SynchroParams;
import org.camunda.webapptranslation.tool.WebApplication;
import org.camunda.webapptranslation.tool.app.AppDictionary;
import org.camunda.webapptranslation.tool.app.AppPilot;
import org.camunda.webapptranslation.tool.report.ReportInt;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* Detection                              */
/*                                                                      */
/* The operation detect any issues in the dictionary, and do not change them*/
/*                                                                      */
/* -------------------------------------------------------------------- */

public class DictionaryDetection extends Operation {


    private final static String INDENTATION_FULL = "      ";

    /**
     * Detection
     * Check:
     * 1/ all dictionary exists
     * 2/ the language is complete
     *
     * @param synchroParams access to parameters
     * @param report        report the status
     */
    public void detection(Set<String> expectedLanguages,
                          WebApplication webApplication,
                          AppDictionary referenceDictionary,
                          SynchroParams synchroParams,
                          ReportInt report) {

        report.info(DictionaryDetection.class, "----- Application " + webApplication.applicationName);

        // check each dictionary
        for (String language : expectedLanguages.stream().sorted().collect(Collectors.toList())) {
            if (synchroParams.getOnlyCompleteOneLanguage() != null && !synchroParams.getOnlyCompleteOneLanguage().equals(language))
                continue;

            if (language.equals(referenceDictionary.getLanguage())) {
                report.info(DictionaryDetection.class, headerLanguage(language) + "Referentiel");
                continue;
            }

            AppDictionary dictionary = new AppDictionary(webApplication.translationFolder, language);
            report.info(AppPilot.class, "  sourceFile [" + dictionary.getFileName()+"]");
            if (!dictionary.existFile()) {
                report.info(DictionaryDetection.class, headerLanguage(language) + "Not exist (" + referenceDictionary.getDictionary().size() + " missing keys)");
                continue;
            }
            // read the dictionary
            if (!dictionary.read(report)) {
                // error already reported
                continue;
            }
            DictionaryStatus dictionaryStatus = checkKeys(dictionary, referenceDictionary);
            List<String> listReports = new ArrayList<>();
            if (dictionaryStatus.nbMissingKeys > 0)
                listReports.add("Missing " + dictionaryStatus.nbMissingKeys + " keys");
            if (dictionaryStatus.nbTooMuchKeys > 0)
                listReports.add("Too much " + dictionaryStatus.nbTooMuchKeys + " keys");
            if (dictionaryStatus.nbIncorrectKeyClass > 0)
                listReports.add("Incorrect class " + dictionaryStatus.nbIncorrectKeyClass + " keys");

            if (listReports.isEmpty())
                report.info(DictionaryDetection.class, headerLanguage(language) + "OK");
            else {
                // report errors
                report.info(DictionaryDetection.class,
                        headerLanguage(language)
                                + String.join(",", listReports));
                if (synchroParams.getDetection() == SynchroParams.DETECTION.FULL) {
                    reportFullDectection(dictionaryStatus, report);
                }
            }

        } // end language
    }

    /**
     * Report the full details
     *
     * @param dictionaryStatus status containing all details
     * @param report           report
     */
    private void reportFullDectection(DictionaryStatus dictionaryStatus, ReportInt report) {
        if (dictionaryStatus.nbMissingKeys > 0) {
            report.info(DictionaryDetection.class, INDENTATION_FULL + "Missing keys: " + String.join(", ", dictionaryStatus.missingKeys));
        }
        if (dictionaryStatus.nbTooMuchKeys > 0) {
            report.info(DictionaryDetection.class, INDENTATION_FULL + "Too much keys: " + String.join(", ", dictionaryStatus.tooMuchKeys));
        }
        if (dictionaryStatus.nbIncorrectKeyClass > 0) {
            report.info(DictionaryDetection.class, INDENTATION_FULL + "Incorrect classes: " + String.join(", ", dictionaryStatus.incorrectClass));
        }


    }
}
