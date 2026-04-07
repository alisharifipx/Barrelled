package au.com.barrelled;

public class ExportItem {

    private final String fileName;
    private final String exportName;
    private final boolean isDefault;
    private final boolean isTypeOnly;
    private boolean selected;

    public ExportItem(String fileName, String exportName, boolean isDefault,  boolean isTypeOnly) {
        this.fileName = fileName;
        this.exportName = exportName;
        this.isDefault = isDefault;
        this.isTypeOnly = isTypeOnly;
        this.selected = true;
    }

    public String getFileName() {
        return fileName;
    }

    public String getExportName() {
        return exportName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isTypeOnly() {
        return isTypeOnly;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
