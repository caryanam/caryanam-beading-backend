package com.bidding.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class SwaggerConfig {

//    @Bean
//    public OpenAPI toursOpenAPI() {
//
//        return new OpenAPI()
//
//                .info(new Info()
//
//                        .title(" CARYANAM BIDDING  BACKEND Application API")
//
//                        .description("Production Ready REST APIs for CARYANAM BIDDING  Management System")
//
//                        .version("1.0.0")
//
//                        .contact(new Contact()
//                                .name("geetanjali khaladkar")
//                                .email("support@MannKiAavaj.com"))
//
//                        .license(new License()
//                                .name("Apache 2.0")))
//
//                .externalDocs(new ExternalDocumentation()
//                        .description("API Documentation"));
//    }
//}
@Bean
public OpenAPI customOpenAPI() {

    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
            .info(
                    new Info()
                            .title("Caryanam Bidding")
                            .version("1.0")
                            .description("Caryanam-Bidding")
                            .contact(
                                    new Contact()
                                            .name("Caryanam Bidding")
                                            .email("support@caryanamaBidding.com")
                            )
            )
            .addSecurityItem(
                    new SecurityRequirement()
                            .addList(securitySchemeName)
            )
            .components(
                    new Components()
                            .addSecuritySchemes(
                                    securitySchemeName,
                                    new SecurityScheme()
                                            .name(securitySchemeName)
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")
                            )
            );




}
}

