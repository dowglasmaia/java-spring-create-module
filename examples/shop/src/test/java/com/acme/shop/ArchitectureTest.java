package com.acme.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Architecture rules A01-A10 and A12 of the "Hexagonal Pragmática" model (see the java-spring-create-module skill).
 * Layout per feature: api / application (port, usecase) / domain / infrastructure; plus a minimal shared.
 * Rule A11 (a rich feature keeps its @Entity in infrastructure.persistence) is enforced by scripts/check-architecture.mjs.
 */
@AnalyzeClasses(packages = "com.acme.shop", importOptions = {ImportOption.DoNotIncludeTests.class, ArchitectureTest.ExcludeLegacyPackages.class})
class ArchitectureTest {

    private static final String BASE = "com.acme.shop";

    /**
     * Top-level packages of the OLD layered structure that are not migrated yet (for example "controllers", "services").
     * Only used while a project is being migrated; it must be empty (strict) when the last feature is done.
     */
    private static final Set<String> LEGACY_PACKAGES = Set.of();

    /** Leaves the legacy packages out of the analysis. */
    public static final class ExcludeLegacyPackages implements ImportOption {
        @Override
        public boolean includes(Location location) {
            String uri = location.asURI().toString();
            return LEGACY_PACKAGES.stream().noneMatch(legacy -> uri.contains("/com/acme/shop/" + legacy + "/"));
        }
    }

    /** A01 - the domain is framework-free (Java only, plus its own feature and shared.error). */
    @ArchTest
    static final ArchRule domain_is_framework_free = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta..", "javax..", "com.fasterxml..", "org.hibernate..",
                    "lombok..", "org.slf4j..", "org.apache.kafka..", "feign..")
            .because("the domain is pure Java");

    /** A02 - the domain does not depend on outer layers. */
    @ArchTest
    static final ArchRule domain_does_not_depend_on_outer_layers = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..api..", "..infrastructure..");

    /** A03 - application knows neither the api nor the infrastructure, nor web/persistence technology. */
    @ArchTest
    static final ArchRule application_does_not_depend_on_api_or_infrastructure = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..api..", "..infrastructure..", "jakarta.persistence..", "jakarta.servlet..", "org.springframework.web..",
                    "org.springframework.http..", "org.springframework.data..", "org.springframework.jdbc..",
                    "org.springframework.kafka..", "org.springframework.cloud..");

    /** A04 - the api (inbound adapter) talks to use cases and the domain, never to the infrastructure. */
    @ArchTest
    static final ArchRule api_does_not_touch_infrastructure = noClasses().that().resideInAPackage("..api..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..");

    /** A05 - a feature does not use another feature's infrastructure or api. */
    @ArchTest
    static final ArchRule features_do_not_use_other_features_infrastructure_or_api = classes().should(new ArchCondition<JavaClass>("not use another feature's infrastructure or api") {
        @Override
        public void check(JavaClass origin, ConditionEvents events) {
            String own = featureOf(origin.getPackageName());
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                String targetPackage = dependency.getTargetClass().getPackageName();
                String target = featureOf(targetPackage);
                if (target != null && own != null && !target.equals(own) && !target.equals("shared")
                        && (targetPackage.contains(".infrastructure") || targetPackage.contains(".api"))) {
                    events.add(SimpleConditionEvent.violated(origin, origin.getName() + " uses " + dependency.getTargetClass().getName()
                            + " (infrastructure or api of feature '" + target + "')"));
                }
            }
        }
    });

    /** A06 - ports are interfaces; use cases are concrete classes (no interface per use case). */
    @ArchTest
    static final ArchRule ports_are_interfaces = classes().that().resideInAPackage("..application.port..")
            .should().beInterfaces();

    @ArchTest
    static final ArchRule use_cases_are_not_interfaces = classes().that().resideInAPackage("..application.usecase..")
            .should().notBeInterfaces();

    /** A07 - stereotypes and ORM annotations stay in their layer: @Entity/@Table only in infrastructure. */
    @ArchTest
    static final ArchRule web_and_persistence_stereotypes_stay_out_of_domain_and_application = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().beAnnotatedWith(RestController.class)
            .orShould().beAnnotatedWith(Controller.class)
            .orShould().beAnnotatedWith(RestControllerAdvice.class)
            .orShould().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("jakarta.persistence.Table");

    @ArchTest
    static final ArchRule orm_annotations_stay_out_of_the_api = noClasses().that().resideInAPackage("..api..")
            .should().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith("jakarta.persistence.Entity")
            .orShould().beAnnotatedWith("jakarta.persistence.Table");

    /** A08 - transactions belong to the use cases, not to the domain or the api. */
    @ArchTest
    static final ArchRule no_transactions_in_domain_or_api = noClasses().that().resideInAnyPackage("..domain..", "..api..")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule no_transactional_methods_in_domain_or_api = noMethods().that().areDeclaredInClassesThat()
            .resideInAnyPackage("..domain..", "..api..").should().beAnnotatedWith(Transactional.class);

    /** A09 - constructor injection only. */
    @ArchTest
    static final ArchRule no_field_injection = noFields().should().beAnnotatedWith(Autowired.class);

    /** A10 - shared is the bottom: it depends on no feature. */
    @ArchTest
    static final ArchRule shared_depends_on_no_feature = classes().that().resideInAPackage(BASE + ".shared..")
            .should(new ArchCondition<JavaClass>("not depend on a feature") {
                @Override
                public void check(JavaClass origin, ConditionEvents events) {
                    for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                        String target = featureOf(dependency.getTargetClass().getPackageName());
                        if (target != null && !target.equals("shared")) {
                            events.add(SimpleConditionEvent.violated(origin, origin.getName() + " depends on feature '" + target + "'"));
                        }
                    }
                }
            });

    /** A12 - one class per operation, named {@code <Verb><Noun>UseCase}, and only in application.usecase. */
    @ArchTest
    static final ArchRule use_cases_are_named_after_their_operation = classes().that().resideInAPackage("..application.usecase..")
            .and().areNotNestedClasses().and().areNotAnonymousClasses()
            .should().haveSimpleNameEndingWith("UseCase");

    @ArchTest
    static final ArchRule use_case_classes_live_in_the_usecase_package = classes().that().haveSimpleNameEndingWith("UseCase")
            .and().areNotNestedClasses()
            .should().resideInAPackage("..application.usecase..");

    /** The feature name is the first package segment after the base package; null outside the base package. */
    private static String featureOf(String packageName) {
        if (!packageName.startsWith(BASE + ".")) {
            return null;
        }
        String rest = packageName.substring(BASE.length() + 1);
        int dot = rest.indexOf('.');
        String first = dot < 0 ? rest : rest.substring(0, dot);
        return LEGACY_PACKAGES.contains(first) ? null : first; // an unmigrated layered package is not a feature
    }
}
