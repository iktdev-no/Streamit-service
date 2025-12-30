package no.iktdev.streamit.service.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.iktdev.streamit.service.db.Access
import no.iktdev.streamit.service.db.DbType
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@TestConfiguration
class DatasourceConfiguration {
    @Bean
    @Primary
    fun dataSource(): DataSource {
        val access = Access(
            username = "sa",
            password = "",
            address = "", // ikke brukt for H2
            port = 0,     // ikke brukt for H2
            databaseName = "testdb",
            dbType = DbType.H2
        )

        val maxPoolSize: Int = 10
        val config = HikariConfig().apply {
            this.jdbcUrl = "jdbc:h2:mem:${access.databaseName};MODE=MySQL;DB_CLOSE_DELAY=-1"
            this.driverClassName = "org.h2.Driver"
            this.username = access.username
            this.password = access.password
            this.maximumPoolSize = maxPoolSize
            this.isAutoCommit = false
            this.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            this.validate()
        }

        return HikariDataSource(config)
    }
}