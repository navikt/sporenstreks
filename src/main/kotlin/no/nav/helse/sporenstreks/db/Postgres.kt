package no.nav.helse.spion.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.util.KtorExperimentalAPI
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil


enum class Role {
    admin, user, readonly;

    override fun toString() = name.toLowerCase()
}


fun getDataSource(hikariConfig: HikariConfig, dbName: String, vaultMountpath: String?) =
        if (!vaultMountpath.isNullOrEmpty()) {
            dataSourceFromVault(hikariConfig, dbName, vaultMountpath, Role.user)
        } else {
            HikariDataSource(hikariConfig)
        }

@KtorExperimentalAPI
fun dataSourceFromVault(hikariConfig: HikariConfig, dbName: String, vaultMountpath: String, role: Role): HikariDataSource {
    return HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
            hikariConfig,
            vaultMountpath,
            "${dbName}-$role")
}
