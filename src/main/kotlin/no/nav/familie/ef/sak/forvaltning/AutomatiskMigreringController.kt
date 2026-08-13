package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandling.migrering.AutomatiskMigreringService
import no.nav.familie.ef.sak.behandling.migrering.MigreringExceptionType
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.HarRolleForvalter
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/automatisk-migrering")
class AutomatiskMigreringController(
    private val automatiskMigreringService: AutomatiskMigreringService,
    private val tilgangService: TilgangService,
) {
    @HarRolleForvalter
    @GetMapping
    fun migrerAutomatiskt(
        @RequestParam antall: Int,
    ) {
        brukerfeilHvis(antall > 100) {
            "Kan ikke migrere fler enn 100"
        }
        automatiskMigreringService.migrerAutomatisk(antall)
    }

    @HarRolleForvalter
    @PostMapping("rekjoer")
    fun rekjoer(
        @RequestBody personIdent: PersonIdent,
    ) {
        automatiskMigreringService.rekjør(personIdent.ident)
    }

    @HarRolleForvalter
    @PostMapping("rekjoer/{arsak}")
    fun rekjoer(
        @PathVariable("arsak") årsak: MigreringExceptionType,
    ) {
        automatiskMigreringService.rekjør(årsak)
    }
}
