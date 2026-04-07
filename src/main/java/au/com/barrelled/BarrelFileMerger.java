package au.com.barrelled;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BarrelFileMerger {

    private static final Pattern BARREL_EXPORT_BLOCK = Pattern.compile(
        "^export\\s*(?:type\\s*)?\\{[^}]+}\\s*from\\s*['\"]([^'\"]+)['\"];?[ \\t]*(?:\\R|\\z)",
        Pattern.MULTILINE
    );

    public static String merge(
        String existingContent,
        String newContent,
        Set<String> managedPaths
    ) {
        LinkedHashMap<String, LinkedHashSet<String>> incoming = ExportScanner.scanBarrelFile(newContent);

        // Preserve comments, whitespace, and export forms this run does not manage.
        StringBuilder result = new StringBuilder();
        if (!existingContent.isEmpty()) {
            Matcher matcher = BARREL_EXPORT_BLOCK.matcher(existingContent);
            int lastEnd = 0;

            while (matcher.find()) {
                result.append(existingContent, lastEnd, matcher.start());

                if (!managedPaths.contains(matcher.group(1))) {
                    result.append(matcher.group());
                }

                lastEnd = matcher.end();
            }

            result.append(existingContent.substring(lastEnd));
        }

        String preservedContent = result.toString()
            .replaceFirst("(?s)\\A(?:[ \\t]*\\R)+", "")
            .replaceFirst("(?s)(?:\\R[ \\t]*)+\\z", "");

        StringBuilder merged = new StringBuilder();
        if (!preservedContent.isEmpty()) {
            merged.append(preservedContent);
            if (!preservedContent.endsWith("\n")) {
                merged.append("\n");
            }
        }

        for (Map.Entry<String, LinkedHashSet<String>> entry : incoming.entrySet()) {
            List<String> names = new ArrayList<>(entry.getValue());
            merged.append("export { ")
                .append(String.join(", ", names))
                .append(" } from '")
                .append(entry.getKey())
                .append("';\n");
        }

        return merged.toString();
    }
}
