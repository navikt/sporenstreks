package no.nav.helse.sporenstreks.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.domene.RefusjonskravStatus
import org.postgresql.jdbc.PgArray
import org.slf4j.LoggerFactory
import java.io.IOException
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresRefusjonskravRepository(val ds: DataSource, val mapper: ObjectMapper) : RefusjonskravRepository {
    private val logger = LoggerFactory.getLogger(PostgresRefusjonskravRepository::class.java)

    private val tableName = "refusjonskrav"
    private val getByVirksomhetsnummerStatement = """SELECT * FROM $tableName 
            WHERE data ->> 'virksomhetsnummer' = ?;"""

    private val getByStatuses = """SELECT * FROM $tableName 
            WHERE data ->> 'status' = ?;"""

    private val getByIdStatement = """SELECT * FROM $tableName WHERE data ->> 'id' = ?"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val updateStatement = "UPDATE $tableName SET data = ?::json WHERE data ->> 'id' = ?;"

    private val getByIdentitetsnummerAndVirksomhetsnummerStatement = """SELECT * FROM $tableName 
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
                resultList.add(extractRefusjonskrav(res))
            }
            return resultList
        }
    }

    override fun getByStatus(status: RefusjonskravStatus): List<Refusjonskrav> {
        ds.connection.use { con ->
            val resultList = ArrayList<Refusjonskrav>()
            val res = con.prepareStatement(getByStatuses).apply {
                setString(1, status.toString())
            }.executeQuery()

            while (res.next()) {
                resultList.add(extractRefusjonskrav(res))
            }
            return resultList
        }
    }

    override fun bulkInsert(kravListe: List<Refusjonskrav>): List<Int> {
        logger.info("Starter serialisering av ${kravListe.size} krav")
        val jsonListe = kravListe.map { mapper.writeValueAsString(it) } // hold denne utenfor connection.use
        logger.info("Serialisering ferdig, starter en tilkobling og sender")
        ds.connection.use { con ->
            try {
                con.autoCommit = false

                val statement = con.prepareStatement(saveStatement, PreparedStatement.RETURN_GENERATED_KEYS)

                for (json in jsonListe) {
                    statement.setString(1, json)
                    statement.addBatch()
                }

                statement.executeBatch()
                con.commit()
                logger.info("Comittet")

                val referanseNummere = ArrayList<Int>(kravListe.size)
                while (statement.generatedKeys.next()) {
                    referanseNummere.add(statement.generatedKeys.getInt(2))
                }

                return@bulkInsert referanseNummere
            } catch (e: SQLException) {
                logger.error("Ruller tilbake bulkinnsetting")
                try {
                    con.rollback()
                } catch (ex: Exception) {
                    logger.error("Klarte ikke rulle tilbake bulkinnsettingen", ex)
                }

                throw e
            }
        }
    }

    override fun update(krav: Refusjonskrav) {
        val json = mapper.writeValueAsString(krav)
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, json)
                setString(2, krav.id.toString())
            }.executeUpdate()
        }
    }

    override fun getById(id: UUID): Refusjonskrav? {2
        ds.connection.use {
            val existingYpList = ArrayList<Refusjonskrav>()
            val res = it.prepareStatement(getByIdStatement).apply {
                setString(1, id.toString())
            }.executeQuery()

            while (res.next()) {
                existingYpList.add(extractRefusjonskrav(res))
            }

            return existingYpList.firstOrNull()
        }
    }


    override fun insert(refusjonskrav: Refusjonskrav): Refusjonskrav {
        val json = mapper.writeValueAsString(refusjonskrav)
        ds.connection.use {
            it.prepareStatement(saveStatement).apply {
                setString(1, json)
            }.executeUpdate()
        }

        return getById(refusjonskrav.id) ?: throw IOException("Unable to read receipt for refusjonskrav with id ${refusjonskrav.id}")
    }

    override fun getExistingRefusjonskrav(identitetsnummer: String, virksomhetsnummer: String): List<Refusjonskrav> {
        ds.connection.use {
            val existingYpList = ArrayList<Refusjonskrav>()
            val res = it.prepareStatement(getByIdentitetsnummerAndVirksomhetsnummerStatement).apply {
                setString(1, identitetsnummer)
                setString(2, virksomhetsnummer)
            }.executeQuery()

            while (res.next()) {
                existingYpList.add(extractRefusjonskrav(res))
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

    private fun extractRefusjonskrav(res: ResultSet): Refusjonskrav {
        val refusjonsKrav = mapper.readValue<Refusjonskrav>(res.getString("data"))
        refusjonsKrav.referansenummer = res.getInt("referansenummer")
        return refusjonsKrav
    }

}