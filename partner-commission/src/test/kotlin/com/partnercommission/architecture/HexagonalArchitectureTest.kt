package com.partnercommission.architecture

import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import org.junit.jupiter.api.Test

class HexagonalArchitectureTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.partnercommission")

    @Test
    fun `헥사고날 레이어 의존성 - 외부에서 내부로만 향해야 한다`() {
        layeredArchitecture()
            .consideringAllDependencies()
            .layer("Infrastructure").definedBy("..infrastructure..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .whereLayer("Infrastructure").mayOnlyAccessLayers("Application", "Domain")
            .whereLayer("Application").mayOnlyAccessLayers("Domain")
            .whereLayer("Domain").mayNotAccessAnyLayer()
            .ignoreDependency(
                JavaClass.Predicates.resideInAPackage("com.partnercommission.."),
                JavaClass.Predicates.resideOutsideOfPackage("com.partnercommission..")
            )
            .because("Hexagonal Architecture: 의존성은 항상 외부에서 내부로 향해야 함")
            .check(classes)
    }
}
