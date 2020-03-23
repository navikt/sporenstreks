package no.nav.helse.spion.db

import com.zaxxer.hikari.HikariConfig

fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
            driverClassName = "org.postgresql.Driver"
            username?.let { this.username = it }
            password?.let { this.password = it }
        }


fun createLocalHikariConfig() =
        createHikariConfig("jdbc:postgresql://localhost:5432/spion", "spion", "spion")