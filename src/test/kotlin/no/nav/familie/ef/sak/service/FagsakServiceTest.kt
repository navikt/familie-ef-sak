package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.util.BrukerContextUtil
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.objectMapper
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
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
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

        val behandlingInaktiv = Behandling(fagsakId = fagsakDB.id,
                                           type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                           status = BehandlingStatus.FERDIGSTILT,
                                           aktiv = false,
                                           steg = StegType.BEHANDLING_FERDIGSTILT,
                                           resultat = BehandlingResultat.INNVILGET)
        val behandlingAktiv = Behandling(fagsakId = fagsakDB.id,
                                         type = BehandlingType.REVURDERING,
                                         status = BehandlingStatus.UTREDES,
                                         aktiv = true,
                                         steg = StegType.VILKÅR,
                                         resultat = BehandlingResultat.INNVILGET)

        behandlingRepository.insert(behandlingInaktiv)
        behandlingRepository.insert(behandlingAktiv)

        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.BARNETILSYN)
        println(objectMapper.writeValueAsString(fagsak))
        assertThat(fagsak.behandlinger.size).isEqualTo(2)
        assertThat(fagsak.stønadstype).isEqualTo(fagsakRequest.stønadstype)
        assertThat(fagsak.personIdent).isEqualTo(personIdent)
        assertThat(fagsak.behandlinger.find { it.aktiv }?.status).isEqualTo(behandlingAktiv.status)
        assertThat(fagsak.behandlinger.find { it.aktiv }?.type).isEqualTo(behandlingAktiv.type)

        assertThat(fagsak.behandlinger.find { !it.aktiv }?.status).isEqualTo(behandlingInaktiv.status)
        assertThat(fagsak.behandlinger.find { !it.aktiv }?.type).isEqualTo(behandlingInaktiv.type)
    }


}