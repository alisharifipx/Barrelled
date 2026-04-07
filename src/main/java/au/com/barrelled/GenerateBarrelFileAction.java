package au.com.barrelled;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateBarrelFileAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        VirtualFile folder = event.getData(CommonDataKeys.VIRTUAL_FILE);

        if (project == null || folder == null || !folder.isDirectory()) {
            return;
        }

        BarrelledSettings settings = BarrelledSettings.getInstance(project);
        String ext = settings.getFileExtension();
        String barrelFileName = "index" + ext;

        List<ExportItem> allExports = new ArrayList<>();
        VirtualFile[] children = folder.getChildren();
        Arrays.sort(children, Comparator.comparing(VirtualFile::getName));

        for (VirtualFile child : children) {
            String name = child.getName();

            if (child.isDirectory()
                || name.equals(barrelFileName)
                || (!name.endsWith(".ts") && !name.endsWith(".tsx")
                    && !name.endsWith(".js") && !name.endsWith(".jsx"))) {
                continue;
            }

            allExports.addAll(ExportScanner.scan(child));
        }

        if (allExports.isEmpty()) {
            return;
        }

        VirtualFile outputFolder;
        switch (settings.getOutputMode()) {
            case RELATIVE   -> outputFolder = getVirtualFile(settings, folder);
            case CUSTOM_PATH -> {
                VirtualFile found = LocalFileSystem.getInstance()
                    .findFileByPath(settings.resolveCustomPath(project));
                outputFolder = (found != null && found.isDirectory()) ? found : folder;
            }
            default -> outputFolder = folder;
        }

        String sourceRelativePrefix = getRelativePath(outputFolder, folder);

        VirtualFile existingBarrel = outputFolder.findChild(barrelFileName);
        if (existingBarrel != null) {
            try {
                String existingContent = new String(existingBarrel.contentsToByteArray(), StandardCharsets.UTF_8);

                if (!existingContent.isBlank()) {
                    java.util.LinkedHashMap<String, java.util.LinkedHashSet<String>> alreadyExported =
                        ExportScanner.scanBarrelFile(existingContent);

                    for (ExportItem item : allExports) {
                        String path = sourceRelativePrefix + item.getFileName();
                        java.util.LinkedHashSet<String> namesForPath = alreadyExported.get(path);

                        if (namesForPath == null) {
                            item.setSelected(false);
                        } else {
                            String expectedToken = expectedBarrelToken(item);
                            item.setSelected(namesForPath.contains(expectedToken));
                        }
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        BarrelSelectDialog dialog = new BarrelSelectDialog(allExports);
        if (!dialog.showAndGet()) {
            return;
        }

        List<ExportItem> selected = dialog.getSelectedItems();
        if (selected.isEmpty()) {
            return;
        }

        LinkedHashMap<String, List<ExportItem>> grouped =  new LinkedHashMap<>();
        for (ExportItem item : selected) {
            grouped.computeIfAbsent(item.getFileName(), k -> new ArrayList<>()).add(item);
        }

        StringBuilder content = new StringBuilder();
        for (Map.Entry<String, List<ExportItem>> entry : grouped.entrySet()) {
            String fileName = entry.getKey();
            boolean isTypeScript = ext.equals(".ts");
            List<String> namedExports = getNamedExports(entry, isTypeScript);

            if (!namedExports.isEmpty()) {
                content.append("export { ")
                    .append(String.join(", ", namedExports))
                    .append(" } from '")
                    .append(sourceRelativePrefix)
                    .append(fileName)
                    .append("';\n");
            }
        }

        String finalContent = content.toString();
        WriteCommandAction.runWriteCommandAction(project, "Generate Barrel File", null, () -> {
            try {
                VirtualFile indexFile = outputFolder.findChild(barrelFileName);

                if (indexFile == null) {
                    indexFile = outputFolder.createChildData(this, barrelFileName);
                    indexFile.setBinaryContent(finalContent.getBytes(StandardCharsets.UTF_8));
                } else {
                    String existingContent = new String(indexFile.contentsToByteArray(), StandardCharsets.UTF_8);
                    String mergedContent = BarrelFileMerger.merge(existingContent, finalContent);
                    indexFile.setBinaryContent(mergedContent.getBytes(StandardCharsets.UTF_8));
                }

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    private static VirtualFile getVirtualFile(BarrelledSettings settings, VirtualFile folder) {
        String[] segments = settings.getRelativePath().trim().split("/");
        VirtualFile target = folder;
        for (String seg : segments) {
            if (seg.equals("..") && target.getParent() != null) {
                target = target.getParent();
            } else if (!seg.isEmpty() && !seg.equals(".")) {
                VirtualFile child = target.findChild(seg);
                if (child != null && child.isDirectory()) target = child;
            }
        }
        return target;
    }

    private static @NotNull List<String> getNamedExports(Map.Entry<String, List<ExportItem>> entry, boolean isTypeScript) {
        List<ExportItem> items = entry.getValue();

        List<String> valueExports = new ArrayList<>();
        List<String> typeExports = new ArrayList<>();

        for (ExportItem item : items) {
            if (item.isDefault()) {
                valueExports.addFirst("default as " + item.getExportName());
            } else if (item.isTypeOnly()) {
                // Skip entirely for JS. Type-only exports have no JS equivalent
                if (isTypeScript) {
                    typeExports.add("type " + item.getExportName());
                }
            } else {
                valueExports.add(item.getExportName());
            }
        }

        List<String> combined = new ArrayList<>();
        combined.addAll(valueExports);
        combined.addAll(typeExports);

        return combined;
    }

    private String getRelativePath(VirtualFile from, VirtualFile to) {
        String fromPath = from.getPath();
        String toPath = to.getPath();

        if (fromPath.equals(toPath)) return "./";

        String[] fromParts = fromPath.split("/");
        String[] toParts = toPath.split("/");

        int common = 0;
        while (common < fromParts.length && common < toParts.length
               && fromParts[common].equals(toParts[common])) {
            common++;
        }

        StringBuilder rel = new StringBuilder();

        int stepsUp = fromParts.length - common;
        rel.repeat("../", Math.max(0, stepsUp));

        for (int i = common; i < toParts.length; i++) {
            rel.append(toParts[i]).append("/");
        }

        if (!rel.toString().startsWith("..")) {
            rel.insert(0, "./");
        }

        return rel.toString();
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        VirtualFile virtualFile = event.getData(CommonDataKeys.VIRTUAL_FILE);

        event.getPresentation().setEnabledAndVisible(virtualFile != null && virtualFile.isDirectory());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static String expectedBarrelToken(ExportItem item) {
        if (item.isDefault()) {
            return "default as " + item.getExportName();
        }

        if (item.isTypeOnly()) {
            return "type " + item.getExportName();
        }

        return item.getExportName();
    }
}
