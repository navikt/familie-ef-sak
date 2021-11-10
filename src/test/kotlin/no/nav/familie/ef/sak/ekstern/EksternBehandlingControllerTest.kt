package no.nav.familie.ef.sak.ekstern

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class EksternBehandlingControllerTest {

    private val pdlClient = mockk<PdlClient>()
    private val behandlingRepository = mockk<BehandlingRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>()
    private val eksternBehandlingService = EksternBehandlingService(tilkjentYtelseService, behandlingRepository)
    private val eksternBehandlingController = EksternBehandlingController(pdlClient, eksternBehandlingService)

    private val ident1 = "11111111111"
    private val ident2 = "22222222222"

    @BeforeEach
    internal fun setUp() {
        every { pdlClient.hentPersonidenter(ident1, true) } returns PdlIdenter(
                listOf(
                        PdlIdent(ident1, true),
                        PdlIdent(ident2, false)
                )
        )
    }

    @Test
    internal fun `skal feile når den ikke finner identer til personen`() {
        every { pdlClient.hentPersonidenter(ident1, true) } returns PdlIdenter(emptyList())
        val finnesBehandlingForPerson =
                eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent(ident1))
        assertThat(finnesBehandlingForPerson.status)
                .isEqualTo(Ressurs.Status.FEILET)
    }

    @Test
    internal fun `skal returnere false når det ikke finnes en behandling`() {
        every {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(Stønadstype.OVERGANGSSTØNAD, setOf(ident1, ident2))
        } returns null
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent(ident1)).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `skal returnere false når en behandling finnes som er teknisk opphør`() {
        every {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(Stønadstype.OVERGANGSSTØNAD, setOf(ident1, ident2))
        } returns behandling(fagsak(), type = BehandlingType.TEKNISK_OPPHØR)
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent(ident1)).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `skal returnere true når behandling finnes`() {
        every {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(Stønadstype.OVERGANGSSTØNAD, setOf(ident1, ident2))
        } returns behandling(fagsak())
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(Stønadstype.OVERGANGSSTØNAD, PersonIdent(ident1)).data)
                .isEqualTo(true)
    }

    @Test
    internal fun `uten stønadstype - skal returnere false når det ikke finnes noen behandling`() {
        every { behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(any(), setOf(ident1, ident2)) } returns null
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(null, PersonIdent(ident1)).data)
                .isEqualTo(false)
    }

    @Test
    internal fun `opprett en ikke-utdaterte og en utdatert andele som er maksimalt under ett år, forvent behandlinger utdaterte lik false`() {
        mockOpprettTilkjenteYtelser(opprettIkkeUtdatertTilkjentYtelse(), opprettUtdatertTilkjentYtelse())
        assertThat(eksternBehandlingController.finnesBehandlingForPersonIdenter(Stønadstype.OVERGANGSSTØNAD,
                                                                                true,
                                                                                setOf("12345678910")).data).isEqualTo(false)
    }

    @Test
    internal fun `opprett utdaterte andeler som er maksimalt under ett år, forvent behandlinger utdaterte lik true`() {
        mockOpprettTilkjenteYtelser(opprettUtdatertTilkjentYtelse(), opprettUtdatertTilkjentYtelse())
        assertThat(eksternBehandlingController.finnesBehandlingForPersonIdenter(Stønadstype.OVERGANGSSTØNAD,
                                                                                true,
                                                                                setOf("12345678910")).data).isEqualTo(true)
    }

    @Test
    internal fun `uten stønadstype - skal returnere true når det minimum en behandling`() {
        var counter = 0
        every { behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(any(), setOf(ident1, ident2)) } answers {
            if (counter++ == 1) {
                behandling(fagsak())
            } else {
                null
            }
        }
        assertThat(eksternBehandlingController.finnesBehandlingForPerson(null, PersonIdent(ident1)).data)
                .isEqualTo(true)
        verify(exactly = 2) { behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(any(), any()) }
    }

    private fun opprettIkkeUtdatertTilkjentYtelse(): TilkjentYtelse {
        return lagTilkjentYtelse(
                andelerTilkjentYtelse = listOf(
                        lagAndelTilkjentYtelse(
                                beløp = 1,
                                fraOgMed = LocalDate.of(2019, 1, 1),
                                tilOgMed = LocalDate.of(2019, 2, 1)
                        ),
                        lagAndelTilkjentYtelse(
                                beløp = 1,
                                fraOgMed = LocalDate.of(2020, 1, 1),
                                tilOgMed = LocalDate.now().minusMonths(11)
                        )
                )
        )
    }

    private fun opprettUtdatertTilkjentYtelse(): TilkjentYtelse {
        return lagTilkjentYtelse(
                andelerTilkjentYtelse = listOf(
                        lagAndelTilkjentYtelse(
                                beløp = 1,
                                fraOgMed = LocalDate.of(2019, 1, 1),
                                tilOgMed = LocalDate.of(2019, 2, 1)
                        ),
                        lagAndelTilkjentYtelse(
                                beløp = 1,
                                fraOgMed = LocalDate.now().minusMonths(14),
                                tilOgMed = LocalDate.now().minusYears(1).minusMonths(1)
                        )
                )
        )
    }

    private fun mockOpprettTilkjenteYtelser(tilkjentYtelse: TilkjentYtelse, annenTilkjentYtelse: TilkjentYtelse) {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()

        every { eksternBehandlingService.finnesBehandlingFor(any(), any()) } returns true
        every {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(Stønadstype.OVERGANGSSTØNAD, any())
        } returns behandling(id = uuid1)
        every {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(Stønadstype.BARNETILSYN, any())
        } returns behandling(id = uuid2)
        every {
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(Stønadstype.SKOLEPENGER, any())
        } returns null
        every { tilkjentYtelseService.hentForBehandling(uuid1) } returns tilkjentYtelse
        every { tilkjentYtelseService.hentForBehandling(uuid2) } returns annenTilkjentYtelse
    }
}