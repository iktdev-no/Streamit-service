package no.iktdev.streamit.service.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {
    @Bean
    fun streamitOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Streamit Service API")
                .version("1.0")
                .description(
                    "Streamit API. Public clients should use the /secure route. " +
                        "Protected endpoints accept a device-issued JWT in the Authorization: Bearer header. " +
                        "The required Streamit scope is documented on each operation."
                )
        )
        .servers(listOf(Server().url("/secure").description("Public API via reverse proxy")))
        .components(
            Components().addSecuritySchemes(
                DEVICE_BEARER_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description(
                        "Device token issued by Streamit. Paste the token value only; Swagger UI adds " +
                            "the Bearer prefix. Token scopes are Streamit claims, not OAuth scopes."
                    )
            )
        )

    @Bean
    fun streamitOperationCustomizer(): OperationCustomizer = OperationCustomizer { operation, handlerMethod ->
        val requirement = handlerMethod.getMethodAnnotation(RequiresAuthentication::class.java)
        val scope = requirement?.withScope

        if (scope != null && scope != Scope.None) {
            operation.addSecurityItem(SecurityRequirement().addList(DEVICE_BEARER_SCHEME))
            operation.addExtension("x-streamit-required-scope", scope.name)
            val authDescription = "Requires a device Bearer JWT with the Streamit scope `${scope.name}`."
            operation.description = listOfNotNull(operation.description, authDescription)
                .filter(String::isNotBlank)
                .joinToString("\n\n")
            operation.responses = (operation.responses ?: ApiResponses())
                .addApiResponse("401", ApiResponse().description("Missing, expired, revoked, or invalid device token"))
                .addApiResponse("403", ApiResponse().description("Device token does not include the required scope"))
        } else if (requirement == null) {
            operation.addExtension(
                "x-streamit-authentication",
                "No @RequiresAuthentication annotation; current interceptor treats this operation as unscoped."
            )
        }

        operation
    }

    private companion object {
        const val DEVICE_BEARER_SCHEME = "deviceBearer"
    }
}
