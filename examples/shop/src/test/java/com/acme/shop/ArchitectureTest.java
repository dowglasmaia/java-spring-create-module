package com.acme.shop;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.core.importer.ImportOption;
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
 * Architecture rules A01-A10 of the "Hexagonal Pragmática" model (see the java-spring-create-module skill).
 * Rule A11 (a rich feature keeps its @Entity in infra.persistence) is enforced by scripts/check-architecture.mjs.
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
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infra..");

    /** A03 - application does not know infrastructure or web/persistence technology. */
    @ArchTest
    static final ArchRule application_does_not_depend_on_infra = noClasses().that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..infra..", "jakarta.persistence..", "jakarta.servlet..", "org.springframework.web..",
                    "org.springframework.http..", "org.springframework.data..", "org.springframework.jdbc..",
                    "org.springframework.kafka..", "org.springframework.cloud..");

    /** A04 - web never touches persistence. */
    @ArchTest
    static final ArchRule web_does_not_touch_persistence = noClasses().that().resideInAPackage("..infra.web..")
            .should().dependOnClassesThat().resideInAPackage("..infra.persistence..");

    /** A05 - a feature does not use another feature's infra. */
    @ArchTest
    static final ArchRule features_do_not_use_other_features_infra = classes().should(new ArchCondition<JavaClass>("not use another feature's infra") {
        @Override
        public void check(JavaClass origin, ConditionEvents events) {
            String own = featureOf(origin.getPackageName());
            for (Dependency dependency : origin.getDirectDependenciesFromSelf()) {
                String targetPackage = dependency.getTargetClass().getPackageName();
                String target = featureOf(targetPackage);
                if (target != null && own != null && !target.equals(own) && !target.equals("shared")
                        && (targetPackage.contains(".infra.") || targetPackage.endsWith(".infra"))) {
                    events.add(SimpleConditionEvent.violated(origin, origin.getName() + " uses " + dependency.getTargetClass().getName()
                            + " (infra of feature '" + target + "')"));
                }
            }
        }
    });

    /** A06 - no interfaces in application (the service is a concrete class; ports live in domain). */
    @ArchTest
    static final ArchRule application_declares_no_interfaces = classes().that().resideInAPackage("..application..")
            .should().notBeInterfaces();

    /** A07 - controllers and persistence stereotypes stay out of domain and application. */
    @ArchTest
    static final ArchRule web_and_persistence_stereotypes_stay_out_of_domain_and_application = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().beAnnotatedWith(RestController.class)
            .orShould().beAnnotatedWith(Controller.class)
            .orShould().beAnnotatedWith(RestControllerAdvice.class)
            .orShould().beAnnotatedWith(Repository.class)
            .orShould().beAnnotatedWith("jakarta.persistence.Entity");

    /** A08 - transactions belong to the application services, not to the domain or the web layer. */
    @ArchTest
    static final ArchRule no_transactions_in_domain_or_web = noClasses().that().resideInAnyPackage("..domain..", "..infra.web..")
            .should().beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule no_transactional_methods_in_domain_or_web = noMethods().that().areDeclaredInClassesThat()
            .resideInAnyPackage("..domain..", "..infra.web..").should().beAnnotatedWith(Transactional.class);

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

    /** The feature name is the first package segment after the base package; null outside the base package. */
    private static String featureOf(String packageName) {
        if (!packageName.startsWith(BASE + ".")) {
            return null;
        }
        String rest = packageName.substring(BASE.length() + 1);
        int dot = rest.indexOf('.');
        return dot < 0 ? rest : rest.substring(0, dot);
    }
}
