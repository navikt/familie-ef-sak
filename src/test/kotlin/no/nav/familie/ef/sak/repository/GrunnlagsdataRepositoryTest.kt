package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Grunnlagsdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class GrunnlagsdataRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var grunnlagsdataRepository: GrunnlagsdataRepository

    @Test
    internal fun `hente data går OK`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val grunnlagsdata = opprettGrunnlagsdata()
        grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdata))

        assertThat(grunnlagsdataRepository.findByIdOrThrow(behandling.id).data).isEqualTo(grunnlagsdata)
    }

    @Test
    internal fun `bakåtkompatibilitet - tidligereVedtaksPerioder er null då den ikke finnes med i tidligere objekter`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val grunnlagsdata = opprettGrunnlagsdata().copy(tidligereVedtaksperioder = null)
        grunnlagsdataRepository.insert(Grunnlagsdata(behandling.id, grunnlagsdata))

        assertThat(grunnlagsdataRepository.findByIdOrThrow(behandling.id).data).isEqualTo(grunnlagsdata)
    }
}
