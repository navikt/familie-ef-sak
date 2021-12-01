package no.nav.familie.ef.sak.arbeidsforhold

import no.nav.familie.ef.sak.arbeidsforhold.ekstern.ArbeidsforholdService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/arbeidsforhold")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ArbeidsforholdController(
    private val tilgangService: TilgangService,
    private val arbeidsforholdService: ArbeidsforholdService
) {

    @GetMapping("fagsak/{fagsakId}")
    fun hentArbeidsforhold(@PathVariable("fagsakId") fagsakId: UUID,
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                           @RequestParam ansettelsesperiodeFom: LocalDate
    ): Ressurs<List<ArbeidsforholdDto>> {
        tilgangService.validerTilgangTilFagsak(fagsakId)
        val arbeidsforhold = arbeidsforholdService.hentArbeidsforhold(fagsakId, ansettelsesperiodeFom)
        return Ressurs.success(arbeidsforhold)
    }
}
