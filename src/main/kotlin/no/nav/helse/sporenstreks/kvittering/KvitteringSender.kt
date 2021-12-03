package no.nav.helse.sporenstreks.kvittering

interface KvitteringSender {
    fun send(kvittering: Kvittering)
}
