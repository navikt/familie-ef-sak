package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.mapper.MedlemskapMapper
import no.nav.familie.ef.sak.mapper.SivilstandMapper
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.RegistergrunnlagRepository
import no.nav.familie.ef.sak.repository.domain.Registergrunnlag
import no.nav.familie.ef.sak.repository.domain.RegistergrunnlagData
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

internal class RegistergrunnlagRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var registergrunnlagRepository: RegistergrunnlagRepository
    @Autowired private lateinit var medlemskapMapper: MedlemskapMapper
    @Autowired private lateinit var pdlClient: PdlClient

    private val fagsak = fagsak()
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        fagsakRepository.insert(fagsak)
        val behandling = behandlingRepository.insert(behandling)

        val data = opprettData()
        registergrunnlagRepository.insert(Registergrunnlag(behandlingId = behandling.id,
                                                           data = data))
    }

    @Test
    internal fun `hene data på behandlingId`() {
        val grunnlagsdata = registergrunnlagRepository.findByIdOrNull(behandling.id)
        assertThat(grunnlagsdata).isNotNull
    }

    @Test
    internal fun `oppdatering av grunnlagsdata skal oppdatere versjon`() {
        val grunnlagsdata = registergrunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(grunnlagsdata.versjon).isEqualTo(1)

        registergrunnlagRepository.update(grunnlagsdata)
        val oppdatertGrunnlagsdata = registergrunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertGrunnlagsdata.versjon).isEqualTo(2)
    }

    private fun opprettData(): RegistergrunnlagData {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)
        val pdlSøker = pdlClient.hentSøker("")

        val medlemskap = medlemskapMapper.tilDto(medlemskapsdetaljer = søknad.medlemskap,
                                                 medlUnntak = Medlemskapsinfo("", emptyList(), emptyList(), emptyList()),
                                                 pdlSøker = pdlSøker)

        val sivilstand = SivilstandMapper.tilDto(sivilstandsdetaljer = søknad.sivilstand,
                                                 pdlSøker = pdlSøker)
        return RegistergrunnlagData(medlemskap.registergrunnlag, sivilstand.registergrunnlag)
    }
}