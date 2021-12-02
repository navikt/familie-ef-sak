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
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

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

    @Test
    internal fun `skal oppdatere personident på fagsak dersom personen har fått ny ident som er tidligere registrert`() {

        val iGår = Sporbar(opprettetAv = "XY",
                           opprettetTid = LocalDateTime.now().minusDays(1),
                           endret = Endret(endretAv = "XY", endretTid = LocalDateTime.now().minusDays(1)))
        val iDag = Sporbar(opprettetAv = "XY",
                           opprettetTid = LocalDateTime.now().minusHours(1),
                           endret = Endret(endretAv = "XY", endretTid = LocalDateTime.now().minusHours(1)))

        val gjeldendeIdent = "12345678901"
        val feilRegistrertIdent = "99988877712"
        val fagsakMedFeilregistrertIdent = fagsakRepository.insert(Fagsak(eksternId = EksternFagsakId(id = 1234),
                                                                         stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                                         søkerIdenter = setOf(FagsakPerson(ident = gjeldendeIdent,
                                                                                                           sporbar = iGår),
                                                                                              FagsakPerson(ident = feilRegistrertIdent,
                                                                                                           sporbar = iDag))))

        assertThat(fagsakMedFeilregistrertIdent.hentAktivIdent()).isEqualTo(feilRegistrertIdent)

        val oppdatertFagsak = fagsakService.hentEllerOpprettFagsak(gjeldendeIdent, Stønadstype.OVERGANGSSTØNAD)
        assertThat(oppdatertFagsak.søkerIdenter.map { it.ident }).contains(feilRegistrertIdent)
        assertThat(oppdatertFagsak.hentAktivIdent()).isEqualTo(gjeldendeIdent)
        val fagsakEtterOppdatering = fagsakService.hentFagsak(fagsakMedFeilregistrertIdent.id).hentAktivIdent()
        assertThat(fagsakEtterOppdatering).isEqualTo(gjeldendeIdent)
    }

    @Test
    internal fun `skal oppdatere personident hvis fagsak har gammel ident`() {

        val iGår = Sporbar(opprettetAv = "XY",
                           opprettetTid = LocalDateTime.now().minusDays(1),
                           endret = Endret(endretAv = "XY", endretTid = LocalDateTime.now().minusDays(1)))

        val gjeldendeIdent = "12345678901"
        val historiskIdent = "98765432109"
        val fagsakMedHistoriskIdent = fagsakRepository.insert(Fagsak(eksternId = EksternFagsakId(id = 1234),
                                                                     stønadstype = Stønadstype.OVERGANGSSTØNAD,
                                                                     søkerIdenter = setOf(FagsakPerson(ident = historiskIdent,
                                                                                                           sporbar = iGår))))

        assertThat(fagsakMedHistoriskIdent.hentAktivIdent()).isEqualTo(historiskIdent)

        val oppdatertFagsak = fagsakService.hentEllerOpprettFagsak(gjeldendeIdent, Stønadstype.OVERGANGSSTØNAD)
        assertThat(oppdatertFagsak.søkerIdenter.map { it.ident }).contains(historiskIdent)
        assertThat(oppdatertFagsak.hentAktivIdent()).isEqualTo(gjeldendeIdent)

        val fagsakEtterOppdatering = fagsakService.hentFagsak(fagsakMedHistoriskIdent.id)
        assertThat(fagsakEtterOppdatering.hentAktivIdent()).isEqualTo(gjeldendeIdent)
    }
}