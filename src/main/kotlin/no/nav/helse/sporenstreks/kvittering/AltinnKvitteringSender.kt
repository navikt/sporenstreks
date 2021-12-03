package no.nav.helse.sporenstreks.kvittering

import no.altinn.schemas.services.intermediary.receipt._2009._10.ReceiptStatusEnum
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage
import no.nav.helse.sporenstreks.db.KvitteringRepository
import org.slf4j.LoggerFactory

class AltinnKvitteringSender(
    private val altinnKvitteringMapper: AltinnKvitteringMapper,
    private val iCorrespondenceAgencyExternalBasic: ICorrespondenceAgencyExternalBasic,
    private val username: String,
    private val password: String,
    private val db: KvitteringRepository
) : KvitteringSender {

    private val log = LoggerFactory.getLogger("AltinnKvitteringSender")

    companion object {
        const val SYSTEM_USER_CODE = "NAV_HELSEARBEIDSGIVER"
    }

    override fun send(kvittering: Kvittering) {
        try {
            val receiptExternal = iCorrespondenceAgencyExternalBasic.insertCorrespondenceBasicV2(
                username, password,
                SYSTEM_USER_CODE, kvittering.id.toString(),
                altinnKvitteringMapper.mapKvitteringTilInsertCorrespondence(kvittering)
            )
            if (receiptExternal.receiptStatusCode != ReceiptStatusEnum.OK) {
                log.error("Fikk uventet statuskode fra Altinn {}", receiptExternal.receiptStatusCode)
                throw RuntimeException("Feil ved sending kvittering til Altinn")
            } else {
                kvittering.status = KvitteringStatus.SENDT
            }
        } catch (e: ICorrespondenceAgencyExternalBasicInsertCorrespondenceBasicV2AltinnFaultFaultFaultMessage) {
            log.error("Feil ved sending kvittering til Altinn", e)
            log.error("${e.faultInfo} ${e.cause} ${e.message}")
            log.error("Feil ved sending kvittering til Altinn", e)
            throw RuntimeException("Feil ved sending kvittering til Altinn", e)
        } catch (e: Exception) {
            log.error("Feil ved sending kvittering til Altinn", e)
            throw e
        } finally {
            db.update(kvittering)
        }
    }
}
