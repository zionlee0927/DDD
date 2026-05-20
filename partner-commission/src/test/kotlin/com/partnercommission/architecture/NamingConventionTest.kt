package com.partnercommission.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test

class NamingConventionTest {

    private val classes = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("com.partnercommission")

    @Test
    fun `UseCase 인터페이스는 UseCase로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..application.port.in..")
            .and().areInterfaces()
            .should().haveSimpleNameEndingWith("UseCase")
            .because("Inbound Port 인터페이스는 UseCase로 끝나야 함")
            .check(classes)
    }

    @Test
    fun `Application Service는 Service로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..application.service..")
            .and().areNotInterfaces()
            .and().areNotMemberClasses()
            .and().areNotAnonymousClasses()
            .should().haveSimpleNameEndingWith("Service")
            .because("Application Service는 Service로 끝나야 함")
            .check(classes)
    }

    @Test
    fun `Controller는 Controller로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..infrastructure.inbound.web..")
            .and().areNotMemberClasses()
            .should().haveSimpleNameEndingWith("Controller")
            .because("Web Controller는 Controller로 끝나야 함")
            .check(classes)
    }

    @Test
    fun `JPA Entity는 JpaEntity로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..infrastructure.outbound.persistence..")
            .and().haveSimpleNameContaining("Entity")
            .should().haveSimpleNameEndingWith("JpaEntity")
            .because("JPA Entity는 JpaEntity로 끝나야 함")
            .check(classes)
    }

    @Test
    fun `JPA Repository는 JpaRepository로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..infrastructure.outbound.persistence..")
            .and().areInterfaces()
            .and().haveSimpleNameContaining("Repository")
            .should().haveSimpleNameEndingWith("JpaRepository")
            .because("JPA Repository는 JpaRepository로 끝나야 함")
            .check(classes)
    }

    @Test
    fun `Repository 구현체는 RepositoryImpl로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..infrastructure.outbound.persistence..")
            .and().haveSimpleNameContaining("Repository")
            .and().areNotInterfaces()
            .should().haveSimpleNameEndingWith("RepositoryImpl")
            .because("Repository 구현체는 RepositoryImpl로 끝나야 함")
            .check(classes)
    }

    @Test
    fun `Exception 클래스는 Exception으로 끝나야 한다`() {
        classes()
            .that().resideInAPackage("..exception..")
            .and().areAssignableTo(Exception::class.java)
            .should().haveSimpleNameEndingWith("Exception")
            .allowEmptyShould(true)
            .because("Exception 클래스는 Exception으로 끝나야 함")
            .check(classes)
    }
}
