package au.com.barrelled;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class BarrelFileMerger {

    public static String merge(String existingContent, String newContent) {
        // Parse both files using ExportScanner's barrel parser
        LinkedHashMap<String, LinkedHashSet<String>> existing = ExportScanner.scanBarrelFile(existingContent);
        LinkedHashMap<String, LinkedHashSet<String>> incoming = ExportScanner.scanBarrelFile(newContent);

        // Merge: existing paths get new names added, new paths are appended
        for (Map.Entry<String, LinkedHashSet<String>> entry : incoming.entrySet()) {
            existing.computeIfAbsent(entry.getKey(), k -> new LinkedHashSet<>())
                .addAll(entry.getValue());
        }

        // Preserve any non-export lines from existing file (comments, blank lines etc)
        StringBuilder result = new StringBuilder();
        for (String line : existingContent.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("export")) {
                result.append(line).append("\n");
            }
        }

        // Write merged export lines
        for (Map.Entry<String, LinkedHashSet<String>> entry : existing.entrySet()) {
            List<String> names = new ArrayList<>(entry.getValue());
            result.append("export { ")
                .append(String.join(", ", names))
                .append(" } from '")
                .append(entry.getKey())
                .append("';\n");
        }

        return result.toString();
    }
}
