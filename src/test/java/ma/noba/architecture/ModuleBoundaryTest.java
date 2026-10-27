package ma.noba.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Architecture guardrails for the modular monolith.
 *
 * <p>Macro shape: vertical feature modules under {@code ma.noba.<module>}.
 * Micro shape: each module is a hexagon ({@code domain} → {@code application} → {@code infra},
 * with {@code interfaces} on the outside).
 *
 * <p>Rules are non-negotiable for new code. Existing violations are frozen (see
 * {@code src/test/resources/archunit_store}) so the build fails only when someone introduces a
 * new violation; fixing a violation removes it from the store.
 *
 * <p>Uses the programmatic ArchUnit API (not the JUnit engine) so it runs in the normal Surefire
 * test lifecycle.
 */
class ModuleBoundaryTest {

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("ma.noba");

    /** Feature modules that the shared kernel must never depend on. */
    private static final String[] FEATURE_MODULES = {
            "ma.noba.identity..",
            "ma.noba.user..",
            "ma.noba.provider..",
            "ma.noba.queue..",
            "ma.noba.booking..",
            "ma.noba.payment..",
            "ma.noba.subscription..",
            "ma.noba.loyalty..",
            "ma.noba.referral..",
            "ma.noba.promotion..",
            "ma.noba.packagegift..",
            "ma.noba.qa..",
            "ma.noba.media..",
            "ma.noba.reminder..",
            "ma.noba.announcement..",
            "ma.noba.badge..",
            "ma.noba.providernetwork..",
            "ma.noba.employee..",
            "ma.noba.groupqueue..",
            "ma.noba.statement..",
            "ma.noba.analytics..",
            "ma.noba.admin..",
    };

    /**
     * The shared kernel is the bottom of the dependency graph: it may only use itself and
     * platform-level configuration. Anything else means the "kernel" is leaking into features.
     */
    private static final ArchRule KERNEL_HAS_NO_FEATURE_DEPENDENCIES = noClasses()
            .that().resideInAPackage("ma.noba.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(FEATURE_MODULES);

    /** Domain logic must not know about persistence or HTTP adapters. */
    private static final ArchRule DOMAIN_DOES_NOT_DEPEND_ON_ADAPTERS = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..infra..", "..infrastructure..", "..interfaces..");

    /** Use cases go through ports; they must not reach into JPA repositories or gateways. */
    private static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_INFRA = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage("..infra..", "..infrastructure..");

    /** Controllers talk to application services, not to adapters. */
    private static final ArchRule INTERFACES_DO_NOT_DEPEND_ON_INFRA = noClasses()
            .that().resideInAPackage("..interfaces..")
            .should().dependOnClassesThat().resideInAnyPackage("..infra..", "..infrastructure..");

    /**
     * A module may use another module's public contract, but never its JPA entities. This is the
     * rule that makes a later extraction (or SaaS replacement) possible.
     */
    private static final ArchRule MODULES_DO_NOT_SHARE_PERSISTENCE_ENTITIES = classes()
            .that().resideInAPackage("ma.noba..")
            .should(notDependOnOtherModulesEntities());

    @Test
    void kernelHasNoFeatureDependencies() {
        KERNEL_HAS_NO_FEATURE_DEPENDENCIES.check(CLASSES);
    }

    /**
     * Modules must form a DAG before they can be extracted into build modules or services.
     * There are no known cycles: this is a hard gate. Introduce a port or a domain event instead.
     */
    @Test
    void modulesAreFreeOfCycles() {
        assertThat(currentCyclePairs())
                .as("Module dependency cycles detected. Break them with a consumer-owned port or a domain event.")
                .isEmpty();
    }

    private static Set<String> currentCyclePairs() {
        Map<String, Set<String>> graph = new HashMap<>();
        for (JavaClass javaClass : CLASSES) {
            String from = moduleOf(javaClass.getPackageName());
            if (from == null) {
                continue;
            }
            for (var dependency : javaClass.getDirectDependenciesFromSelf()) {
                String to = moduleOf(dependency.getTargetClass().getPackageName());
                if (to != null && !to.equals(from)) {
                    graph.computeIfAbsent(from, key -> new HashSet<>()).add(to);
                }
            }
        }

        Set<String> pairs = new TreeSet<>();
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            for (String target : entry.getValue()) {
                if (entry.getKey().compareTo(target) < 0 && reachable(graph, target, entry.getKey())) {
                    pairs.add(entry.getKey() + "<->" + target);
                }
            }
        }
        return pairs;
    }

    private static boolean reachable(Map<String, Set<String>> graph, String start, String target) {
        ArrayDeque<String> queue = new ArrayDeque<>();
        Set<String> seen = new HashSet<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(target)) {
                return true;
            }
            for (String next : graph.getOrDefault(current, Set.of())) {
                if (seen.add(next)) {
                    queue.add(next);
                }
            }
        }
        return false;
    }

    @Test
    void domainDoesNotDependOnAdapters() {
        FreezingArchRule.freeze(DOMAIN_DOES_NOT_DEPEND_ON_ADAPTERS).check(CLASSES);
    }

    @Test
    void applicationDoesNotDependOnInfra() {
        FreezingArchRule.freeze(APPLICATION_DOES_NOT_DEPEND_ON_INFRA).check(CLASSES);
    }

    @Test
    void interfacesDoNotDependOnInfra() {
        FreezingArchRule.freeze(INTERFACES_DO_NOT_DEPEND_ON_INFRA).check(CLASSES);
    }

    @Test
    void modulesDoNotSharePersistenceEntities() {
        FreezingArchRule.freeze(MODULES_DO_NOT_SHARE_PERSISTENCE_ENTITIES).check(CLASSES);
    }

    private static ArchCondition<JavaClass> notDependOnOtherModulesEntities() {
        return new ArchCondition<>("not depend on another module's domain model") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                String sourceModule = moduleOf(item.getPackageName());
                if (sourceModule == null) {
                    return;
                }
                Set<String> reportedTargets = new HashSet<>();
                for (var dependency : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dependency.getTargetClass();
                    String targetPackage = target.getPackageName();
                    String targetModule = moduleOf(targetPackage);
                    if (targetModule == null || targetModule.equals(sourceModule)) {
                        continue;
                    }
                    // The composition root wires every module together by design, and the shared
                    // kernel's vocabulary (UserStatus, Money, ...) is intentionally cross-module.
                    if ("app".equals(sourceModule) || "shared".equals(targetModule)) {
                        continue;
                    }
                    if (targetPackage.startsWith("ma.noba." + targetModule + ".domain.model")
                            && reportedTargets.add(target.getName())) {
                        events.add(SimpleConditionEvent.violated(item,
                                item.getName() + " depends on " + target.getName()));
                    }
                }
            }
        };
    }

    private static String moduleOf(String packageName) {
        if (!packageName.startsWith("ma.noba.")) {
            return null;
        }
        String[] parts = packageName.split("\\.");
        return parts.length >= 3 ? parts[2] : null;
    }
}
