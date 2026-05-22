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
        "commission" to AllowedAccess(cs = setOf("tenant"), events = setOf("attribution")),
        "statement" to AllowedAccess(events = setOf("commission")),
        "tracking" to AllowedAccess(),
        "partner" to AllowedAccess(),
        "tenant" to AllowedAccess(),
        "notification" to AllowedAccess(events = setOf("statement")),
    )

    private val allBcs = allowedDependencies.keys.toList()

    // ==================== domain (전체) ====================
    // 타 BC 전부 금지 (shared만 허용)

    @Test
    fun `BC의 domain은 다른 BC를 참조하지 않는다`() {
        for (bc in allBcs) {
            noClasses()
                .that().resideInAPackage("..${bc}.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                    *allBcs.filter { it != bc }.flatMap { other ->
                        listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                    }.toTypedArray()
                )
                .because("$bc domain은 다른 BC를 참조하지 않는다 (shared만 허용)")
                .allowEmptyShould(true)
                .check(classes)
        }
    }

    // ==================== application ====================
    // 타 BC 전부 금지 (타 BC 접근은 port/out → infrastructure adapter에서)
    // 예외: 이벤트 리스너는 타 BC의 domain/event 참조 허용

    @Test
    fun `BC의 application은 다른 BC를 참조하지 않는다`() {
        for (bc in allBcs) {
            val access = allowedDependencies[bc] ?: AllowedAccess()

            // 이벤트 구독 관계가 아닌 BC는 전면 금지
            val forbidden = allBcs.filter { it != bc && it !in access.events }
            if (forbidden.isNotEmpty()) {
                noClasses()
                    .that().resideInAPackage("..${bc}.application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        *forbidden.flatMap { other ->
                            listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                        }.toTypedArray()
                    )
                    .because("$bc application은 다른 BC를 참조하지 않는다 (port/out으로 격리)")
                    .allowEmptyShould(true)
                    .check(classes)
            }

            // 이벤트 구독 관계 BC — domain/event만 허용
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
                    .because("$bc application은 $eventBc 의 domain/event만 참조 가능 (이벤트 구독)")
                    .allowEmptyShould(true)
                    .check(classes)
            }
        }
    }

    // ==================== infrastructure ====================
    // C-S 관계 BC의 ReadRepository만 허용 (ACL Adapter에서 사용)

    @Test
    fun `BC의 infrastructure는 허용되지 않은 BC를 참조하지 않는다`() {
        for (bc in allBcs) {
            val access = allowedDependencies[bc] ?: AllowedAccess()
            val forbidden = allBcs.filter { it != bc && it !in access.cs }

            // 완전 금지 BC
            if (forbidden.isNotEmpty()) {
                noClasses()
                    .that().resideInAPackage("..${bc}.infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        *forbidden.flatMap { other ->
                            listOf("..${other}.domain..", "..${other}.application..", "..${other}.infrastructure..")
                        }.toTypedArray()
                    )
                    .because("$bc infrastructure는 Context Mapping에 정의되지 않은 BC 참조 금지")
                    .allowEmptyShould(true)
                    .check(classes)
            }

            // C-S 관계 BC — value/ReadRepository만 허용
            for (csBc in access.cs) {
                noClasses()
                    .that().resideInAPackage("..${bc}.infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                        "..${csBc}.domain.aggregate..",
                        "..${csBc}.domain.service..",
                        "..${csBc}.domain.exception..",
                        "..${csBc}.domain.event..",
                        "..${csBc}.application..",
                        "..${csBc}.infrastructure..",
                    )
                    .because("$bc infrastructure는 $csBc 의 value/ReadRepository만 참조 가능")
                    .allowEmptyShould(true)
                    .check(classes)
            }
        }
    }
}
