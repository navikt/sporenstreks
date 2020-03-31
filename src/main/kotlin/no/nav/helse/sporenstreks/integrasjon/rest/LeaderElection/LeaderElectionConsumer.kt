package no.nav.helse.sporenstreks.integrasjon.rest.LeaderElection

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.url
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.net.UnknownHostException


interface LeaderElectionConsumer {
    suspend fun isLeader(): Boolean
}

class MockLeaderElectionConsumer : LeaderElectionConsumer {
    override suspend fun isLeader(): Boolean {
        return true
    }
}

class LeaderElectionConsumerImpl(
        val baseUrl: String,
        val httpClient: HttpClient) : LeaderElectionConsumer {

    override suspend fun isLeader(): Boolean {
        return try {
            val leader = getHostInfo() == getLeader()
            leader
        } catch (e: Exception) {
            log.error("failed to get leader", e)
            false
        }
    }

    private suspend fun getLeader(): HostInfo? {
        return httpClient.get<HostInfo> {
            url("http://$baseUrl")
        }
    }

    fun getHostInfo(): HostInfo? {
        return try {
            HostInfo(InetAddress.getLocalHost().hostName)
        } catch (e: UnknownHostException) {
            log.error("failed to get hostname for leader election", e)
            HostInfo("")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(LeaderElectionConsumerImpl::class.java)
    }
}


data class HostInfo(
        val name: String
)