package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.MottakerRolle.BRUKER
import no.nav.familie.ef.sak.brev.domain.MottakerRolle.FULLMAKT
import no.nav.familie.ef.sak.brev.domain.MottakerRolle.VERGE
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.repository.behandling
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.repository.findByIdOrNull

internal class BrevmottakereServiceTest {

    val brevmottakereRepository = mockk<BrevmottakereRepository>()
    val brevmottakereService = BrevmottakereService(brevmottakereRepository)
    val behandling = behandling()

    @Test
    fun `skal feile hvis brevmottakere settes til ingen mottakere`() {

        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val brevmottakereDto = BrevmottakereDto(personer = emptyList(), organisasjoner = emptyList())

        assertThrows<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(behandlingId = behandling.id, brevmottakereDto = brevmottakereDto)
        }
    }

    @Test
    fun `skal feile hvis mer enn 2 brevmottakere`() {

        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        assertThrows<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(behandlingId = behandling.id,
                                                    brevmottakereDto = brevmottakereDtoMed3Mottakere)
        }
    }

    @Test
    fun `skal feile hvis samme mottaker legges til flere ganger`() {

        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val brevmottakereDto = BrevmottakereDto(personer = listOf(BrevmottakerPerson(personIdent = "123",
                                                                                     mottakerRolle = BRUKER,
                                                                                     navn = "navn"),
                                                                  BrevmottakerPerson(personIdent = "123",
                                                                                     mottakerRolle = VERGE,
                                                                                     navn = "navn")),
                                                organisasjoner = emptyList())

        assertThrows<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(behandlingId = behandling.id,
                                                    brevmottakereDto = brevmottakereDto)
        }
    }

    @Test
    fun `skal hvis samme organisasjon legges til flere ganger`() {

        every { brevmottakereRepository.findByIdOrNull(behandling.id) } returns mockk()

        val brevmottakereDto = BrevmottakereDto(personer = emptyList(),
                                                organisasjoner = listOf(BrevmottakerOrganisasjon(organisasjonsnummer = "123",
                                                                                                 navnHosOrganisasjon = "n",
                                                                                                 mottakerRolle = FULLMAKT),
                                                                        BrevmottakerOrganisasjon(organisasjonsnummer = "123",
                                                                                                 navnHosOrganisasjon = "n",
                                                                                                 mottakerRolle = FULLMAKT)))

        assertThrows<ApiFeil> {
            brevmottakereService.lagreBrevmottakere(behandlingId = behandling.id,
                                                    brevmottakereDto = brevmottakereDto)
        }
    }

    private val brevmottakereDtoMed3Mottakere = BrevmottakereDto(
            personer = listOf(
                    BrevmottakerPerson("A",
                                       "A",
                                       VERGE),
                    BrevmottakerPerson("B",
                                       "B",
                                       BRUKER)),
            organisasjoner = listOf(
                    BrevmottakerOrganisasjon("C",
                                             "C",
                                             FULLMAKT)))
}