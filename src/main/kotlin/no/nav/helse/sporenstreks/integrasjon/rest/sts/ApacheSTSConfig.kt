package no.nav.helse.sporenstreks.integrasjon.rest.sts

import org.apache.cxf.Bus
import org.apache.cxf.BusFactory
import org.apache.cxf.binding.soap.Soap12
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.endpoint.Client
import org.apache.cxf.frontend.ClientProxy
import org.apache.cxf.ws.policy.PolicyBuilder
import org.apache.cxf.ws.policy.PolicyEngine
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver
import org.apache.cxf.ws.security.SecurityConstants
import org.apache.neethi.Policy

val STS_CLIENT_AUTHENTICATION_POLICY = "classpath:sts-policies/untPolicy.xml"
val STS_SAML_POLICY = "classpath:sts-policies/requestSamlPolicy.xml"

fun wsStsClient(stsUrl: String, credentials: Pair<String, String>): org.apache.cxf.ws.security.trust.STSClient {
    val bus = BusFactory.getDefaultBus()
    return org.apache.cxf.ws.security.trust.STSClient(bus).apply {
        isEnableAppliesTo = false
        isAllowRenewing = false
        location = stsUrl
        properties = mapOf(
            SecurityConstants.USERNAME to credentials.first,
            SecurityConstants.PASSWORD to credentials.second
        )
        setPolicy(bus.resolvePolicy(STS_CLIENT_AUTHENTICATION_POLICY))
    }
}

private fun Bus.resolvePolicy(policyUri: String): Policy {
    val registry = getExtension(PolicyEngine::class.java).registry
    val resolved = registry.lookup(policyUri)

    val policyBuilder = getExtension(PolicyBuilder::class.java)
    val referenceResolver = RemoteReferenceResolver("", policyBuilder)

    return resolved ?: referenceResolver.resolveReference(policyUri)
}

fun org.apache.cxf.ws.security.trust.STSClient.configureFor(servicePort: Any) {
    configureFor(servicePort, STS_SAML_POLICY)
}

fun org.apache.cxf.ws.security.trust.STSClient.configureFor(servicePort: Any, policyUri: String) {
    val client = ClientProxy.getClient(servicePort)
    client.configureSTS(this, policyUri)
}

fun Client.configureSTS(stsClient: org.apache.cxf.ws.security.trust.STSClient, policyUri: String = STS_SAML_POLICY) {
    requestContext[SecurityConstants.STS_CLIENT] = stsClient
    requestContext[SecurityConstants.CACHE_ISSUED_TOKEN_IN_ENDPOINT] = true
    setClientEndpointPolicy(bus.resolvePolicy(policyUri))
}

private fun Client.setClientEndpointPolicy(policy: Policy) {
    val policyEngine: PolicyEngine = bus.getExtension(PolicyEngine::class.java)
    val message = SoapMessage(Soap12.getInstance())
    val endpointPolicy = policyEngine.getClientEndpointPolicy(endpoint.endpointInfo, null, message)
    policyEngine.setClientEndpointPolicy(endpoint.endpointInfo, endpointPolicy.updatePolicy(policy, message))
}
