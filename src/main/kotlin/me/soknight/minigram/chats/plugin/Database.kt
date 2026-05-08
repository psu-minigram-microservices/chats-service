package me.soknight.minigram.chats.plugin

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.soknight.minigram.chats.config.DatabaseConfig
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

fun Application.configureDatabase(config: DatabaseConfig) {
    val ds = HikariDataSource(HikariConfig().apply {
        jdbcUrl         = config.url
        driverClassName = config.driver
        username        = config.username
        password        = config.password
        maximumPoolSize = config.maxPoolSize
        minimumIdle     = config.minIdle
    })

    Flyway.configure()
        .dataSource(ds)
        .locations("classpath:db/migration")
        .schemas("public")
        .load()
        .migrate()

    Database.connect(ds)
}

suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
    withContext(Dispatchers.IO) {
        suspendTransaction(statement = block)
    }
