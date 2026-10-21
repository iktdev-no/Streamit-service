package no.iktdev.streamit.service.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.media.Discriminator
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.core.converter.ModelConverters
import org.springdoc.core.models.GroupedOpenApi
import no.iktdev.streamit.service.auth.RequiresAuthentication
import no.iktdev.streamit.service.auth.Scope
import no.iktdev.streamit.service.controller.api.GeneralController
import no.iktdev.streamit.service.controller.api.authentication.AuthenticationController
import no.iktdev.streamit.service.controller.api.content.AssetsApiController
import no.iktdev.streamit.service.controller.api.content.CatalogController
import no.iktdev.streamit.service.controller.api.content.FavoritesController
import no.iktdev.streamit.service.controller.api.content.GenreController
import no.iktdev.streamit.service.controller.api.content.SearchController
import no.iktdev.streamit.service.controller.api.content.SummaryController
import no.iktdev.streamit.service.controller.api.content.VideoProgressController
import no.iktdev.streamit.service.controller.api.import.MediaImportController
import no.iktdev.streamit.service.controller.api.meta.CastErrorController
import no.iktdev.streamit.service.controller.api.meta.DeviceRegistrationController
import no.iktdev.streamit.service.controller.api.meta.MetadataController
import no.iktdev.streamit.service.controller.api.meta.RemoteController
import no.iktdev.streamit.service.controller.api.user.UserController
import no.iktdev.streamit.service.controller.assets.AssetsController
import no.iktdev.streamit.service.controller.content.ContentController
import no.iktdev.streamit.service.model.shared.CollectionReference
import no.iktdev.streamit.service.model.shared.ContentType
import no.iktdev.streamit.service.model.shared.consumer.content.Movie
import no.iktdev.streamit.service.model.shared.consumer.content.Serie
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
    fun openRouteApiDocs(): GroupedOpenApi = routeApiGroup("open", "/open")

    @Bean
    fun secureRouteApiDocs(): GroupedOpenApi = routeApiGroup("secure", "/secure")

    @Bean
    fun consumerApiDocs(): GroupedOpenApi = contractApiGroup("consumer", consumerControllers)

    @Bean
    fun importingApiDocs(): GroupedOpenApi = contractApiGroup("importing", importingControllers)

    private fun routeApiGroup(group: String, serverPath: String): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group(group)
            .addOpenApiMethodFilter { method -> method.declaringClass in routeControllers }
            .addOpenApiCustomizer { api ->
                api.servers(
                    listOf(
                        Server().url(serverPath).description("Streamit $group route")
                    )
                )
                addSharedContractSchemas(api)
            }
            .build()

    private fun contractApiGroup(group: String, controllers: Set<Class<*>>): GroupedOpenApi =
        GroupedOpenApi.builder()
            .group(group)
            .addOpenApiMethodFilter { method -> method.declaringClass in controllers }
            .addOpenApiCustomizer { api ->
                api.info(api.info.title("Streamit ${group.replaceFirstChar { it.uppercase() }} API"))
                api.servers(listOf(Server().url("/secure").description("Public API via reverse proxy")))
                addSharedContractSchemas(api)
            }
            .build()

    private fun addSharedContractSchemas(api: OpenAPI) {
        val components = api.components ?: Components().also { api.components = it }
        listOf(
            ContentType::class.java,
            CollectionReference::class.java,
            Movie::class.java,
            Serie::class.java
        ).forEach { type ->
            ModelConverters.getInstance().readAll(type).forEach { (name, schema) ->
                if (components.schemas?.containsKey(name) != true) {
                    components.addSchemas(name, schema)
                }
            }
        }
        exposeCatalogVariants(api, components)
    }

    /** Put the Movie/Serie union on arrays of Catalog while keeping Catalog a plain base schema. */
    private fun exposeCatalogVariants(api: OpenAPI, components: Components) {
        val catalogRef = "#/components/schemas/Catalog"
        val unionSchema = Schema<Any>().oneOf(
            listOf(
                Schema<Any>().`$ref`("#/components/schemas/Movie"),
                Schema<Any>().`$ref`("#/components/schemas/Serie")
            )
        )

        components.schemas?.forEach { (name, component) ->
            if (name == "Movie" || name == "Serie") {
                val required = component.required
                component.allOf?.firstOrNull { it.properties != null }?.required = required
            }
            component.properties?.values?.forEach { property ->
                if (property.type == "array" && property.items?.`$ref` == catalogRef) {
                    property.items = unionSchema
                }
            }
        }

        api.paths?.values?.forEach { pathItem ->
            pathItem.readOperations()?.forEach { operation ->
                operation.responses?.values?.forEach { response ->
                    response.content?.values?.forEach { mediaType ->
                        val schema = mediaType.schema
                        if (schema?.type == "array" && schema.items?.`$ref` == catalogRef) {
                            schema.items = unionSchema
                        }
                    }
                }
            }
        }
    }

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

        val importingControllers = setOf(
            MediaImportController::class.java,
            MetadataController::class.java
        )

        val consumerControllers = setOf(
            GeneralController::class.java,
            AuthenticationController::class.java,
            AssetsApiController::class.java,
            CatalogController::class.java,
            FavoritesController::class.java,
            GenreController::class.java,
            SearchController::class.java,
            SummaryController::class.java,
            VideoProgressController::class.java,
            CastErrorController::class.java,
            DeviceRegistrationController::class.java,
            RemoteController::class.java,
            UserController::class.java,
            AssetsController::class.java,
            ContentController::class.java
        )

        val routeControllers = consumerControllers + importingControllers
    }
}
