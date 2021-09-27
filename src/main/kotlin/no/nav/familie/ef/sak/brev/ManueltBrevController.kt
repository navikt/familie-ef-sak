package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.ManueltBrevDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevFritekstDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/manueltbrev"])
@ProtectedWithClaims(issuer = "azuread")
class ManueltBrevController(private val manueltBrevService: ManueltBrevService) {

    @PostMapping("")
    fun lagManueltBrev(@RequestBody brevInnhold: ManueltBrevDto): Ressurs<ByteArray> {
        return Ressurs.success(manueltBrevService.lagManueltBrev(brevInnhold))
    }

}