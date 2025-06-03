package org.nab.new_afm_back.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.dev-url:http://localhost:5623}")
    private String devUrl;

    @Value("${app.openapi.prod-url:http://192.168.122.47:5623}")
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

        // Enhanced components for better multipart support
        Components components = new Components();

        // Define file upload schema
        Schema<?> fileSchema = new Schema<>()
                .type("string")
                .format("binary")
                .description("File to upload");

        // Define multipart file array schema
        Schema<?> fileArraySchema = new Schema<>()
                .type("array")
                .items(fileSchema)
                .description("Array of files to upload");

        // Define case upload request schema
        Schema<?> caseUploadSchema = new Schema<>()
                .type("object")
                .description("Case upload request with multipart form data")
                .addProperty("caseData", new Schema<>()
                        .type("string")
                        .description("Case data in JSON format")
                        .example("{\"number\": \"CASE001\", \"author\": \"John Doe\", \"title\": \"Sample Case\"}"))
                .addProperty("additionalFiles", fileArraySchema)
                .addProperty("categories", new Schema<>()
                        .type("array")
                        .items(new Schema<>().type("string"))
                        .description("Categories for the uploaded files")
                        .example("[\"Document\", \"Evidence\"]"));

        components.addSchemas("File", fileSchema);
        components.addSchemas("FileArray", fileArraySchema);
        components.addSchemas("CaseUploadRequest", caseUploadSchema);

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .components(components);

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