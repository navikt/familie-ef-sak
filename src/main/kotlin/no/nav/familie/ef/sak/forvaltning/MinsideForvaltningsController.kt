package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.minside.MinSideKafkaProducerService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/minside/forvaltning/")
@ProtectedWithClaims(issuer = "azuread")
class MinsideForvaltningsController(
    private val featureToggleService: FeatureToggleService,
    private val minSideKafkaProducerService: MinSideKafkaProducerService,
) {
    @PostMapping("aktiver")
    fun aktiverPersonForMinSide(@RequestBody personIdentDto: PersonIdentDto) {
        feilHvisIkke(erUtviklerMedVeilderrolle()) { "Kan kun kjøres av utvikler med veilederrolle" }
        validerPersonIdent(personIdentDto)
        minSideKafkaProducerService.aktiver(personIdent = personIdentDto.personIdent)
    }

    @PostMapping("deaktiver")
    fun deaktiverPersonForMinSide(@RequestBody personIdentDto: PersonIdentDto) {
        feilHvisIkke(erUtviklerMedVeilderrolle()) { "Kan kun kjøres av utvikler med veilederrolle" }
        validerPersonIdent(personIdentDto)
        minSideKafkaProducerService.deaktiver(personIdent = personIdentDto.personIdent)
    }

    private fun validerPersonIdent(personIdentDto: PersonIdentDto) {
        if (personIdentDto.personIdent.length != 11) {
            error("PersonIdent må ha 11 siffer")
        }
    }

    private fun erUtviklerMedVeilderrolle(): Boolean =
        SikkerhetContext.erSaksbehandler() && featureToggleService.isEnabled(Toggle.UTVIKLER_MED_VEILEDERRROLLE)
}
