package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Unprotected // kommer kunne brukes uten token
@RestController
@RequestMapping("api/automatisk-migrering")
class AutomatiskMigreringController(
    private val automatiskMigreringService: AutomatiskMigreringService,
    private val featureToggleService: FeatureToggleService
) {

    @GetMapping
    fun migrerAutomatiskt(@RequestParam antall: Int) {
        brukerfeilHvis(antall > 100) {
            "Kan ikke migrere fler enn 100"
        }
        brukerfeilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.automatisk-migrering")) {
            "Feature toggle for migrering er slått av"
        }
        automatiskMigreringService.migrerAutomatisk(antall)
    }

    @PostMapping("rekjoer")
    fun rekjoer(@RequestBody personIdent: PersonIdent) {
        automatiskMigreringService.rekjør(personIdent.ident)
    }

    @PostMapping("rekjoer/{arsak}")
    fun rekjoer(@PathVariable("arsak") årsak: MigreringExceptionType) {
        automatiskMigreringService.rekjør(årsak)
    }
}
