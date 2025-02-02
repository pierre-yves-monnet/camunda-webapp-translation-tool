package org.camunda.webapptranslation.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SynchroParams {

    public static final String PLEASE_TRANSLATE_THE_SENTENCE = "_PLEASETRANSLATETHESENTENCE";
    public static final String PLEASE_VERIFY_THE_SENTENCE = "_PLEASEVERIFYTHESENTENCE";
    public static final String PLEASE_VERIFY_THE_SENTENCE_REFERENCE = "_PLEASEVERIFYTHESENTENCE_REFERENCE";
    private final List<String> msgErrors = new ArrayList<>();
    private boolean usage = false;
    private String referenceLanguage = "en";
    private File referenceFolder = null;
    private File translationFolder = null;
    private File optimizeFolder = null;
    /**
     * If not null, then only this language is completed (the -c option must be set too)
     */
    private String onlyCompleteOneLanguage = null;
    private DETECTION detection = DETECTION.SYNTHETIC;
    private COMPLETION completion = COMPLETION.NO;
    private REPORT report = REPORT.STDOUT;
    private String googleAPIKey;
    private int limitNumberGoogleTranslation = 100;

    /**
     * Static to be use in lambda
     *
     * @param msg message to print
     */
    private static void print(String msg) {
        System.out.println(msg);
    }

    /**
     * Explore the arguments to fulfil parameters
     *
     * @param args arguments
     */
    public void explore(String[] args) {
        int i = 0;
        int parameterCommand = 0;

        while (i < args.length) {
            if (("-d".equals(args[i]) || "--detect".equals(args[i])) && i < args.length - 1) {
                try {
                    detection = DETECTION.valueOf(args[i + 1]);
                } catch (Exception e) {
                    print("-d <" + DETECTION.NO + "|" + DETECTION.SYNTHETIC + "|" + DETECTION.FULL + "> detection and comparaison. Default is " + DETECTION.SYNTHETIC);
                }
                i += 2;
            } else if (("-l".equals(args[i]) || "--language".equals(args[i])) && i < args.length - 1) {
                onlyCompleteOneLanguage = args[i + 1];
                i += 2;
            } else if (("-c".equals(args[i]) || "--completion".equals(args[i])) && i < args.length - 1) {
                try {
                    completion = COMPLETION.valueOf(args[i + 1]);
                } catch (Exception e) {
                    print("-d <" + COMPLETION.NO + "|" + COMPLETION.KEYS + "|" + COMPLETION.TRANSLATION + "> Complete each dictionary. Default is " + COMPLETION.NO);
                }
                i += 2;
            } else if (("-g".equals(args[i]) || "--googleAPIKey".equals(args[i])) && i < args.length - 1) {
                googleAPIKey = args[i + 1];
                i += 2;
            } else if (("--limiteGoogleAPIKey".equals(args[i])) && i < args.length - 1) {
                try {
                    limitNumberGoogleTranslation = Integer.parseInt(args[i + 1]);
                } catch (Exception e) {
                    print("-limiteGoogleAPIKey <number>");

                }
                i += 2;
            } else if ("-u".equals(args[i]) || "--usage".equals(args[i])) {
                usage = true;
                i++;
            } else if (("-r".equals(args[i]) || "--report".equals(args[i])) && i < args.length - 1) {
                try {
                    report = REPORT.valueOf(args[i + 1]);
                } catch (Exception e) {
                    print("-r <" + REPORT.STDOUT + "|" + REPORT.LOGGER + "> accepted");
                }
                i += 2;
            } else {

                // Next args:
                // REFERENCE_FOLDER TRANSLATION_DIRECTORY OPTIMIZE_DIRECTORY + referenceLanguage
                if (parameterCommand == 0)
                    referenceFolder = getFile(args[i]);
                if (parameterCommand == 1)
                    translationFolder = getFile(args[i]);
                if (parameterCommand == 2)
                    optimizeFolder = getFile(args[i]);
                if (parameterCommand == 3)

                    referenceLanguage = args[i];
                parameterCommand++;
                i++;
            }
        }

        if (referenceFolder == null)
            msgErrors.add("No REFERENCE_FOLDER provided");
        if (translationFolder == null)
            msgErrors.add("No TRANSLATION_FOLDER Folder provided");
        if (optimizeFolder == null)
            msgErrors.add("No OPTIMIZE_FOLDER Folder provided");

    }

    public DETECTION getDetection() {
        return detection;
    }

    public boolean isUsage() {
        return usage;
    }

    /**
     * if an error is detected in parameters, then return true. Some parameters, like the folder where
     * all translations are stored, are mandatory.
     *
     * @return true is an error is detected in parameters
     */
    public boolean isError() {
        return !msgErrors.isEmpty();
    }

    public COMPLETION getCompletion() {
        return completion;
    }

    public String getReferenceLanguage() {
        return referenceLanguage;
    }

    public File getReferenceFolder() {
        return referenceFolder;
    }

    public File getTranslationFolder() {
        return translationFolder;
    }

    public File getOptimizeFolder() {
        return optimizeFolder;
    }

    public REPORT getReport() {
        return report;
    }

    public String getOnlyCompleteOneLanguage() {
        return onlyCompleteOneLanguage;
    }

    public String getGoogleAPIKey() {
        return googleAPIKey;
    }

    public int getLimitNumberGoogleTranslation() {
        return limitNumberGoogleTranslation;
    }

    /**
     * print the current options detected
     */
    public void printOptions() {
        print(" REFERENCE_FOLDER to study: " + getReferenceFolder().toString());
        print(" TRANSLATION_FOLDER to study: " + getTranslationFolder().toString());
        print(" OPTIMIZE_FOLDER to study: " + getOptimizeFolder().toString());
        print(" Reference language: " + getReferenceLanguage());
        print(" Detection: " + getDetection());
        print(" Completion: " + getCompletion());
        if (getGoogleAPIKey() != null) {
            print(" GoogleAPIKey: " + getGoogleAPIKey());
            print(" Maximum number of Google  translation: " + getLimitNumberGoogleTranslation());
        }
        if (getOnlyCompleteOneLanguage() != null)
            print(" Only one language: " + getOnlyCompleteOneLanguage());

        print(" Report: " + getReport());

    }

    /**
     * print the usage
     */
    public void printUsage() {
        print("Usage: SynchroTranslation [options] REFERENCE_FOLDER TRANSLATION_FOLDER OPTIMIZE_FOLDER [ReferenceLanguage]");
        print(" REFERENCE_FOLDER is the project https://github.com/camunda/camunda-bpm-platform");
        print(" TRANSLATION_FOLDER is the project https://github.com/camunda/camunda-webapp-translations");
        print(" Subfolders contains a list of .json files. Each file is a language (de.json). The reference language contains all references. Each language is controlled from this reference to detect the missing keys.");
        print("Options:");
        print(" -d|--detect <" + DETECTION.NO + "|" + DETECTION.SYNTHETIC + "|" + DETECTION.FULL + "> detection and comparison. Default is " + DETECTION.SYNTHETIC);

        print(" -c|--complete: <" + COMPLETION.NO + "|" + COMPLETION.KEYS + "|" + COMPLETION.TRANSLATION + "> missing keys are created in each the dictionary. Current file are saved with <language>_<date>.txt and a new file is created. Missing keys are suffixed with '" + PLEASE_TRANSLATE_THE_SENTENCE
                + "'. With " + COMPLETION.TRANSLATION + ", dictionary are exploded to get a good translation. Default is " + COMPLETION.NO);
        print(" -g|--googleAPIKey <GoogleAPIKey>: Give a Google API Key to translate the missing keys");
        print(" --limiteGoogleAPIKey <Number of Translation>: Set the limit. Default is 100");
        print(" -l|--language <language>: if set, only this language is analysed / completed");

        print(" -r|--report  <" + REPORT.STDOUT + "|" + REPORT.LOGGER + ">");
        print(" TranslationFolder is the root folder which contains all translations (cloned from https://github.com/camunda/camunda-webapp-translations)");
    }

    public void printError() {
        print("Error:");
        msgErrors.forEach(SynchroParams::print);
        print("");
    }


    /**
     * Get the File folder from the string
     *
     * @param folderSt Folder as a String, given as the parameter
     * @return the File or null if the folder does not exist
     */
    private File getFile(String folderSt) {
        try {
            File folder = new File(folderSt);
            if (folder.isDirectory())
                return folder;
            print("parameter [" + folderSt + "] does not point to a folder");
            return null;
        } catch (Exception e) {
            print("parameter [" + folderSt + "] does not point to a folder");
            return null;
        }
    }

    public enum DETECTION {NO, SYNTHETIC, FULL}

    public enum COMPLETION {NO, KEYS, TRANSLATION}

    public enum REPORT {STDOUT, LOGGER}
}
