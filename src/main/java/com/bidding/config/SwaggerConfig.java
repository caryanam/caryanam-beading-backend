package com.bidding.config;



import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI toursOpenAPI() {

        return new OpenAPI()

                .info(new Info()

                        .title(" CARYANAM BIDDING  BACKEND Application API")

                        .description("Production Ready REST APIs for CARYANAM BIDDING  Management System")

                        .version("1.0.0")

                        .contact(new Contact()
                                .name("geetanjali khaladkar")
                                .email("support@MannKiAavaj.com"))

                        .license(new License()
                                .name("Apache 2.0")))

                .externalDocs(new ExternalDocumentation()
                        .description("API Documentation"));
    }
}
