package au.com.barrelled;

public enum OutputMode {
    SAME_DIR,       // In the same directory as the ts/js files
    RELATIVE,       // Relative path from the selected folder e.g. ../../
    CUSTOM_PATH     // A fixed folder somewhere in the project
}
