package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.GrunnlagsdataRepository
import no.nav.familie.ef.sak.repository.domain.Grunnlagsdata
import no.nav.familie.ef.sak.repository.domain.GrunnlagsdataData
import no.nav.familie.ef.sak.repository.domain.JsonWrapper
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

internal class GrunnlagsdataRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var grunnlagsdataRepository: GrunnlagsdataRepository
    @Autowired private lateinit var medlemskapMapper: MedlemskapMapper
    @Autowired private lateinit var pdlClient: PdlClient

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        fagsakRepository.insert(fagsak)
        val behandling = behandlingRepository.insert(behandling)

        val data = opprettData()
        grunnlagsdataRepository.insert(Grunnlagsdata(behandlingId = behandling.id,
                                                     data = JsonWrapper(objectMapper.writeValueAsString(data))))
    }

    @Test
    internal fun `hene data på behandlingId`() {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrNull(behandling.id)
        assertThat(grunnlagsdata).isNotNull
    }

    @Test
    internal fun `oppdatering av grunnlagsdata skal oppdatere versjon`() {
        val grunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        assertThat(grunnlagsdata.versjon).isEqualTo(1)

        grunnlagsdataRepository.update(grunnlagsdata)
        val oppdatertGrunnlagsdata = grunnlagsdataRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertGrunnlagsdata.versjon).isEqualTo(2)
    }

    private fun opprettData(): GrunnlagsdataData {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)
        val pdlSøker = pdlClient.hentSøker("")

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 medlUnntak = Medlemskapsinfo("", emptyList(), emptyList(), emptyList()),
                                                 pdlSøker = pdlSøker)

        val sivilstand = SivilstandMapper.tilDto(sivilstandsdetaljer = søknad.sivilstand,
                                                 pdlSøker = pdlSøker)
        return GrunnlagsdataData(medlemskap.registergrunnlag, sivilstand.registergrunnlag)
    }
}