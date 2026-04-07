package au.com.barrelled;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BarrelFileMergerTest {

    @Test
    void replacesManagedExportsAndPreservesUnmanagedExports() {
        String existing = """
            // Public API
            export { OldButton, ButtonProps } from './Button';
            export * from './legacy';
            export { Modal } from './Modal';
            export type { ExternalProps } from './external';
            """;
        String incoming = """
            export { Button, type ButtonProps } from './Button';
            """;

        String merged = BarrelFileMerger.merge(existing, incoming, Set.of("./Button", "./Modal"));

        assertEquals("""
            // Public API
            export * from './legacy';
            export type { ExternalProps } from './external';
            export { Button, type ButtonProps } from './Button';
            """, merged);
    }

    @Test
    void removesAllManagedExportsWhenIncomingSelectionIsEmpty() {
        String existing = """
            export { Button } from './Button';
            export { Unmanaged } from './Unmanaged';
            """;

        String merged = BarrelFileMerger.merge(existing, "", Set.of("./Button"));

        assertEquals("""
            export { Unmanaged } from './Unmanaged';
            """, merged);
    }

    @Test
    void doesNotPrefixNewExportsWithBlankLineWhenExistingFileIsEmpty() {
        String incoming = "export { Button } from './Button';\n";

        String merged = BarrelFileMerger.merge("", incoming, Set.of("./Button"));

        assertEquals(incoming, merged);
    }
}
