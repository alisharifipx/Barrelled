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

    private final List<ExportItem> allItems;

    public BarrelSelectDialog(List<ExportItem> allItems) {
        super(true);
        this.allItems = allItems;
        setTitle("Generate Barrel File");
        init();
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
            JBLabel fileLabel = new JBLabel(entry.getKey() + ".ts");
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
