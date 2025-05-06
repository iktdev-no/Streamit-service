package no.iktdev.streamit.service

import no.iktdev.streamit.library.db.datasource.MySqlDataSource
import no.iktdev.streamit.shared.Env
import org.h2.jdbcx.JdbcDataSource
import org.jetbrains.exposed.sql.Database
import java.sql.Connection

class H2Datasource(databaseName: String) : MySqlDataSource(
    databaseName = databaseName,
    address = "",
    port = "",
    username = "",
    password = ""
) {

    /*override fun getConnection(): Connection {
        return jdbcDataSource.connection
    }

    override fun getConnection(username: String?, password: String?): Connection {
        return jdbcDataSource.getConnection(username, password)
    }

    override fun setLoginTimeout(seconds: Int) {
        jdbcDataSource.loginTimeout = seconds
    }

    override fun getLoginTimeout(): Int {
        return jdbcDataSource.loginTimeout
    }

    override fun getLogWriter(): PrintWriter? {
        return jdbcDataSource.logWriter
    }

    override fun setLogWriter(out: PrintWriter?) {
        jdbcDataSource.logWriter = out
    }

    override fun getParentLogger(): Logger? {
        throw SQLFeatureNotSupportedException("getParentLogger is not supported")
    }

    override fun <T : Any?> unwrap(iface: Class<T>?): T {
        if (iface != null && iface.isAssignableFrom(this.javaClass)) {
            return this as T
        }
        return jdbcDataSource.unwrap(iface)
    }

    override fun isWrapperFor(iface: Class<*>?): Boolean {
        if (iface != null && iface.isAssignableFrom(this.javaClass)) {
            return true
        }
        return jdbcDataSource.isWrapperFor(iface)
    }*/

    override fun createDatabaseStatement(): String {
        return "CREATE SCHEMA $databaseName"
    }

    override fun toConnectionUrl(): String {
        return "jdbc:h2:mem:$databaseName;MODE=MYSQL;DB_CLOSE_DELAY=-1;" // "jdbc:h2:mem:test;MODE=MySQL;DB_CLOSE_DELAY=-1;"
    }

    fun toDatabase2(): Database {
        return Database.connect(
            toConnectionUrl(),
            user = username,
            password = password
        )
    }

}