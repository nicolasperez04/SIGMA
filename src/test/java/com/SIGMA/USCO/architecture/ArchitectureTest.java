package com.SIGMA.USCO.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.DisplayName;

import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.constructor;
import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Reglas de arquitectura SIGMA")
@AnalyzeClasses(packages = "com.SIGMA.USCO")
public class ArchitectureTest {

    @ArchTest
    static final ArchRule common_should_not_depend_on_business_modules =
            noClasses().that().resideInAPackage("..common..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..Modalities..", "..Users..", "..documents..");

    /*
     * La API real de archunit 1.3.0 no tiene JavaCall.Predicates.owner:
     * JavaCall.Predicates solo expone target(DescribedPredicate<? super CodeUnitCallTarget>),
     * por lo que el constructor de RuntimeException se compone con
     * AccessTarget.Predicates.constructor().and(declaredIn(equivalentTo(...))).
     */
    @ArchTest
    static final ArchRule services_should_not_instantiate_runtime_exceptions =
            noClasses().that().resideInAPackage("..service..")
                    .should().callCodeUnitWhere(target(constructor().and(declaredIn(equivalentTo(RuntimeException.class)))));

    @ArchTest
    static final ArchRule controllers_should_not_inject_repositories =
            noClasses().that().resideInAPackage("..controller..")
                    .and().haveSimpleNameNotEndingWith("TestController")
                    .and().haveSimpleNameNotEndingWith("Test")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");
}