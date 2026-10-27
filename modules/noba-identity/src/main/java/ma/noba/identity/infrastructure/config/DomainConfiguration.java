package ma.noba.identity.infrastructure.config;

import ma.noba.shared.DomainService;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(
        basePackages = {
                "ma.noba.identity.application",
                "ma.noba.identity.domain",
                "ma.noba.identity.domain.service"
        },
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {DomainService.class}
        )
)
public class DomainConfiguration {

}
