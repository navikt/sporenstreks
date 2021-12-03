package no.nav.helse.sporenstreks.kvittering

class DummyKvitteringSender : KvitteringSender {
    override fun send(kvittering: Kvittering) {
        println("Sender kvittering: ${kvittering.id}")
    }
}
