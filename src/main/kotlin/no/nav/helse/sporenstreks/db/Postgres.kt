package no.nav.helse.sporenstreks.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil

enum class Role {
    admin, user, readonly;

    override fun toString() = name.lowercase()
}

fun getDataSource(hikariConfig: HikariConfig, dbName: String, vaultMountpath: String?) =
    if (!vaultMountpath.isNullOrEmpty()) {
        dataSourceFromVault(hikariConfig, dbName, vaultMountpath, Role.user)
    } else {
        HikariDataSource(hikariConfig)
    }

fun dataSourceFromVault(hikariConfig: HikariConfig, dbName: String, vaultMountpath: String, role: Role): HikariDataSource {
    return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
        hikariConfig,
        vaultMountpath,
        "$dbName-$role"
    )
}
