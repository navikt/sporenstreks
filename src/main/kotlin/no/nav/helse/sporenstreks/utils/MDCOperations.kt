package no.nav.helse.sporenstreks.utils

import org.slf4j.LoggerFactory
import java.security.SecureRandom

/**
 * Utility-klasse for kommunikasjon med MDC.
 */
object MDCOperations {


    private val RANDOM = SecureRandom()
    private val log = LoggerFactory.getLogger(MDCOperations::class.java)

    fun generateCallId(): String {
        val randomNr = randomNumber
        val systemTime = systemTime
        return "CallId_" + systemTime + "_" + randomNr
    }

    private val randomNumber: Int
        get() = RANDOM.nextInt(Int.MAX_VALUE)

    private val systemTime: Long
        get() = System.currentTimeMillis()

}

