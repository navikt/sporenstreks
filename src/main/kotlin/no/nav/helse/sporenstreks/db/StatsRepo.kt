package no.nav.helse.sporenstreks.db

import javax.sql.DataSource
import kotlin.collections.ArrayList

data class AntallKravStatsUke(
    val weekNumber: Int,
    val antall_web: Int,
    val antall_excel: Int,
    val antall_tariffendring: Int
)
interface IStatsRepo {
    fun getAntallKravStatsUke(): List<AntallKravStatsUke>
}

class StatsRepoImpl(
    private val ds: DataSource
) : IStatsRepo {
    override fun getAntallKravStatsUke(): List<AntallKravStatsUke> {
        val query = """
            SELECT
                extract('week' from date(data->>'opprettet')) as uke,
                count(*) filter ( where data->>'kilde' = 'WEBSKJEMA' ) AS antall_web,
                count(*) filter ( where data->>'kilde' LIKE 'XLSX%' AND (data->'tariffEndring')::boolean = false) AS antall_excel,
                count(*) filter ( where data->>'kilde' LIKE 'XLSX%' AND (data->'tariffEndring')::boolean = true) AS antall_tariffendring
            FROM refusjonskrav where date(data->>'opprettet') > '2022-01-02' group by uke order by uke;
        """.trimIndent()

        ds.connection.use {
            val res = it.prepareStatement(query).executeQuery()
            val returnValue = ArrayList<AntallKravStatsUke>()
            while (res.next()) {
                returnValue.add(
                    AntallKravStatsUke(
                        res.getInt("uke"),
                        res.getInt("antall_web"),
                        res.getInt("antall_excel"),
                        res.getInt("antall_tariffendring")
                    )
                )
            }
            return returnValue
        }
    }
}
