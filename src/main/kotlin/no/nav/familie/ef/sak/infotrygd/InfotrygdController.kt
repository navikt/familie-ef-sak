package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/infotrygd")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class InfotrygdController(private val tilgangService: TilgangService,
                          private val infotrygdService: InfotrygdService) {

    @PostMapping("perioder")
    fun hentPerioder(@RequestBody personIdent: PersonIdentDto): Ressurs<InfotrygdPerioderDto> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(infotrygdService.hentDtoPerioder(personIdent.personIdent))
    }

    @PostMapping("saker")
    fun hentSaker(@RequestBody personIdent: PersonIdentDto): Ressurs<InfotrygdSakResponse> {
        tilgangService.validerTilgangTilPersonMedBarn(personIdent.personIdent, AuditLoggerEvent.ACCESS)
        return Ressurs.success(infotrygdService.hentSaker(personIdent.personIdent))
    }

}