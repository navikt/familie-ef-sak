package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrunnlagsdataRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Test
    internal fun `hente data går OK`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val grunnlagsdata = opprettGrunnlagsdata()
        grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdata))

        assertThat(grunnlagsdataRepository.findByIdOrThrow(behandling.id).data).isEqualTo(grunnlagsdata)
    }

    @Test
    internal fun `finnBehandlingerSomManglerGrunnlagsdata skal finne behandlinger som har status OPPRETTET`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                status = BehandlingStatus.OPPRETTET,
                                                                type = BehandlingType.BLANKETT))
        val behandling2 = behandlingRepository.insert(behandling(fagsak,
                                                                 status = BehandlingStatus.UTREDES,
                                                                 type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val behandling3 = behandlingRepository.insert(behandling(fagsak,
                                                                 status = BehandlingStatus.FERDIGSTILT,
                                                                 type = BehandlingType.BLANKETT))

        val behandlinger = grunnlagsdataRepository.finnBehandlingerSomManglerGrunnlagsdata()
        assertThat(behandlinger.map { it.first }).containsExactlyInAnyOrder(behandling.id, behandling2.id, behandling3.id)
        assertThat(behandlinger.find { it.first == behandling.id }!!.second.let { BehandlingStatus.valueOf(it) })
                .isEqualTo(BehandlingStatus.OPPRETTET)
    }

    @Test
    internal fun `bakåtkompatibilitet - tidligereVedtaksPerioder er null då den ikke finnes med i tidligere objekter`() {
        val fagsak = fagsakRepository.insert(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val grunnlagsdata = opprettGrunnlagsdata().copy(tidligereVedtaksperioder = null)
        grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdata))

        assertThat(grunnlagsdataRepository.findByIdOrThrow(behandling.id).data).isEqualTo(grunnlagsdata)
    }

}
