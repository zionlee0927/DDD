package com.partnercommission.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class LayerDependencyTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.partnercommission")

    @Test
    fun `domain 레이어는 application에 의존하지 않는다`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..application..")
            .check(classes)
    }

    @Test
    fun `domain 레이어는 infrastructure에 의존하지 않는다`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .check(classes)
    }

    @Test
    fun `domain 레이어는 Spring에 의존하지 않는다`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .check(classes)
    }

    @Test
    fun `domain 레이어는 JPA에 의존하지 않는다`() {
        noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
            .check(classes)
    }

    @Test
    fun `application 레이어는 infrastructure에 의존하지 않는다`() {
        noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .check(classes)
    }
}
