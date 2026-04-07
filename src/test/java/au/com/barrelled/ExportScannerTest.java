package au.com.barrelled;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportScannerTest {

    @Test
    void scansJsxDefaultExpressionUsingFileName() {
        List<ExportItem> items = ExportScanner.scanContent(
            "Button.jsx",
            "export default memo(Button);\n"
        );

        assertEquals(1, items.size());
        ExportItem item = items.getFirst();
        assertEquals("Button", item.getFileName());
        assertEquals("Button", item.getExportName());
        assertTrue(item.isDefault());
    }

    @Test
    void scansAnonymousDefaultUsingFileName() {
        List<ExportItem> items = ExportScanner.scanContent(
            "Widget.tsx",
            "export default function () {\n  return null;\n}\n"
        );

        assertEquals(1, items.size());
        ExportItem item = items.getFirst();
        assertEquals("Widget", item.getFileName());
        assertEquals("Widget", item.getExportName());
        assertTrue(item.isDefault());
    }

    @Test
    void parsesTypeOnlyBarrelExportsAsTypeTokens() {
        LinkedHashMap<String, LinkedHashSet<String>> exports = ExportScanner.scanBarrelFile(
            "export type { ButtonProps } from './Button';\n"
        );

        assertEquals(SetFactory.of("type ButtonProps"), exports.get("./Button"));
    }

    private static class SetFactory {
        static LinkedHashSet<String> of(String... values) {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (String value : values) {
                set.add(value);
            }
            return set;
        }
    }
}
