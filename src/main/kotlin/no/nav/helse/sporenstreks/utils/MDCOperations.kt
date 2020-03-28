package no.nav.helse.sporenstreks.utils

import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.security.SecureRandom

/**
 * Utility-klasse for kommunikasjon med MDC.
 */
object MDCOperations {

    const val MDC_CALL_ID = "callId"
    const val MDC_USER_ID = "userId"
    const val MDC_CONSUMER_ID = "consumerId"

    private val RANDOM = SecureRandom()
    private val log = LoggerFactory.getLogger(MDCOperations::class.java)

    fun generateCallId(): String {
        val randomNr = randomNumber
        val systemTime = systemTime
        return "CallId_" + systemTime + "_" + randomNr
    }

    fun getFromMDC(key: String): String {
        val value = MDC.get(key)
        return value
    }

    fun putToMDC(key: String, value: String) {
        MDC.put(key, value)
    }

    fun remove(key: String) {
        MDC.remove(key)
    }

    private val randomNumber: Int
        private get() = RANDOM.nextInt(Int.MAX_VALUE)

    private val systemTime: Long
        private get() = System.currentTimeMillis()

}

