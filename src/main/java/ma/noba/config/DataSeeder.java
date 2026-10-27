package ma.noba.config;

import ma.noba.provider.domain.model.ServiceCategory;
import ma.noba.provider.domain.port.service.ServiceCategoryRepository;
import ma.noba.user.domain.model.Role;
import ma.noba.user.domain.port.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /**
     * Idempotently ensures the built-in roles exist. In production they are inserted by a Flyway
     * migration; when Flyway is disabled (tests create the schema via Hibernate) they would be
     * missing, and signup would fail with a null {@code user_role.role_id}.
     */
    @Bean
    CommandLineRunner seedRoles(RoleRepository roleRepository) {
        return args -> {
            List<Role> roles = List.of(
                    new Role("USER", "Regular customer"),
                    new Role("PROVIDER", "Service provider"),
                    new Role("DELIVERER", "Delivery personnel"),
                    new Role("ADMIN", "System administrator"));
            for (Role role : roles) {
                if (roleRepository.findByName(role.getName()) == null) {
                    roleRepository.save(role);
                    log.info("Seeded role {}", role.getName());
                }
            }
        };
    }

    @Bean
    CommandLineRunner seedServiceCategories(ServiceCategoryRepository repository) {
        return args -> {
            if (repository.findAll().isEmpty()) {
                log.info("Seeding service categories...");

                repository.save(new ServiceCategory("Haircut", Duration.ofMinutes(30), "Standard haircut service"));
                repository.save(new ServiceCategory("Coloring", Duration.ofMinutes(60), "Hair coloring and highlights"));
                repository.save(new ServiceCategory("Consultation", Duration.ofMinutes(15), "Initial consultation"));
                repository.save(new ServiceCategory("Styling", Duration.ofMinutes(45), "Hair styling and treatment"));
                repository.save(new ServiceCategory("Wash & Dry", Duration.ofMinutes(20), "Hair wash and blow dry"));
                repository.save(new ServiceCategory("Beard Trim", Duration.ofMinutes(15), "Beard grooming and trimming"));
                repository.save(new ServiceCategory("Makeup", Duration.ofMinutes(60), "Professional makeup application"));
                repository.save(new ServiceCategory("Nails", Duration.ofMinutes(30), "Manicure and pedicure services"));
                repository.save(new ServiceCategory("Massage", Duration.ofMinutes(60), "Relaxation and therapeutic massage"));
                repository.save(new ServiceCategory("Facial", Duration.ofMinutes(45), "Facial treatment and skincare"));

                log.info("Seeded {} service categories", repository.findAll().size());
            }
        };
    }
}
