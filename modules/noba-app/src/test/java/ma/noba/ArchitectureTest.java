package ma.noba;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private static final JavaClasses IMPORTED_CLASSES =
            new ClassFileImporter()
                    .importPackages("ma.noba");

    private static final String IDENTITY = "ma.noba.identity..";
    private static final String USER = "ma.noba.user..";
    private static final String QUEUE = "ma.noba.queue..";
    private static final String PROVIDER = "ma.noba.provider..";
    private static final String SHARED = "ma.noba.shared..";

    private static final String DOMAIN = "..domain..";
    private static final String APPLICATION = "..application..";
    private static final String INFRA = "..infra..";
    private static final String INTERFACES = "..interfaces..";

    @Test
    void domain_should_not_depend_on_infra() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAPackage(INFRA);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void domain_should_not_depend_on_interfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(DOMAIN)
                .should().dependOnClassesThat().resideInAPackage(INTERFACES);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void application_should_not_depend_on_interfaces() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(APPLICATION)
                .should().dependOnClassesThat().resideInAPackage(INTERFACES);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void identity_should_not_depend_on_user() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(IDENTITY)
                .and().haveSimpleNameNotEndingWith("IntegrationTest")
                .and().haveSimpleNameNotEndingWith("AbstractIntegrationTest")
                .should().dependOnClassesThat().resideInAPackage(USER);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void provider_domain_should_not_depend_on_identity_or_user() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("ma.noba.provider.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(IDENTITY, USER);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void provider_application_should_not_depend_on_identity_or_user() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("ma.noba.provider.application..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(IDENTITY, USER);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void shared_domain_should_not_depend_on_any_module() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("ma.noba.shared.domain..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(IDENTITY, USER, QUEUE, PROVIDER);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void queue_domain_should_not_depend_on_provider_domain() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("ma.noba.queue.domain..")
                .should().dependOnClassesThat().resideInAPackage("ma.noba.provider.domain..");

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void entities_should_be_in_domain_package() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                .and().resideOutsideOfPackage("ma.noba.identity.infrastructure..")
                .should().resideInAPackage(DOMAIN);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void controllers_should_be_in_interfaces_package() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .and().resideOutsideOfPackage("ma.noba.identity.infrastructure.restapi..")
                .should().resideInAPackage(INTERFACES);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void services_should_be_in_application_or_infra_package() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .and().resideOutsideOfPackage("ma.noba.identity..")
                .and().resideOutsideOfPackage("ma.noba.shared..")
                .should().resideInAnyPackage(APPLICATION, INFRA);

        rule.check(IMPORTED_CLASSES);
    }

    @Test
    void repositories_should_be_in_infra_package() {
        ArchRule rule = classes()
                .that().areAnnotatedWith(org.springframework.stereotype.Repository.class)
                .and().resideOutsideOfPackage("ma.noba.identity..")
                .should().resideInAPackage(INFRA);

        rule.check(IMPORTED_CLASSES);
    }
}
