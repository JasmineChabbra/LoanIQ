package io.loaniq.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI loanIqOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LoanIQ Underwriting API")
                        .version("1.0.0")
                        .description(
                                """
                                AI-assisted credit underwriting. Applications are scored by a
                                deterministic rule engine; a language model is used only to
                                extract facts from documents and to phrase the rationale, never
                                to make the decision itself.
                                """)
                        .license(new License().name("MIT")));
    }
}
