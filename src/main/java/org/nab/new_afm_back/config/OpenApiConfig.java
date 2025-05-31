package org.nab.new_afm_back.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.dev-url:http://localhost:8080}")
    private String devUrl;

    @Value("${app.openapi.prod-url:}")
    private String prodUrl;

    @Bean
    public OpenAPI myOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl(devUrl);
        devServer.setDescription("Server URL in Development environment");

        Contact contact = new Contact();
        contact.setEmail("superback@example.com");
        contact.setName("AFM Development Team");

        License mitLicense = new License().name("MIT License").url("https://choosealicense.com/licenses/mit/");

        Info info = new Info()
                .title("AFM Case Management API")
                .version("1.0")
                .contact(contact)
                .description("This API exposes endpoints for managing cases and PDF documents in AFM system.")
                .license(mitLicense);

        OpenAPI openAPI = new OpenAPI().info(info);
        openAPI.servers(List.of(devServer));

        if (!prodUrl.isEmpty()) {
            Server prodServer = new Server();
            prodServer.setUrl(prodUrl);
            prodServer.setDescription("Server URL in Production environment");
            openAPI.servers(List.of(devServer, prodServer));
        }

        return openAPI;
    }
}