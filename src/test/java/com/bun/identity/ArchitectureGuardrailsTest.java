package com.bun.identity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class ArchitectureGuardrailsTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");

    @Test
    void featurePackagesStaySimple() {
        assertTrue(Files.exists(MAIN_JAVA.resolve("com/bun/identity/auth")));
        assertTrue(Files.exists(MAIN_JAVA.resolve("com/bun/identity/user")));
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/identityaccess")));
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/usermanagement")));
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/partyprofile")));
    }

    @Test
    void templateDoesNotContainProjectSpecificUserDomains() throws IOException {
        for (Path file : allJavaFilesUnder("com/bun/identity")) {
            String name = file.getFileName().toString();
            assertFalse(name.contains("Customer"), () -> "Customer domain file should not be in starter: " + file);
            assertFalse(name.contains("Seller"), () -> "Seller domain file should not be in starter: " + file);
            assertFalse(name.contains("Admin"), () -> "Admin domain file should not be in starter: " + file);
        }
    }

    @Test
    void removedCommandHandlerScaffoldingDoesNotReturn() {
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/auth/application")));
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/user/application")));
    }

    @Test
    void removedYagniClassesDoNotReturn() {
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/auth/service/JwtService.java")));
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/auth/service/InternalTokenService.java")));
        assertFalse(Files.exists(MAIN_JAVA.resolve("com/bun/identity/exception/AppUserException.java")));
    }

    @Test
    void controllersDoNotUseRepositoryDirectly() throws IOException {
        List<Path> controllerFiles = allJavaFilesUnder("com/bun/identity/auth/controller");
        controllerFiles.addAll(allJavaFilesUnder("com/bun/identity/user/controller"));

        for (Path file : controllerFiles) {
            String content = Files.readString(file);
            assertFalse(content.contains("Repository"),
                    () -> "Controller must not reference repositories directly: " + file);
        }
    }

    private static List<Path> allJavaFilesUnder(String relative) throws IOException {
        Path root = MAIN_JAVA.resolve(relative);
        if (!Files.exists(root)) {
            return java.util.Collections.emptyList();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .collect(java.util.stream.Collectors.toList());
        }
    }
}
