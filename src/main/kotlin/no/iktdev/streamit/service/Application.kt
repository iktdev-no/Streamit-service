package no.iktdev.streamit.service

import mu.KotlinLogging
import no.iktdev.streamit.library.db.datasource.MySqlDataSource
import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
import no.iktdev.streamit.library.db.tables.authentication.RegisteredDevicesTable
import no.iktdev.streamit.library.db.tables.content.*
import no.iktdev.streamit.library.db.tables.other.CastErrorTable
import no.iktdev.streamit.library.db.tables.other.DataAudioTable
import no.iktdev.streamit.library.db.tables.other.DataVideoTable
import no.iktdev.streamit.library.db.tables.user.ProfileImageTable
import no.iktdev.streamit.library.db.tables.user.UserTable
import no.iktdev.streamit.library.db.withTransaction
import no.iktdev.streamit.shared.Env
import no.iktdev.streamit.shared.database.AccessTokenTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletRequestWrapper

val log = KotlinLogging.logger {}

@SpringBootApplication
class Application {
}

var context: ApplicationContext? = null
fun main(args: Array<String>) {
    val datasource = MySqlDataSource.fromDatabaseEnv()
    val database = datasource.createDatabase().also { x ->
        println(x)
    } ?: throw RuntimeException("Unable to create database..")
    databaseSetup(database)
    context = runApplication<Application>(*args)
}

fun getTables(): Array<Table> {
    val contentTables = arrayOf<Table>(
        CatalogTable,
        GenreTable,
        MovieTable,
        SerieTable,
        SubtitleTable,
        SummaryTable,
        TitleTable,
        ProgressTable,
        ContinueWatchTable,
    )
    val userTable = arrayOf<Table>(
        UserTable,
        ProfileImageTable
    )

    val authTables = arrayOf<Table>(
        DelegatedAuthenticationTable,
        RegisteredDevicesTable,
        AccessTokenTable
    )

    val miscTables = arrayOf<Table>(
        CastErrorTable,
        DataAudioTable,
        DataVideoTable
    )

    return contentTables + userTable + authTables + miscTables
}

fun databaseSetup(database: Database) {
    withTransaction(database) {
        SchemaUtils.createMissingTablesAndColumns(*getTables())
        log.info("Database transaction completed")
    }
}

@Configuration
class InterceptorConfiguration(
    @Autowired val authInterceptor: AuthorizationInterceptor
): WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        super.addInterceptors(registry)
        log.info { "Adding AuthorizationInterceptor" }
        registry.addInterceptor(authInterceptor).addPathPatterns("/**")
    }

    override fun configurePathMatch(configurer: PathMatchConfigurer) {
        super.configurePathMatch(configurer)
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(ApiRestController::class.java))
        configurer.addPathPrefix("/stream", HandlerTypePredicate.forAnnotation(ContentRestController::class.java))
        configurer.addPathPrefix("/assets", HandlerTypePredicate.forAnnotation(AssetRestController::class.java))
    }
}

@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedOrigins(*Env.getAllowedOrigins().toTypedArray()) // or specify allowed origins
            .allowedMethods("*")
            .allowCredentials(true)
    }
}

@Component
@Order(2)
class PathDefiner : Filter {

    private fun setModifiedPath(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val uri = httpRequest.requestURI

        val accessModes = listOf("open", "secure")
        var mode = "unknown"
        var internalPath = uri

        for (access in accessModes) {
            when {
                uri.startsWith("/$access/api") -> {
                    mode = access
                    internalPath = "/api" + uri.removePrefix("/$access/api")
                    break
                }
                uri.startsWith("/$access/stream") -> {
                    mode = access
                    internalPath = "/stream" + uri.removePrefix("/$access/stream")
                    break
                }
                uri.startsWith("/$access/assets") -> {
                    mode = access
                    internalPath = "/assets" + uri.removePrefix("/$access/assets")
                    break
                }
            }
        }

        request.removeAttribute("internalAccessMode")
        request.setAttribute("internalAccessMode", mode)

        val modifiedRequest = object : HttpServletRequestWrapper(httpRequest) {
            override fun getRequestURI(): String {
                return internalPath
            }
        }
        chain.doFilter(modifiedRequest, response)
    }


    private fun setSinglePath(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        request.setAttribute("internalAccessMode", "open")
        chain.doFilter(request, response)
    }

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (Env.singleEntryPaths) {
            setSinglePath(request, response, chain)
        } else {
            setModifiedPath(request, response, chain)
        }
    }
}

