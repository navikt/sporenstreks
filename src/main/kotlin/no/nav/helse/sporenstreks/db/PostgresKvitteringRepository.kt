package no.nav.helse.sporenstreks.db

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.sporenstreks.kvittering.Kvittering
import no.nav.helse.sporenstreks.kvittering.KvitteringStatus
import java.io.IOException
import java.sql.Connection
import java.sql.ResultSet
import java.util.*
import javax.sql.DataSource
import kotlin.collections.ArrayList

class PostgresKvitteringRepository(val ds: DataSource, val mapper: ObjectMapper) : KvitteringRepository {

    private val tableName = "kvittering"

    private val getByStatuses = """SELECT * FROM $tableName
        WHERE data ->> 'status' = ? LIMIT ?;"""

    private val saveStatement = "INSERT INTO $tableName (data) VALUES (?::json);"

    private val updateStatement = "UPDATE $tableName SET data = ?::json WHERE data ->> 'id' = ?;"

    private val getByIdStatement = """SELECT * FROM $tableName WHERE data ->> 'id' = ?"""

    private val deleteStatement = "DELETE FROM $tableName WHERE data ->> 'id' = ?"


    override fun insert(kvittering: Kvittering): Kvittering {
        val json = mapper.writeValueAsString(kvittering)
        ds.connection.use {
            it.prepareStatement(saveStatement).apply {
                setString(1, json)
            }.executeUpdate()
        }

        return getById(kvittering.id)
                ?: throw IOException("Unable to read receipt for kvittering with id ${kvittering.id}")
    }

    override fun insert(kvittering: Kvittering, connection: Connection): Kvittering {
        val json = mapper.writeValueAsString(kvittering)
        connection.prepareStatement(saveStatement).apply {
            setString(1, json)
        }.executeUpdate()
        return kvittering
    }

    override fun getById(id: UUID): Kvittering? {
        ds.connection.use {
            val existingYpList = ArrayList<Kvittering>()
            val res = it.prepareStatement(getByIdStatement).apply {
                setString(1, id.toString())
            }.executeQuery()

            while (res.next()) {
                existingYpList.add(extractKvittering(res))
            }

            return existingYpList.firstOrNull()
        }
    }


    override fun getByStatus(status: KvitteringStatus, limit: Int): List<Kvittering> {
        ds.connection.use { con ->
            val resultList = ArrayList<Kvittering>()
            val res = con.prepareStatement(getByStatuses).apply {
                setString(1, status.toString())
                setInt(2, limit)
            }.executeQuery()

            while (res.next()) {
                resultList.add(extractKvittering(res))
            }
            return resultList
        }
    }

    override fun update(kvittering: Kvittering) {
        val json = mapper.writeValueAsString(kvittering)
        ds.connection.use {
            it.prepareStatement(updateStatement).apply {
                setString(1, json)
                setString(2, kvittering.id.toString())
            }.executeUpdate()
        }
    }

    override fun delete(id: UUID): Int {
        ds.connection.use {
            return it.prepareStatement(deleteStatement).apply {
                setString(1, id.toString())
            }.executeUpdate()
        }
    }

    private fun extractKvittering(res: ResultSet): Kvittering {
        val kvittering = mapper.readValue<Kvittering>(res.getString("data"))
        return kvittering
    }
}