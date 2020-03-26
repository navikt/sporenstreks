package no.nav.helse.sporenstreks.prosessering

import kotlinx.coroutines.CoroutineScope
import java.time.Duration

class ProcessRefusjonskravJob(coroutineScope: CoroutineScope) :
        RecurringJob(coroutineScope, Duration.ofMinutes(5)) {

    override fun doJob() {
        // hent ventende refusjonskrav fra databasen



    }
}