package com.littleetx.sustechnav.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

private val logger = LoggerFactory.getLogger("DatabaseModule")

const val packageName = "com.littleetx.sustechnav.database"


private fun initializeTables() {
    try {
        transaction {
            // 扫描所有Table子类
            val reflections = Reflections(packageName)
            val tableClasses = reflections.getSubTypesOf(Table::class.java)
                .filter { it.name.startsWith(packageName) }
                .filter { !Modifier.isAbstract(it.modifiers) }

            logger.info("Found ${tableClasses.size} tables to initialize")

            // 创建表结构
            tableClasses.forEach { tableClass ->
                val tableInstance = tableClass.kotlin.objectInstance ?: tableClass.getDeclaredConstructor().newInstance()
                SchemaUtils.create(tableInstance as Table)
                logger.debug("Initialized table: ${tableClass.simpleName}")
            }
        }
    } catch (e: Exception) {
        logger.error("Table initialization failed", e)
    }
}


fun Application.applyProperty(name: String, block: (String) -> Unit) {
    environment.config.propertyOrNull(name)?.let{ block(it.getString()) }
}


fun Application.module() {

    val config = HikariConfig().apply {
        applyProperty("database.url") { jdbcUrl = it }
        applyProperty("database.driver") { driverClassName = it }
        applyProperty("database.username") { username = it }
        applyProperty("database.password") { password = it }
        applyProperty("database.maximumPoolSize") { maximumPoolSize = it.toInt() }
        // as of version 0.46.0, if these options are set here, they do not need to be duplicated in DatabaseConfig
        applyProperty("database.isReadOnly") { isReadOnly = it.toBoolean() }
        applyProperty("database.transactionIsolation") { transactionIsolation = it }
    }

    val dataSource = HikariDataSource(config)

    Database.connect(
        datasource = dataSource,
        databaseConfig = DatabaseConfig {
            // set other parameters here
        }
    )

    initializeTables()
}