package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.simulering.DbMigreringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@Deprecated("Slettes når migrering er kjørt i produksjon")
internal class SimuleringBeriketDbMigreringTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var simuleringsresultatRepository: SimuleringsresultatRepository
    @Autowired private lateinit var dbMigreringService: DbMigreringService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    fun `skal legget til beriket simuleringsresultat i databasen`() {
        val personIdent = "12345678901"
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson(personIdent))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = true))

        val lagretSimuleringsresultat = simuleringsresultatRepository.insert(
                Simuleringsresultat(
                        behandlingId = behandling.id,
                        data = DetaljertSimuleringResultat(emptyList())
                )
        )

        Assertions.assertThat(lagretSimuleringsresultat.beriketData).isNull()

        dbMigreringService.dbMigrering()

        val migrertSimuleringsresultat = simuleringsresultatRepository.findById(behandling.id).get()

        Assertions.assertThat(migrertSimuleringsresultat.beriketData).isNotNull
    }

}