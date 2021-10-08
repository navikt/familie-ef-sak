package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/frittstaende-brev"])
@ProtectedWithClaims(issuer = "azuread")
class FrittståendeBrevController(private val frittståendeBrevService: FrittståendeBrevService) {

    @PostMapping("")
    fun forhåndsvisFrittståendeBrev(@RequestBody brevInnhold: FrittståendeBrevDto): Ressurs<ByteArray> {
        return Ressurs.success(frittståendeBrevService.lagFrittståendeBrev(brevInnhold))
    }

    @PostMapping("/send")
    fun sendFrittståendeBrev(@RequestBody brevInnhold: FrittståendeBrevDto): Ressurs<Unit>{
        return Ressurs.success(frittståendeBrevService.sendFrittståendeBrev(brevInnhold))
    }

}