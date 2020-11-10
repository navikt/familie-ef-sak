package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api

import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping(path = ["/api/external/sak/"])
@Unprotected
@Validated
class TestSakController(private val behandlingService: BehandlingService,
                        private val stegService: StegService,
                        private val fagsakService: FagsakService,
                        private val oppgaveRepository: OppgaveRepository) {

    @PostMapping("dummy")
    fun dummy(): UUID {
        //TODO Dette steget må trigges et annet sted når vi har satt flyten for opprettelse av behandling.
        // Trigger den her midlertidig for å kunne utføre inngangsvilkår-steget
        val fagsakDto =
                fagsakService.hentEllerOpprettFagsak(Testsøknad.søknadOvergangsstønad.personalia.verdi.fødselsnummer.verdi.verdi,
                                                     Stønadstype.OVERGANGSSTØNAD)
        val fagsak = fagsakService.hentFagsak(fagsakDto.id)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        behandlingService.mottaSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "123")
        stegService.håndterRegistrerOpplysninger(behandling, "")
        val eksisterendeOppgave =
                oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(behandling.id, Oppgavetype.BehandleSak)
        if (eksisterendeOppgave == null) {
            oppgaveRepository.insert(Oppgave(behandlingId = behandling.id, gsakOppgaveId = 2L, type = Oppgavetype.BehandleSak))
        }
        return behandling.id
    }
}