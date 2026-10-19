package de.testprojekt;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Sichert die hexagonale Architektur ab. Ein Verstoß lässt den Build scheitern.
 *
 * - domain kennt niemanden außer sich selbst
 * - application kennt domain, aber keine Adapter
 * - Adapter kennen sich gegenseitig nicht
 * - domain und application bleiben frei von Spring, JDBC und Jakarta
 * Ausnahme: config ist die Stelle, an der alles verdrahtet wird, und darf jede Schicht kennen.
 */
class ArchitectureTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("de.testprojekt");

    @Test
    void hexagonalLayersAreRespected() {
        onionArchitecture()
                .domainModels("de.testprojekt.domain..")
                .applicationServices("de.testprojekt.application..")
                .adapter("web", "de.testprojekt.adapter.in.web..")
                .adapter("persistence", "de.testprojekt.adapter.out.persistence..")
                .withOptionalLayers(true)
                .ignoreDependency(resideInAPackage("de.testprojekt.config.."), alwaysTrue())
                .check(CLASSES);
    }

    @Test
    void coreIsFrameworkFree() {
        noClasses()
                .that().resideInAnyPackage("de.testprojekt.domain..", "de.testprojekt.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage("org.springframework..", "jakarta..", "java.sql..", "javax.sql..")
                .check(CLASSES);
    }
}
