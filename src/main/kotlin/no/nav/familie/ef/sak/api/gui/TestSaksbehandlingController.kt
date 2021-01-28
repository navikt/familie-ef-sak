package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.PersonService
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.ef.søknad.NavnOgFnr
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
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
                                   private val behandlingshistorikkService: BehandlingshistorikkService,
                                   private val behandlingService: BehandlingService,
                                   private val personService: PersonService) {

    @PostMapping(path = ["fagsak"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFagsakForTestperson(@RequestBody testFagsakRequest: TestFagsakRequest): Ressurs<UUID> {
        val fagsakDto =
                fagsakService.hentEllerOpprettFagsak(testFagsakRequest.personIdent, Stønadstype.OVERGANGSSTØNAD)
        val fagsak = fagsakService.hentFagsak(fagsakDto.id)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        behandlingshistorikkService.opprettHistorikkInnslag(Behandlingshistorikk(behandlingId = behandling.id,
                                                                                 steg = StegType.VILKÅRSVURDERE_INNGANGSVILKÅR))
        val søkerMedBarn = personService.hentPersonMedRelasjoner(testFagsakRequest.personIdent)

        val barnNavnOgFnr = søkerMedBarn.barn.map { NavnOgFnr(it.value.navn.gjeldende().visningsnavn(), it.key) }

        val søknad: SøknadOvergangsstønad = TestsøknadBuilder.Builder()
                .setPersonalia(søkerMedBarn.søker.navn.gjeldende().visningsnavn(), søkerMedBarn.søkerIdent)
                .setBarn(barnNavnOgFnr)
                .build().søknadOvergangsstønad


        behandlingService.lagreSøknadForOvergangsstønad(søknad,
                                                        behandling.id,
                                                        fagsak.id,
                                                        "TESTJPID")
        return Ressurs.success(behandling.id)
    }
}

data class TestFagsakRequest(val personIdent: String)
