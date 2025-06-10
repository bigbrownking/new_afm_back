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
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.dev-url:http://localhost:5623}")
    private String devUrl;

    @Value("${app.openapi.prod-url:http://192.168.122.4:5623}")
    private String prodUrl1;

    @Value("${app.openapi.prod-url:http://192.168.122.47:5623}")
    private String prodUrl2;

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

        Components components = new Components();

        Schema<?> fileSchema = new Schema<>()
                .type("string")
                .format("binary")
                .description("File to upload");

        Schema<?> fileArraySchema = new Schema<>()
                .type("array")
                .items(fileSchema)
                .description("Array of files to upload");

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

        if (!prodUrl1.isEmpty() || !prodUrl2.isEmpty()) {
            Server prodServer1 = new Server();
            prodServer1.setUrl(prodUrl1);
            prodServer1.setDescription("Server URL in Production environment 122.4");

            Server prodServer2 = new Server();
            prodServer2.setUrl(prodUrl2);
            prodServer2.setDescription("Server URL in Production environment 122.47");
            openAPI.servers(List.of(devServer, prodServer1, prodServer2));
        }

        return openAPI;
    }
    public OpenApiConfig(MappingJackson2HttpMessageConverter converter) {
        var supportedMediaTypes = new ArrayList<>(converter.getSupportedMediaTypes());
        supportedMediaTypes.add(new MediaType("application", "octet-stream"));
        converter.setSupportedMediaTypes(supportedMediaTypes);
    }
}