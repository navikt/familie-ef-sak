package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var fagsakService: FagsakService
    @Autowired lateinit var fagsakRepository: FagsakRepository
    @Autowired lateinit var behandlingRepository: BehandlingRepository

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal hente fagsak med tilhørende behandlinger som ikke finnes fra før`() {
        val personIdent = "23118612345"
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(personIdent, Stønadstype.OVERGANGSSTØNAD)
        assertThat(fagsak.behandlinger.size).isEqualTo(0)
        assertThat(fagsak.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
        assertThat(fagsak.personIdent).isEqualTo(personIdent)
    }

    @Test
    internal fun `skal hente fagsak med tilhørende behandlinger som finnes fra før`() {
        val personIdent = "23118612345"

        val fagsakRequest = Fagsak(stønadstype = Stønadstype.BARNETILSYN,
                                   søkerIdenter = setOf(FagsakPerson(ident = personIdent)))
        val fagsakDB = fagsakRepository.insert(fagsakRequest)

        val behandling1 = Behandling(fagsakId = fagsakDB.id,
                                     type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                     status = BehandlingStatus.FERDIGSTILT,
                                     steg = StegType.BEHANDLING_FERDIGSTILT,
                                     resultat = BehandlingResultat.INNVILGET,
                                     årsak = BehandlingÅrsak.SØKNAD)
        val behandling2 = Behandling(fagsakId = fagsakDB.id,
                                     type = BehandlingType.REVURDERING,
                                     status = BehandlingStatus.UTREDES,
                                     steg = StegType.VILKÅR,
                                     resultat = BehandlingResultat.INNVILGET,
                                     årsak = BehandlingÅrsak.SØKNAD)

        behandlingRepository.insert(behandling1)
        behandlingRepository.insert(behandling2)

        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(personIdent, Stønadstype.BARNETILSYN)
        assertThat(fagsak.behandlinger.size).isEqualTo(2)
        assertThat(fagsak.stønadstype).isEqualTo(fagsakRequest.stønadstype)
        assertThat(fagsak.personIdent).isEqualTo(personIdent)

        val førstegangsbehandling = fagsak.behandlinger.single { it.type == BehandlingType.FØRSTEGANGSBEHANDLING }
        assertThat(førstegangsbehandling.status).isEqualTo(behandling1.status)
        assertThat(førstegangsbehandling.type).isEqualTo(behandling1.type)

        val revurdering = fagsak.behandlinger.single { it.type == BehandlingType.REVURDERING }
        assertThat(revurdering.status).isEqualTo(behandling2.status)
        assertThat(revurdering.type).isEqualTo(behandling2.type)
    }

}