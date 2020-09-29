package no.nav.helse.sporenstreks.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.metrics.prometheus.PrometheusMetricsTrackerFactory

fun createHikariConfig(jdbcUrl: String, username: String? = null, password: String? = null, prometheusMetricsTrackerFactory: PrometheusMetricsTrackerFactory? = null) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 7
            minimumIdle = 2
            idleTimeout = 10001
            connectionTimeout = 2000
            maxLifetime = 30001
            driverClassName = "org.postgresql.Driver"
            username?.let { this.username = it }
            password?.let { this.password = it }
            poolName = "defaultPool"
            prometheusMetricsTrackerFactory?.let { metricsTrackerFactory = prometheusMetricsTrackerFactory }
        }


fun createNonVaultHikariConfig() =
        createHikariConfig("jdbc:postgresql://localhost:5432/sporenstreks", "sporenstreks", "sporenstreks", null)
