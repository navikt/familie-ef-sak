package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode

import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/manueltbrev"])
@ProtectedWithClaims(issuer = "azuread")
class ManueltBrevController(private val brevClient: BrevClient) {

    @PostMapping("")
    fun lagManueltBrev(@RequestBody brevInnhold: JsonNode): Ressurs<ByteArray> {
        return Ressurs.success(brevClient.lagManueltBrev(brevInnhold))
    }

}