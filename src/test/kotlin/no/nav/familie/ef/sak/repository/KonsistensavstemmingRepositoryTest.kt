package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.KonsistensavstemmingRepository
import no.nav.familie.ef.sak.repository.domain.Konsistensavstemming
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class KonsistensavstemmingRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var konsistensavstemmingRepository: KonsistensavstemmingRepository

    @Test
    internal fun `Gitt att datumet for konsistensavstemming er dagens dato skal finnKonsistensavstemmingMedDatoIdag returnere konsistensavstemmingOppdrag`() {
        val konsistensavstemmingOppdrag =
                konsistensavstemmingRepository.insert(Konsistensavstemming(
                        dato = LocalDate.now(),
                        stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val konsistensavstemmingOppdragFraDb =
                konsistensavstemmingRepository.finnKonsistensavstemmingMedDatoIdag(stønadstype = Stønadstype.OVERGANGSSTØNAD)
        assertThat(konsistensavstemmingOppdrag).isEqualTo(konsistensavstemmingOppdragFraDb)
    }

    @Test
    internal fun `Gitt att datumet for konsistensavstemming er ikke dagens dato skal finnKonsistensavstemmingMedDatoIdag returnere null`() {
        konsistensavstemmingRepository.insert(Konsistensavstemming(
                dato = LocalDate.now().plusDays(5),
                stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val konsistensavstemmingOppdrag =
                konsistensavstemmingRepository.finnKonsistensavstemmingMedDatoIdag(stønadstype = Stønadstype.OVERGANGSSTØNAD)
        assertThat(konsistensavstemmingOppdrag).isNull()
    }
}