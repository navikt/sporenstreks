package no.nav.helse.sporenstreks.db

import javax.sql.DataSource
import kotlin.collections.ArrayList

data class AntallKravStatsUke(
    val weekNumber: Int,
    val antall_web: Int,
    val antall_excel: Int
)
interface IStatsRepo {
    fun getAntallKravStatsUke(): List<AntallKravStatsUke>
}

class StatsRepoImpl(
    private val ds: DataSource
) : IStatsRepo {
    override fun getAntallKravStatsUke(): List<AntallKravStatsUke> {
        val query = """
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<AntallKravStatsUke>()
            while (res.next()) {
                returnValue.add(
                    AntallKravStatsUke(
                        res.getInt("uke"),
                        res.getInt("antall_web"),
                        res.getInt("antall_excel")
                    )
                )
            }
            return returnValue
        }
    }
}
