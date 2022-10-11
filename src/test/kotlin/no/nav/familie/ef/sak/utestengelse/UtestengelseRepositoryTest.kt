package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

internal class UtestengelseRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var utestengelseRepository: UtestengelseRepository

    private val fagsakPerson = fagsakPerson(setOf(PersonIdent("1")))
    private val fagsakPerson2 = fagsakPerson(setOf(PersonIdent("2")))

    private val periode = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 3))
    private val periode2 = Månedsperiode(YearMonth.of(2021, 1), YearMonth.of(2021, 3))
    private val utestengelse =
        Utestengelse(fagsakPersonId = fagsakPerson.id, fom = periode.fomDato, tom = periode.tomDato)
    private val utestengelse2 =
        Utestengelse(fagsakPersonId = fagsakPerson.id, fom = periode2.fomDato, tom = periode2.tomDato)

    @BeforeEach
    internal fun setUp() {
        testoppsettService.opprettPerson(fagsakPerson)
        testoppsettService.opprettPerson(fagsakPerson2)
    }

    @Test
    internal fun `skal kunne lagre å hente`() {
        utestengelseRepository.insert(utestengelse)
        val fraDb = utestengelseRepository.findByIdOrThrow(utestengelse.id)
        assertThat(fraDb.id).isEqualTo(utestengelse.id)
        assertThat(fraDb.versjon).isEqualTo(utestengelse.versjon)
        assertThat(fraDb.fagsakPersonId).isEqualTo(utestengelse.fagsakPersonId)
        assertThat(fraDb.fom).isEqualTo(utestengelse.fom)
        assertThat(fraDb.tom).isEqualTo(utestengelse.tom)
    }

    @Test
    internal fun `findAllByFagsakPersonId skal finne utestengelser for person`() {
        utestengelseRepository.insert(utestengelse)
        utestengelseRepository.insert(utestengelse2)

        assertThat(utestengelseRepository.findAllByFagsakPersonId(fagsakPerson2.id)).isEmpty()
        assertThat(utestengelseRepository.findAllByFagsakPersonId(fagsakPerson.id)).hasSize(2)
    }
}

