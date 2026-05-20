package com.partnercommission.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

data class AllowedAccess(
    val cs: Set<String> = emptySet(),           // C-S 관계: 동기 조회 허용
    val events: Set<String> = emptySet(),       // Events 관계: domain/event만 허용
)

class ModuleBoundaryTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.partnercommission")

    // Context Mapping (07-context-mapping.md) 기반
    private val allowedDependencies: Map<String, AllowedAccess> = mapOf(
        "attribution" to AllowedAccess(cs = setOf("tracking", "tenant")),
        "commission" to AllowedAccess(events = setOf("attribution")),
        "statement" to AllowedAccess(events = setOf("commission")),
        "tracking" to AllowedAccess(),
        "partner" to AllowedAccess(),
        "tenant" to AllowedAccess(),
        "notification" to AllowedAccess(events = setOf("statement")),
    )

    private val allBcs = allowedDependencies.keys.toList()

    // ==================== domain/aggregate, domain/value ====================
    // 타 BC 전부 금지 (shared만 허용)

    @Test
    fun `BC의 domain aggregate와 value는 다른 BC를 참조하지 않는다`() {
        for (bc in allBcs) {
            noClasses()
                .that().resideInAnyPackage("..${bc}.domain.aggregate..", "..${bc}.domain.value..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *allBcs.filter { it != bc }.flatMap { other ->
                        listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                    }.toTypedArray()
                )
                .because("$bc 의 aggregate/value는 다른 BC를 ID로만 참조해야 한다")
                .allowEmptyShould(true)
                .check(classes)
        }
    }

    // ==================== domain/service ====================
    // C-S 관계 BC의 aggregate/value만 허용 (파라미터로 받기 위해)
    // Repository, event, application, infrastructure 금지 (순수 유지)

    @Test
    fun `BC의 domain service는 허용된 관계 BC의 aggregate와 value만 참조한다`() {
        for (bc in allBcs) {
            val access = allowedDependencies[bc] ?: AllowedAccess()

            // C-S 관계 BC에서도 repository/service/exception/application/infrastructure 금지
            for (csBc in access.cs) {
                noClasses()
                    .that().resideInAPackage("..${bc}.domain.service..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        "..${csBc}.domain.repository..",
                        "..${csBc}.domain.service..",
                        "..${csBc}.domain.exception..",
                        "..${csBc}.application..",
                        "..${csBc}.infrastructure..",
                    )
                    .because("$bc domain service는 $csBc 의 aggregate/value만 참조 가능 (순수 유지)")
                    .allowEmptyShould(true)
                    .check(classes)
            }

            // 허용되지 않은 BC 전부 금지
            val forbidden = allBcs.filter { it != bc && it !in access.cs }
            if (forbidden.isNotEmpty()) {
                noClasses()
                    .that().resideInAPackage("..${bc}.domain.service..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        *forbidden.flatMap { other ->
                            listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                        }.toTypedArray()
                    )
                    .because("$bc domain service는 Context Mapping에 정의되지 않은 BC 참조 금지")
                    .allowEmptyShould(true)
                    .check(classes)
            }
        }
    }

    // ==================== application ====================
    // C-S 관계: aggregate/value/ReadRepository 허용, 나머지 금지
    // Events 관계: domain/event만 허용

    @Test
    fun `BC의 application은 허용되지 않은 BC를 참조하지 않는다`() {
        for (bc in allBcs) {
            val access = allowedDependencies[bc] ?: AllowedAccess()
            val forbidden = allBcs.filter { it != bc && it !in access.cs && it !in access.events }

            // 완전 금지 BC
            if (forbidden.isNotEmpty()) {
                noClasses()
                    .that().resideInAPackage("..${bc}.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        *forbidden.flatMap { other ->
                            listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                        }.toTypedArray()
                    )
                    .because("$bc application은 Context Mapping에 정의되지 않은 BC 참조 금지")
                    .allowEmptyShould(true)
                    .check(classes)
            }

            // Events 관계 BC — event만 허용
            for (eventBc in access.events) {
                noClasses()
                    .that().resideInAPackage("..${bc}.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        "..${eventBc}.domain.aggregate..",
                        "..${eventBc}.domain.value..",
                        "..${eventBc}.domain.repository..",
                        "..${eventBc}.domain.service..",
                        "..${eventBc}.domain.exception..",
                        "..${eventBc}.application..",
                        "..${eventBc}.infrastructure..",
                    )
                    .because("$bc 는 $eventBc 의 domain/event만 참조 가능 (Events 관계)")
                    .allowEmptyShould(true)
                    .check(classes)
            }

            // C-S 관계 BC — aggregate/value/ReadRepository만 허용, 나머지 금지
            for (csBc in access.cs) {
                noClasses()
                    .that().resideInAPackage("..${bc}.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        "..${csBc}.domain.service..",
                        "..${csBc}.domain.exception..",
                        "..${csBc}.domain.event..",
                        "..${csBc}.application..",
                        "..${csBc}.infrastructure..",
                    )
                    .because("$bc 는 $csBc 의 aggregate/value/ReadRepository만 참조 가능 (C-S 관계)")
                    .allowEmptyShould(true)
                    .check(classes)

                // Write Repository 금지
                noClasses()
                    .that().resideInAPackage("..${bc}.application..")
                    .should().dependOnClassesThat(
                        com.tngtech.archunit.base.DescribedPredicate.describe(
                            "Write Repository in $csBc"
                        ) { javaClass ->
                            javaClass.packageName.contains("$csBc.domain.repository") &&
                                javaClass.simpleName.endsWith("Repository") &&
                                !javaClass.simpleName.endsWith("ReadRepository")
                        }
                    )
                    .because("$bc 는 $csBc 의 ReadRepository만 참조 가능 (쓰기 금지)")
                    .allowEmptyShould(true)
                    .check(classes)
            }
        }
    }

    // ==================== infrastructure ====================
    // 타 BC 전부 금지

    @Test
    fun `BC의 infrastructure는 다른 BC를 참조하지 않는다`() {
        for (bc in allBcs) {
            noClasses()
                .that().resideInAPackage("..${bc}.infrastructure..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *allBcs.filter { it != bc }.flatMap { other ->
                        listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                    }.toTypedArray()
                )
                .because("$bc infrastructure는 자기 BC만 참조한다")
                .allowEmptyShould(true)
                .check(classes)
        }
    }
}
