package no.nav.familie.ef.sak.felles.util

import no.nav.security.mock.oauth2.MockOAuth2Server
import java.util.UUID

object TokenUtil {

    fun clientToken(mockOAuth2Server: MockOAuth2Server, clientId: String, accessAsApplication: Boolean): String {
        val thisId = UUID.randomUUID().toString()

        val claims = mapOf(
            "oid" to thisId,
            "azp" to clientId,
            "roles" to if (accessAsApplication) listOf("access_as_application") else emptyList()
        )

        return mockOAuth2Server.issueToken(
            issuerId = "azuread",
            subject = thisId,
            audience = "aud-localhost",
            claims = claims
        ).serialize()

    }

    fun onBehalfOfToken(mockOAuth2Server: MockOAuth2Server, role: String, saksbehandler: String): String {
        val thisId = UUID.randomUUID().toString()
        val clientId = UUID.randomUUID().toString()

        val claims = mapOf(
            "oid" to thisId,
            "azp" to clientId,
            "name" to saksbehandler,
            "NAVident" to saksbehandler,
            "groups" to listOf(role)
        )

        return mockOAuth2Server.issueToken(
            issuerId = "azuread",
            subject = thisId,
            audience = "aud-localhost",
            claims = claims
        ).serialize()
    }

}
