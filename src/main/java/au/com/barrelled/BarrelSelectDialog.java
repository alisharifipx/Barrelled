package au.com.barrelled;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BarrelSelectDialog extends DialogWrapper {

    private final BarrelledSettings settings;
    private final List<ExportItem> allItems;
    private JTextField relativePathField;

    public BarrelSelectDialog(BarrelledSettings settings, List<ExportItem> allItems) {
        super(true);
        this.settings = settings;
        this.allItems = allItems;
        setTitle("Generate Barrel File");
        init();
    }

    @Override
    protected @Nullable JComponent createNorthPanel() {
        if (settings.getOutputMode() != OutputMode.RELATIVE) {
            return null;
        }

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JBLabel label = new JBLabel("Relative output path from the selected directory:");
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(label);

        relativePathField = new JTextField(settings.getRelativePath());
        relativePathField.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(relativePathField);

        JBLabel hint = new JBLabel("e.g. ../, ../../shared, ./generated");
        hint.setForeground(UIManager.getColor("Label.disabledForeground"));
        hint.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(hint);

        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        LinkedHashMap<String, List<ExportItem>> grouped = new LinkedHashMap<>();
        for (ExportItem item : allItems) {
            grouped.computeIfAbsent(item.getFileName(), k -> new ArrayList<>()).add(item);
        }

        List<JCheckBox> allCheckboxes = new ArrayList<>();

        JCheckBox selectAll = new JCheckBox("Select all");
        selectAll.setSelected(allItems.stream().allMatch(ExportItem::isSelected));
        selectAll.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(selectAll);

        for (Map.Entry<String, List<ExportItem>> entry : grouped.entrySet()) {
            JBLabel fileLabel = new JBLabel(entry.getKey());
            fileLabel.setFont(fileLabel.getFont().deriveFont(Font.BOLD));
            fileLabel.setBorder(BorderFactory.createEmptyBorder(6, 0, 4, 0));
            fileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(fileLabel);

            for (ExportItem item : entry.getValue()) {
                JCheckBox checkbox = getJCheckBox(item, allCheckboxes, selectAll);
                allCheckboxes.add(checkbox);
                panel.add(checkbox);
            }
        }

        selectAll.addActionListener(e -> {
            boolean checked = selectAll.isSelected();
            for (JCheckBox checkbox : allCheckboxes) {
                checkbox.setSelected(checked);
            }

            for (ExportItem item : allItems) {
                item.setSelected(checked);
            }
        });

        JBScrollPane scrollPane = new JBScrollPane(panel);
        scrollPane.setPreferredSize(new Dimension(400, 450));

        return scrollPane;
    }

    @Override
    protected void doOKAction() {
        if (relativePathField != null) {
            settings.setRelativePath(relativePathField.getText().trim());
        }

        super.doOKAction();
    }

    private static @NotNull JCheckBox getJCheckBox(
        ExportItem item,
        List<JCheckBox> allCheckboxes,
        JCheckBox selectAll
    ) {
        String label = item.isDefault()
            ? item.getExportName() + " (default)"
            : item.getExportName();

        JCheckBox checkbox = new JCheckBox(label, item.isSelected());
        checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        checkbox.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        checkbox.addActionListener(e -> {
            item.setSelected(checkbox.isSelected());
            boolean allChecked = allCheckboxes.stream().allMatch(JCheckBox::isSelected);
            selectAll.setSelected(allChecked);
        });

        return checkbox;
    }

    public List<ExportItem> getSelectedItems() {
        return allItems.stream().filter(ExportItem::isSelected).toList();
    }
}
