package no.nav.familie.ef.sak.andreytelser

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/andre-ytelser"])
@ProtectedWithClaims(issuer = "azuread")
class AndreYtelserController(
    private val andreYtelserService: AndreYtelserService,
    private val fagsakPersonService: FagsakPersonService,
    private val tilgangService: TilgangService,
) {
    @GetMapping("/arbeidsavklaringspenger/{fagsakPersonId}")
    fun hentArbeidsavklaringspengerForPerson(
        @PathVariable fagsakPersonId: UUID,
    ): Ressurs<AndreYtelserDto> {
        tilgangService.validerTilgangTilFagsakPerson(fagsakPersonId, AuditLoggerEvent.ACCESS)
        val personIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent, AuditLoggerEvent.ACCESS)

        return Ressurs.success(andreYtelserService.hentAndreYtelser(fagsakPersonId))
    }
}
