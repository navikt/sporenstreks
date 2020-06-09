package no.nav.helse.sporenstreks.integrasjon.rest.sensu

import java.io.OutputStreamWriter
import java.net.Socket
import java.nio.charset.StandardCharsets

interface SensuClient {

    fun write(data: SensuEvent)
}

class SensuClientImpl(
        private val hostname: String,
        private val port: Int
) : SensuClient {

    override fun write(data: SensuEvent) {
        Socket(hostname, port).use { socket ->
            OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8).use { streamWriter ->
                streamWriter.write(data.json)
                streamWriter.flush()
            }
        }
    }
}

class SensuEvent(sensuName: String, output: String) {
    val json = "{" +
            "\"name\":\"" + sensuName + "\"," +
            "\"type\":\"metric\"," +
            "\"handlers\":[\"events_nano\"]," +
            "\"output\":\"" + output + "\"," +
            "\"status\":0" +
            "}"

}
