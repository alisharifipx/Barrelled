package au.com.barrelled;

import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportScanner {

    /**
     * Type-only: interface, type, abstract class counts as type in some setups
     */
    private static final Pattern NAMED_TYPE_EXPORT = Pattern.compile(
        "^export\\s+(?:declare\\s+)?(?:interface|type|abstract\\s+class|enum)\\s+(\\w+)",
        Pattern.MULTILINE
    );

    /**
     * Value exports: const, function, class (non-abstract)
     */
    private static final Pattern NAMED_VALUE_EXPORT = Pattern.compile(
        "^export\\s+(?:declare\\s+)?(?:const|function\\*?|class)\\s+(\\w+)",
        Pattern.MULTILINE
    );

    /**
     * export type { Foo, Bar }
     */
    private static final Pattern BRACE_TYPE_EXPORT = Pattern.compile(
        "^export\\s+type\\s*\\{([^}]+)}",
        Pattern.MULTILINE
    );

    /**
     * export { Foo, Bar } (value brace exports)
     */
    private static final Pattern BRACE_VALUE_EXPORT = Pattern.compile(
        "^export\\s*\\{([^}]+)}",
        Pattern.MULTILINE
    );

    private static final Pattern DEFAULT_EXPORT = Pattern.compile(
        "^export\\s+default\\s+(?:(?:function|class)\\s+)?(\\w+)",
        Pattern.MULTILINE
    );

    /**
     * Matches a full barrel export line: export { ... } from '...'
     */
    private static final Pattern BARREL_EXPORT_LINE = Pattern.compile(
        "^export\\s*(?:type\\s*)?\\{([^}]+)}\\s*from\\s*['\"]([^'\"]+)['\"];?",
        Pattern.MULTILINE
    );

    /**
     * Parses an existing index.ts barrel file and returns a map of the existing exports.
     */
    public static LinkedHashMap<String, LinkedHashSet<String>> scanBarrelFile(String content) {
        LinkedHashMap<String, LinkedHashSet<String>> result = new LinkedHashMap<>();

        Matcher m = BARREL_EXPORT_LINE.matcher(content);
        while (m.find()) {
            String names = m.group(1);
            String path  = m.group(2);

            LinkedHashSet<String> nameSet = result.computeIfAbsent(path, k -> new java.util.LinkedHashSet<>());
            for (String part : names.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) nameSet.add(trimmed);
            }
        }

        return result;
    }

    public static List<ExportItem> scan(VirtualFile file) {
        List<ExportItem> items = new ArrayList<>();
        String nameWithoutExt = file.getName().replaceAll("\\.(tsx?)$", "");
        String content;

        try {
            content = new String(file.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return items;
        }

        // Type-only named exports (interface, type, enum, abstract class)
        Matcher typeMatcher = NAMED_TYPE_EXPORT.matcher(content);
        while (typeMatcher.find()) {
            items.add(new ExportItem(nameWithoutExt, typeMatcher.group(1), false, true));
        }

        // Value named exports (const, function, class)
        Matcher valueMatcher = NAMED_VALUE_EXPORT.matcher(content);
        while (valueMatcher.find()) {
            items.add(new ExportItem(nameWithoutExt, valueMatcher.group(1), false, false));
        }

        // export type { Foo, Bar }
        Matcher braceTypeMatcher = BRACE_TYPE_EXPORT.matcher(content);
        while (braceTypeMatcher.find()) {
            for (String part : braceTypeMatcher.group(1).split(",")) {
                String[] asParts = part.trim().split("\\s+as\\s+");
                String name = asParts[asParts.length - 1].trim();
                if (!name.isEmpty()) {
                    items.add(new ExportItem(nameWithoutExt, name, false, true));
                }
            }
        }

        // export { Foo, Bar } — value brace exports
        // exclude lines that already matched export type { }
        Matcher braceValueMatcher = BRACE_VALUE_EXPORT.matcher(content);
        while (braceValueMatcher.find()) {
            for (String part : braceValueMatcher.group(1).split(",")) {
                String[] asParts = part.trim().split("\\s+as\\s+");
                String name = asParts[asParts.length - 1].trim();
                if (!name.isEmpty()) {
                    items.add(new ExportItem(nameWithoutExt, name, false, false));
                }
            }
        }

        // export default
        Matcher defMatcher = DEFAULT_EXPORT.matcher(content);
        if (defMatcher.find()) {
            items.add(new ExportItem(nameWithoutExt, defMatcher.group(1), true, false));
        }

        return items;
    }
}
