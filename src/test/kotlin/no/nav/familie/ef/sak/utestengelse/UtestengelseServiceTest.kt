package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth
import java.util.UUID

internal class UtestengelseServiceTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var utestengelseRepository: UtestengelseRepository

    @Autowired
    lateinit var utestengelseService: UtestengelseService

    private val fagsakPerson = fagsakPerson()

    private val periode = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 3))
    private val periode2 = Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3))
    private val utestengelse =
        Utestengelse(fagsakPersonId = fagsakPerson.id, fom = periode.fomDato, tom = periode.tomDato)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.opprettPerson(fagsakPerson)
    }

    @Nested
    inner class OpprettUtestengelse {
        @Test
        internal fun `skal kunne opprette utestengelse når det finnes en fra før`() {
            utestengelseRepository.insert(utestengelse)

            utestengelseService.opprettUtestengelse(OpprettUtestengelseDto(fagsakPerson.id, periode2))

            val utestengelser = utestengelseService.hentUtestengelser(fagsakPerson.id)
            assertThat(utestengelser).hasSize(2)
            assertThat(utestengelser[0].fom).isEqualTo(periode2.fomDato)
            assertThat(utestengelser[0].tom).isEqualTo(periode2.tomDato)
            assertThat(utestengelser[1].fom).isEqualTo(periode.fomDato)
            assertThat(utestengelser[1].tom).isEqualTo(periode.tomDato)
        }

        @Test
        internal fun `skal kunne opprette utestengelse når det finnes en overlappende slettet utestengelse`() {
            utestengelseRepository.insert(utestengelse.copy(slettet = true))

            utestengelseService.opprettUtestengelse(OpprettUtestengelseDto(fagsakPerson.id, periode))

            val utestengelser = utestengelseService.hentUtestengelser(fagsakPerson.id)
            assertThat(utestengelser).hasSize(1)
            assertThat(utestengelser[0].fom).isEqualTo(periode.fomDato)
            assertThat(utestengelser[0].tom).isEqualTo(periode.tomDato)
        }

        @Test
        internal fun `validerer at utestengelse ikke overlapper tidligere utestengelser`() {
            utestengelseRepository.insert(utestengelse)

            assertThatThrownBy {
                utestengelseService.opprettUtestengelse(OpprettUtestengelseDto(fagsakPerson.id, periode))
            }.hasMessageContaining("Ny utestengelse overlapper med en eksisterende utestengelse")
        }
    }

    @Nested
    inner class SlettUtestengelse {
        @Test
        internal fun `markerer utestengelse som slettet`() {
            clearBrukerContext()
            val utestengelseSomSlettes =
                Utestengelse(fagsakPersonId = fagsakPerson.id, fom = periode.fomDato, tom = periode.tomDato)

            utestengelseRepository.insert(utestengelseSomSlettes)

            testWithBrukerContext("saksbehandler") {
                utestengelseService.slettUtestengelse(utestengelseSomSlettes.fagsakPersonId, utestengelseSomSlettes.id)
            }

            val oppdatertUtestengelse = utestengelseRepository.findByIdOrThrow(utestengelseSomSlettes.id)
            assertThat(oppdatertUtestengelse.slettet).isTrue
            assertThat(oppdatertUtestengelse.sporbar.opprettetAv).isEqualTo(SYSTEM_FORKORTELSE)
            assertThat(oppdatertUtestengelse.sporbar.endret.endretAv).isEqualTo("saksbehandler")
        }

        @Test
        internal fun `feiler hvis utestengelse allerede er slettet`() {
            utestengelseRepository.insert(utestengelse.copy(slettet = true))
            assertThatThrownBy { utestengelseService.slettUtestengelse(utestengelse.fagsakPersonId, utestengelse.id) }
                .hasMessage("Utestengelse er allerede slettet")
        }

        @Test
        internal fun `feiler hvis id på fagsakPerson ikke er lik`() {
            utestengelseRepository.insert(utestengelse)
            assertThatThrownBy { utestengelseService.slettUtestengelse(UUID.randomUUID(), utestengelse.id) }
                .hasMessageContaining("er ikke lik utestengelse sin fagsakPersonId")
        }
    }
}
