package no.nav.helse.sporenstreks.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import org.slf4j.LoggerFactory
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresRefusjonskravRepository(val ds: DataSource, val mapper: ObjectMapper) : RefusjonskravRepository {
    private val logger = LoggerFactory.getLogger(PostgresRefusjonskravRepository::class.java)
    private val tableName = "refusjonskrav"

    private val getByVirksomhetsnummerStatement = """SELECT data::json FROM $tableName 
            WHERE data ->> 'virksomhetsnummer' = ?;"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val getByIdentitetsnummerAndVirksomhetsnummerStatement = """SELECT data::json FROM $tableName 
         WHERE data ->> 'identitetsnummer' = ?
            AND data ->> 'virksomhetsnummer' = ?;"""

    private val deleteStatement = "DELETE FROM $tableName WHERE data ->> 'id' = ?"

    override fun getAllForVirksomhet(virksomhetsnummer: String): List<Refusjonskrav> {
        ds.connection.use { con ->
            val resultList = ArrayList<Refusjonskrav>()
            val res = con.prepareStatement(getByVirksomhetsnummerStatement).apply {
                setString(1, virksomhetsnummer)
            }.executeQuery()

            while (res.next()) {
                resultList.add(mapper.readValue(res.getString("data")))
            }
            return resultList
        }
    }

    override fun insert(refusjonskrav: Refusjonskrav) {
        val json = mapper.writeValueAsString(refusjonskrav)
        ds.connection.use {
            it.prepareStatement(saveStatement).apply {
                setString(1, json)
            }.executeUpdate()
        }
    }

    override fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav> {
        ds.connection.use {
            val existingYpList = ArrayList<Refusjonskrav>()
            val res = it.prepareStatement(getByIdentitetsnummerAndVirksomhetsnummerStatement).apply {
                setString(1, identitetsnummer)
                setString(2, virksomhetsnummer)
            }.executeQuery()

            while (res.next()) {
                existingYpList.add(mapper.readValue(res.getString("data")))
            }

            return existingYpList
        }
    }


    override fun delete(id: UUID): Int {
        ds.connection.use {
            return it.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }

}