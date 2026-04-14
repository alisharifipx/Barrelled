package au.com.barrelled;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextBrowseFolderListener;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class BarrelledSettingsConfigurable implements Configurable {

    private final Project project;
    private JRadioButton tsRadio;
    private JRadioButton jsRadio;
    private JRadioButton sameDirRadio;
    private JRadioButton relativePathRadio;
    private JRadioButton customPathRadio;
    private JTextField relativePathField;
    private TextFieldWithBrowseButton customPathField;

    public BarrelledSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Barrelled";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        BarrelledSettings settings = BarrelledSettings.getInstance(project);

        JPanel panel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        gbc.insets = JBUI.insetsTop(4);
        panel.add(new JLabel("Generated barrel file extension:"), gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(6);
        tsRadio = new JRadioButton("TypeScript (index.ts)");
        panel.add(tsRadio, gbc);

        gbc.gridy++;
        jsRadio = new JRadioButton("JavaScript (index.js)");
        panel.add(jsRadio, gbc);

        ButtonGroup extensionGroup = new ButtonGroup();
        extensionGroup.add(tsRadio);
        extensionGroup.add(jsRadio);

        if (".ts".equals(settings.getFileExtension())) {
            tsRadio.setSelected(true);
        } else {
            jsRadio.setSelected(true);
        }

        gbc.gridy++;
        gbc.insets = JBUI.insets(12, 0, 8, 0);
        panel.add(new JSeparator(), gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(10);
        panel.add(new JLabel("Output location for the generated barrel file:"), gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(8);
        sameDirRadio = new JRadioButton("Same directory as source files");
        panel.add(sameDirRadio, gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(6);
        relativePathRadio = new JRadioButton("Relative path to the selected folder:");
        panel.add(relativePathRadio, gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(2);
        relativePathField = new JTextField(settings.getRelativePath());
        panel.add(relativePathField, gbc);

        gbc.gridy++;
        JLabel hint = new JLabel("<html><font color='gray'>&nbsp;&nbsp;&nbsp;e.g. ../ or ../../ or ../src/</font></html>");
        panel.add(hint, gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(8);
        customPathRadio = new JRadioButton("Custom folder within project:");
        panel.add(customPathRadio, gbc);

        gbc.gridy++;
        gbc.insets = JBUI.insetsTop(2);
        customPathField = new TextFieldWithBrowseButton();
        String storedPath = settings.getCustomPath();
        String basePath = project.getBasePath() != null ? project.getBasePath() : "";
        String displayPath = storedPath.isEmpty() ? basePath : basePath + "/" + storedPath;
        customPathField.setText(displayPath);
        VirtualFile projectRoot = basePath.isEmpty() ? null : LocalFileSystem.getInstance().findFileByPath(basePath);
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        if (projectRoot != null) {
            descriptor = descriptor.withRoots(projectRoot).withTreeRootVisible(true);
        }

        customPathField.addBrowseFolderListener(new TextBrowseFolderListener(descriptor, project));
        panel.add(customPathField, gbc);
        gbc.gridx = 1;
        gbc.gridy++;
        JLabel customPathHint = new JLabel("<html><font color='gray'>&nbsp;&nbsp;&nbsp;e.g. app/frontend/</font></html>");
        panel.add(customPathHint, gbc);

        ButtonGroup directoryGroup = new ButtonGroup();
        directoryGroup.add(sameDirRadio);
        directoryGroup.add(relativePathRadio);
        directoryGroup.add(customPathRadio);

        Runnable updateFields = () -> {
            relativePathField.setEnabled(relativePathRadio.isSelected());
            customPathField.setEnabled(customPathRadio.isSelected());
        };

        sameDirRadio.addActionListener(e -> updateFields.run());
        relativePathRadio.addActionListener(e -> updateFields.run());
        customPathRadio.addActionListener(e -> updateFields.run());

        switch (settings.getOutputMode()) {
            case SAME_DIR -> {
                sameDirRadio.setSelected(true);
            }
            case RELATIVE ->  {
                relativePathRadio.setSelected(true);
            }
            case CUSTOM_PATH ->  {
                customPathRadio.setSelected(true);
            }
        }

        updateFields.run();

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(panel, BorderLayout.NORTH);

        return wrapper;
    }

    @Override
    public boolean isModified() {
        BarrelledSettings settings = BarrelledSettings.getInstance(project);
        String currentExt = tsRadio.isSelected() ? ".ts" : ".js";
        OutputMode currentMode = sameDirRadio.isSelected() ? OutputMode.SAME_DIR
            : relativePathRadio.isSelected() ? OutputMode.RELATIVE
              : OutputMode.CUSTOM_PATH;

        return currentMode != settings.getOutputMode()
            || !relativePathField.getText().equals(settings.getRelativePath())
            || !customPathField.getText().equals(settings.getCustomPath())
            || !currentExt.equals(settings.getFileExtension());
    }

    @Override
    public void apply() {
        BarrelledSettings settings = BarrelledSettings.getInstance(project);

        if (sameDirRadio.isSelected()) {
            settings.setOutputMode(OutputMode.SAME_DIR);
        } else if (relativePathRadio.isSelected()) {
            settings.setOutputMode(OutputMode.RELATIVE);
        } else {
            settings.setOutputMode(OutputMode.CUSTOM_PATH);
        }

        settings.setRelativePath(relativePathField.getText().trim());

        String abs = customPathField.getText().trim();
        String base = project.getBasePath();
        String rel = (base != null && abs.startsWith(base))
            ? abs.substring(base.length()).replaceAll("^/+", "")
            : abs;
        settings.setCustomPath(rel);
        settings.setFileExtension(tsRadio.isSelected() ? ".ts" : ".js");
    }

    @Override
    public void reset() {
        BarrelledSettings settings = BarrelledSettings.getInstance(project);

        switch(settings.getOutputMode()) {
            case SAME_DIR -> {
                sameDirRadio.setSelected(true);
            }
            case RELATIVE -> {
                relativePathRadio.setSelected(true);
            }
            case CUSTOM_PATH -> {
                customPathRadio.setSelected(true);
            }
        }

        relativePathField.setText(settings.getRelativePath());

        String storedPath = settings.getCustomPath();
        String base = project.getBasePath() != null ? project.getBasePath() : "";
        String displayPath = storedPath.isEmpty() ? base : base + "/" + storedPath;
        customPathField.setText(displayPath);

        if (".ts".equals(settings.getFileExtension())) {
            tsRadio.setSelected(true);
        } else {
            jsRadio.setSelected(true);
        }
    }
}
