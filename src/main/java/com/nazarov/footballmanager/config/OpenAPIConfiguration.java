package com.nazarov.footballmanager.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfiguration {

  @Bean
  public OpenAPI defineOpenApi() {
    String authSchema = "bearerAuth";

    Server serverDev = new Server();
    serverDev.setUrl("http://localhost:8082");
    serverDev.description("Development");

    Server serverQa = new Server();
    serverQa.setUrl("http://localhost:8083");
    serverQa.setDescription("QA");

    Contact myContact = new Contact();
    myContact.setName("Nazarov Development Team");
    myContact.setEmail("your.email@gmail.com");

    Components components = new Components();
    components.addSecuritySchemes(authSchema,
        new SecurityScheme()
            .name(authSchema)
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT"));

    Info information = new Info()
        .title("Football Tournament API")
        .version("1.0")
        .description(
            "REST API for managing football tournaments, leagues, teams, players, and statistics.")
        .contact(myContact);

    return new OpenAPI().info(information)
        .addServersItem(serverDev)
        .addServersItem(serverQa)
        .components(components);
  }
}
