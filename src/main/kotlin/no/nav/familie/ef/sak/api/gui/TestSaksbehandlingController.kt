package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/test")
@ProtectedWithClaims(issuer = "azuread")
@Profile("!prod")
class TestSaksbehandlingController(private val fagsakService: FagsakService,
                                   private val behandlingService: BehandlingService,
                                   private val personService: PersonService) {

    @PostMapping(path = ["fagsak"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFagsakForTestperson(@RequestBody testFagsakRequest: TestFagsakRequest): Ressurs<UUID> {
        val fagsakDto =
                fagsakService.hentEllerOpprettFagsak(testFagsakRequest.personIdent, Stønadstype.OVERGANGSSTØNAD)
        val fagsak = fagsakService.hentFagsak(fagsakDto.id)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        val søkerMedBarn = personService.hentPersonMedRelasjoner(testFagsakRequest.personIdent)

        val barnNavnOgFnr = søkerMedBarn.barn.map { Testsøknad.NavnOgFnr(it.value.navn.gjeldende().visningsnavn(), it.key) }
        val søknad = Testsøknad.søknadOvergangsstønadMedSøker(søker = Testsøknad.NavnOgFnr(søkerMedBarn.søker.navn.gjeldende()
                                                                                                   .visningsnavn(),
                                                                                           søkerMedBarn.søkerIdent),
                                                              barneliste = barnNavnOgFnr)
        val journalpostId = "TESTJPID"
        behandlingService.lagreSøknadForOvergangsstønad(søknad,
                                                        behandling.id,
                                                        fagsak.id,
                                                        journalpostId)
        return Ressurs.success(behandling.id)
    }
}

data class TestFagsakRequest(val personIdent: String)
