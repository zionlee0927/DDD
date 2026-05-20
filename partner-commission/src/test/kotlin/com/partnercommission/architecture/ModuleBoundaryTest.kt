package com.partnercommission.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class ModuleBoundaryTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.partnercommission")

    private val bcs = listOf("attribution", "commission", "tracking", "partner", "statement", "tenant", "notification")

    // ==================== BC 간 참조 규칙 ====================
    // 허용: 타 BC의 domain/repository/ (C-S 동기 조회)
    // 허용: 타 BC의 domain/event/ (이벤트 구독)
    // 금지: 그 외 모든 타 BC 참조

    @Test
    fun `BC의 domain aggregate와 value는 다른 BC를 참조하지 않는다`() {
        for (bc in bcs) {
            noClasses()
                .that().resideInAnyPackage("..${bc}.domain.aggregate..", "..${bc}.domain.value..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *bcs.filter { it != bc }.flatMap { other ->
                        listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                    }.toTypedArray()
                )
                .because("$bc 의 aggregate/value는 다른 BC를 ID로만 참조해야 한다")
                .allowEmptyShould(true)
                .check(classes)
        }
    }

    @Test
    fun `BC의 domain service는 다른 BC의 repository, application, infrastructure를 참조하지 않는다`() {
        for (bc in bcs) {
            noClasses()
                .that().resideInAPackage("..${bc}.domain.service..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *bcs.filter { it != bc }.flatMap { other ->
                        listOf(
                            "..${other}.domain.repository..",
                            "..${other}.domain.service..",
                            "..${other}.domain.exception..",
                            "..${other}.application..",
                            "..${other}.infrastructure..",
                        )
                    }.toTypedArray()
                )
                .because("$bc domain service는 다른 BC의 도메인 객체(aggregate/value)만 파라미터로 받을 수 있다")
                .allowEmptyShould(true)
                .check(classes)
        }
    }

    @Test
    fun `BC의 application은 다른 BC의 aggregate, value, service, application, infrastructure를 참조하지 않는다`() {
        for (bc in bcs) {
            noClasses()
                .that().resideInAPackage("..${bc}.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *bcs.filter { it != bc }.flatMap { other ->
                        listOf(
                            "..${other}.domain.aggregate..",
                            "..${other}.domain.value..",
                            "..${other}.domain.service..",
                            "..${other}.domain.exception..",
                            "..${other}.application..",
                            "..${other}.infrastructure..",
                        )
                    }.toTypedArray()
                )
                .because("$bc application은 다른 BC의 domain/repository와 domain/event만 참조 가능")
                .allowEmptyShould(true)
                .check(classes)
        }
    }

    @Test
    fun `BC의 infrastructure는 다른 BC를 참조하지 않는다`() {
        for (bc in bcs) {
            noClasses()
                .that().resideInAPackage("..${bc}.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *bcs.filter { it != bc }.flatMap { other ->
                        listOf(
                            "..${other}.domain..",
                            "..${other}.application..",
                            "..${other}.infrastructure..",
                        )
                    }.toTypedArray()
                )
                .because("$bc infrastructure는 자기 BC만 참조한다")
                .allowEmptyShould(true)
                .check(classes)
        }
    }
}
