package no.nav.familie.ef.sak.kontantstøtte

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.YearMonth

class KontantstøtteServiceTest {
    private val kontantstøtteClientMock = mockk<KontantstøtteClient>()
    private val personServiceMock = mockk<PersonService>()
    private val kontantstøtteService: KontantstøtteService =
        KontantstøtteService(kontantstøtteClient = kontantstøtteClientMock, personService = personServiceMock)

    private val morIdent = "12345678901"
    private val morGammelIdent1 = "12345678904"
    private val morGammelIdent2 = "12345678902"
    private val identUtenKs = "98765432101"

    private val morsIdenter = listOf(morIdent, morGammelIdent1, morGammelIdent2)
    private val pdlIdenterMor = morsIdenter.mapIndexed { index, ident -> PdlIdent(ident, index == 0) }
    private val pdlIdenterUtenKs = listOf(PdlIdent(identUtenKs, false))

    @BeforeEach
    fun setUp() {
        every { personServiceMock.hentPersonIdenter(morIdent) } returns PdlIdenter(identer = pdlIdenterMor)
        every { personServiceMock.hentPersonIdenter(identUtenKs) } returns PdlIdenter(identer = pdlIdenterUtenKs)

        val ksSakPeriode = KsSakPeriode(YearMonth.now(), YearMonth.now(), KsBarn(1, "1"))
        val kontantstøtteMedPerioder = HentUtbetalingsinfoKontantstøtte(emptyList(), listOf(ksSakPeriode))
        val kontantstøtteUtenPerioder = HentUtbetalingsinfoKontantstøtte(emptyList(), emptyList())

        every { kontantstøtteClientMock.hentUtbetalingsinfo(morsIdenter) } returns kontantstøtteMedPerioder
        every { kontantstøtteClientMock.hentUtbetalingsinfo(listOf(identUtenKs)) } returns kontantstøtteUtenPerioder
    }

    @Test
    fun `skal sjekke kontantstøttevedtak for person med kontantstøtte`() {
        val finnesKontantstøtte = kontantstøtteService.hentUtbetalingsinfoKontantstøtte(morIdent)
        Assertions.assertThat(finnesKontantstøtte.finnesUtbetaling).isTrue()
        verify(exactly = 1) { kontantstøtteClientMock.hentUtbetalingsinfo(morsIdenter) }
    }

    @Test
    fun `skal sjekke kontantstøttevedtak for person uten kontantstøtte`() {
        val finnesKontantstøtte = kontantstøtteService.hentUtbetalingsinfoKontantstøtte(identUtenKs)
        Assertions.assertThat(finnesKontantstøtte.finnesUtbetaling).isFalse()
        verify(exactly = 1) { kontantstøtteClientMock.hentUtbetalingsinfo(listOf(identUtenKs)) }
    }
}
