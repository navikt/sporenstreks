package no.nav.helse.sporenstreks.kvittering

import no.altinn.services.serviceengine.correspondence._2009._10.ICorrespondenceAgencyExternalBasic
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean

object Clients {

    fun iCorrespondenceExternalBasic(serviceUrl: String): ICorrespondenceAgencyExternalBasic =
        createServicePort(
            serviceUrl = serviceUrl,
            serviceClazz = ICorrespondenceAgencyExternalBasic::class.java
        )

    private fun <PORT_TYPE> createServicePort(
        serviceUrl: String,
        serviceClazz: Class<PORT_TYPE>
    ): PORT_TYPE = JaxWsProxyFactoryBean().apply {
        address = serviceUrl
        serviceClass = serviceClazz
    }.create(serviceClazz)
}
