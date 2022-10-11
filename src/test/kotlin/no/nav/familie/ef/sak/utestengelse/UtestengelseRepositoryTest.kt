package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.defaultIdenter
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID
/*
internal class UtestengelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var utestengelseRepository: UtestengelseRepository

    private val fagsakPerson = fagsakPerson(defaultIdenter)
    private val utestengelse =
        Utestengelse(fagsakPersonId = fagsakPerson.id, periode = Månedsperiode(LocalDate.now(), LocalDate.now()))

    @BeforeEach
    internal fun setUp() {
        testoppsettService.opprettPerson(fagsakPerson)
    }

    @Test
    internal fun `skal kunne lagre å hente`() {
        utestengelseRepository.lagre(utestengelse)
        val utestengelser = utestengelseRepository.hentForFagsakPerson(fagsakPerson.id)
        assertThat(utestengelser).hasSize(1)
        val fraDb = utestengelser[0]
        assertThat(fraDb.id).isEqualTo(utestengelse.id)
        assertThat(fraDb.versjon).isEqualTo(utestengelse.versjon)
        assertThat(fraDb.aktiv).isEqualTo(utestengelse.aktiv)
        assertThat(fraDb.fagsakPersonId).isEqualTo(utestengelse.fagsakPersonId)
        assertThat(fraDb.periode).isEqualTo(utestengelse.periode)
        assertThat(fraDb.sporbar).isEqualTo(utestengelse.sporbar)
    }

    @Nested
    inner class markerSlettet {

        @Test
        internal fun `skal feile hvis utestengelse ikke finnes`() {
            assertThatThrownBy {
                utestengelseRepository.markerSlettet(UUID.randomUUID())
            }.hasMessage("asd")
        }

        @Test
        internal fun `skal markere aktiv utestengelse som slettet`() {
            utestengelseRepository.lagre(utestengelse.copy(aktiv = false))
            utestengelseRepository.lagre(utestengelse.copy(versjon = 2))
            utestengelseRepository.markerSlettet(utestengelse.id)
            namedParameterJdbcOperations.query("select * from utestengelse")
        }

    }

    @Nested
    inner class oppdater {

        @Test
        internal fun `skal feile hvis utestengelse ikke finnes`() {
            assertThatThrownBy {
                utestengelseRepository.oppdater(UUID.randomUUID(), Månedsperiode(LocalDate.now(), LocalDate.now()))
            }.hasMessage("asd")
        }
    }
}

 */