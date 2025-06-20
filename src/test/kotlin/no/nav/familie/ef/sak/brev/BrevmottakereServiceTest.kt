package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.MottakerRolle.BRUKER
import no.nav.familie.ef.sak.brev.domain.MottakerRolle.FULLMEKTIG
import no.nav.familie.ef.sak.brev.domain.MottakerRolle.VERGE
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull

internal class BrevmottakereServiceTest {
    val brevmottakereRepository = mockk<BrevmottakereRepository>()
    val frittståendeBrevmottakereRepository = mockk<FrittståendeBrevmottakereRepository>()
    val tilordnetRessursService = mockk<TilordnetRessursService>()
    val brevmottakereService = BrevmottakereService(brevmottakereRepository, frittståendeBrevmottakereRepository, tilordnetRessursService)
    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns true
    }

    @Test
    fun `skal feile hvis brevmottakere settes til ingen mottakere`() {
        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val brevmottakereDto = BrevmottakereDto(personer = emptyList(), organisasjoner = emptyList())

        val feil =
            assertThrows<ApiFeil> {
                brevmottakereService.lagreBrevmottakere(behandlingId = behandling.id, brevmottakereDto = brevmottakereDto)
            }

        assertThat(feil.feil).isEqualTo("Vedtaksbrevet må ha minst 1 mottaker")
    }

    @Test
    fun `skal feile hvis mer enn 4 brevmottakere`() {
        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val feil =
            assertThrows<ApiFeil> {
                brevmottakereService.lagreBrevmottakere(
                    behandlingId = behandling.id,
                    brevmottakereDto = brevmottakereDtoMed5Mottakere,
                )
            }

        assertThat(feil.feil).isEqualTo("Vedtaksbrevet kan ikke ha mer enn 4 mottakere")
    }

    @Test
    fun `skal feile hvis samme mottaker legges til flere ganger`() {
        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val brevmottakereDto =
            BrevmottakereDto(
                personer =
                    listOf(
                        BrevmottakerPerson(
                            personIdent = "123",
                            mottakerRolle = BRUKER,
                            navn = "navn",
                        ),
                        BrevmottakerPerson(
                            personIdent = "123",
                            mottakerRolle = VERGE,
                            navn = "navn",
                        ),
                    ),
                organisasjoner = emptyList(),
            )

        val feil =
            assertThrows<ApiFeil> {
                brevmottakereService.lagreBrevmottakere(
                    behandlingId = behandling.id,
                    brevmottakereDto = brevmottakereDto,
                )
            }

        assertThat(feil.feil).isEqualTo("En person kan bare legges til en gang som brevmottaker")
    }

    @Test
    fun `skal feile hvis saksbehandler ikke eier behandling`() {
        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()
        every { tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(any()) } returns false

        val brevmottakereDto =
            BrevmottakereDto(
                personer =
                    listOf(
                        BrevmottakerPerson(
                            personIdent = "123",
                            mottakerRolle = BRUKER,
                            navn = "navn",
                        ),
                    ),
                organisasjoner = emptyList(),
            )

        val feil =
            assertThrows<ApiFeil> {
                brevmottakereService.lagreBrevmottakere(
                    behandlingId = behandling.id,
                    brevmottakereDto = brevmottakereDto,
                )
            }

        assertThat(feil.feil).isEqualTo("Behandlingen eies av noen andre og brevmottakere kan derfor ikke endres av deg")
    }

    @Test
    fun `skal feile hvis samme organisasjon legges til flere ganger`() {
        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val brevmottakereDto =
            BrevmottakereDto(
                personer = emptyList(),
                organisasjoner =
                    listOf(
                        BrevmottakerOrganisasjon(
                            organisasjonsnummer = "123",
                            navnHosOrganisasjon = "n",
                            mottakerRolle = FULLMEKTIG,
                        ),
                        BrevmottakerOrganisasjon(
                            organisasjonsnummer = "123",
                            navnHosOrganisasjon = "n",
                            mottakerRolle = FULLMEKTIG,
                        ),
                    ),
            )

        val feil =
            assertThrows<ApiFeil> {
                brevmottakereService.lagreBrevmottakere(
                    behandlingId = behandling.id,
                    brevmottakereDto = brevmottakereDto,
                )
            }

        assertThat(feil.feil).isEqualTo("En organisasjon kan bare legges til en gang som brevmottaker")
    }

    private val brevmottakereDtoMed5Mottakere =
        BrevmottakereDto(
            personer =
                listOf(
                    BrevmottakerPerson(
                        "A",
                        "A",
                        VERGE,
                    ),
                    BrevmottakerPerson(
                        "B",
                        "B",
                        BRUKER,
                    ),
                    BrevmottakerPerson(
                        "E",
                        "E",
                        BRUKER,
                    ),
                ),
            organisasjoner =
                listOf(
                    BrevmottakerOrganisasjon(
                        "C",
                        "C",
                        FULLMEKTIG,
                    ),
                    BrevmottakerOrganisasjon(
                        "D",
                        "D",
                        FULLMEKTIG,
                    ),
                ),
        )
}
