package no.iktdev.streamit.service

import mu.KotlinLogging
import no.iktdev.exfl.coroutines.CoroutinesIO
import no.iktdev.streamit.library.db.datasource.DataSource
import no.iktdev.streamit.library.db.datasource.MySqlDataSource
import no.iktdev.streamit.library.db.tables.authentication.DelegatedAuthenticationTable
import no.iktdev.streamit.library.db.tables.authentication.RegisteredDevicesTable
import no.iktdev.streamit.library.db.tables.content.CatalogTable
import no.iktdev.streamit.library.db.tables.content.ContinueWatchTable
import no.iktdev.streamit.library.db.tables.content.GenreTable
import no.iktdev.streamit.library.db.tables.content.MovieTable
import no.iktdev.streamit.library.db.tables.content.ProgressTable
import no.iktdev.streamit.library.db.tables.content.SerieTable
import no.iktdev.streamit.library.db.tables.content.SubtitleTable
import no.iktdev.streamit.library.db.tables.content.SummaryTable
import no.iktdev.streamit.library.db.tables.content.TitleTable
import no.iktdev.streamit.library.db.tables.other.CastErrorTable
import no.iktdev.streamit.library.db.tables.other.DataAudioTable
import no.iktdev.streamit.library.db.tables.other.DataVideoTable
import no.iktdev.streamit.library.db.tables.user.ProfileImageTable
import no.iktdev.streamit.library.db.tables.user.UserTable
import no.iktdev.streamit.library.db.withTransaction
import no.iktdev.streamit.service.api.GeneralController
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerTypePredicate
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
        RegisteredDevicesTable
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
        configurer.addPathPrefix("/api/**", HandlerTypePredicate.forAnnotation(ApiRestController::class.java))
        configurer.addPathPrefix("/stream/**", HandlerTypePredicate.forAnnotation(ContentRestController::class.java))
    }
}

