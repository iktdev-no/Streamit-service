package no.iktdev.streamit.service.config

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SwaggerUiRoutes {
    @GetMapping("/swagger", produces = [MediaType.TEXT_HTML_VALUE])
    fun swaggerIndex(): String = """
        <!doctype html>
        <html lang="en">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>Streamit API documentation</title>
        </head>
        <body>
            <h1>Streamit API documentation</h1>
            <ul>
                <li><a href="/open/swagger-ui.html">Open API</a></li>
                <li><a href="/secure/swagger-ui.html">Secure API</a></li>
            </ul>
        </body>
        </html>
    """.trimIndent()

    @GetMapping("/open/swagger-ui.html", produces = [MediaType.TEXT_HTML_VALUE])
    fun openRouteSwaggerUi(): String = swaggerUi("open")

    @GetMapping("/secure/swagger-ui.html", produces = [MediaType.TEXT_HTML_VALUE])
    fun secureRouteSwaggerUi(): String = swaggerUi("secure")

    private fun swaggerUi(group: String): String = """
        <!doctype html>
        <html lang="en">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>Streamit API - $group</title>
            <link rel="stylesheet" href="/swagger-ui/swagger-ui.css" />
        </head>
        <body>
            <div id="swagger-ui"></div>
            <script src="/swagger-ui/swagger-ui-bundle.js"></script>
            <script src="/swagger-ui/swagger-ui-standalone-preset.js"></script>
            <script>
                window.onload = () => SwaggerUIBundle({
                    url: "/v3/api-docs/$group",
                    dom_id: "#swagger-ui",
                    deepLinking: true,
                    presets: [SwaggerUIBundle.presets.apis, SwaggerUIStandalonePreset],
                    plugins: [SwaggerUIBundle.plugins.DownloadUrl],
                    layout: "StandaloneLayout"
                });
            </script>
        </body>
        </html>
    """.trimIndent()
}
