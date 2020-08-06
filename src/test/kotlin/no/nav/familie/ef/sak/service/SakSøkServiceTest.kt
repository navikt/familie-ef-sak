package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.external.Testsøknad.søknad
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.Barn
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.ef.sak.repository.domain.Søker
import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.ef.sak.validering.Sakstilgang
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakSøkServiceTest {

    private lateinit var sakRepository: SakRepository
    private lateinit var pdlClient: PdlClient
    private lateinit var sakstilgang: Sakstilgang
    private lateinit var sakSøkService: SakSøkService

    @BeforeEach
    fun setUp() {
        sakRepository = mockk()
        pdlClient = mockk()
        sakstilgang = mockk()
        every { sakstilgang.harTilgang(any() as Sak) } returns true
        sakSøkService = SakSøkService(sakRepository, pdlClient, sakstilgang)
    }

    @Test
    fun `skal ikke ha tilgang på sak`() {
        mockPdlHentSøkerKortBolk()
        every { sakRepository.findAll() } returns listOf(sak(UUID.randomUUID(), "11111122222", "22222211111"))

        assertThat(sakSøkService.finnSaker().data!!.saker)
                .hasSize(1)

        every { sakstilgang.harTilgang(any() as Sak) } returns false

        assertThat(sakSøkService.finnSaker().data!!.saker)
                .isEmpty()
    }

    @Test
    fun `Skal returnere ressurs med feil når det ikke finnes en sak`() {
        every { sakRepository.findBySøkerFødselsnummer(any()) } returns emptyList()
        val sakSøk = sakSøkService.finnSakForPerson("")
        assertThat(sakSøk.status).isEqualTo(Ressurs.Status.FEILET)
        assertThat(sakSøk.frontendFeilmelding).isEqualTo("Finner ikke noen sak på personen")
    }

    @Test
    fun `Skal returnere ressurs når man finner sak og person`() {
        val id = UUID.randomUUID()
        val personIdent = "11111122222"
        every { sakRepository.findBySøkerFødselsnummer(any()) } returns listOf(sak(id, personIdent, "22222211111"))
        mockPdlHentSøkerKortBolk()
        val sakSøk = sakSøkService.finnSakForPerson(personIdent)
        assertThat(sakSøk.status).isEqualTo(Ressurs.Status.SUKSESS)
        sakSøk.data!!.let {
            assertThat(it.sakId).isEqualTo(id)
            assertThat(it.personIdent).isEqualTo(personIdent)
            assertThat(it.kjønn).isEqualTo(no.nav.familie.ef.sak.api.dto.Kjønn.MANN)
            assertThat(it.navn.visningsnavn).isEqualTo("Fornavn mellomnavn Etternavn")
        }
    }

    private fun mockPdlHentSøkerKortBolk() {
        every { pdlClient.hentSøkerKortBolk(any()) } answers {
            val list = firstArg() as List<String>
            list.map {
                it to PdlSøkerKort(kjønn = listOf(Kjønn(kjønn = KjønnType.MANN)),
                                   navn = listOf(Navn("Fornavn",
                                                      "mellomnavn",
                                                      "Etternavn",
                                                      Metadata(endringer = listOf(MetadataEndringer(LocalDate.now()))))))
            }.toMap()
        }
    }

    private fun sak(id: UUID, personIdent: String, barnIdent: String): Sak {
        return Sak(
                id = id,
                søknad = objectMapper.writeValueAsBytes(søknad),
                type = SøknadType.OVERGANGSSTØNAD,
                saksnummer = "1",
                søker = Søker(personIdent, "Navn"),
                barn = setOf(Barn(fødselsdato = LocalDate.now(),
                                  harSammeAdresse = true,
                                  fødselsnummer = barnIdent,
                                  navn = "Navn")),
                journalpostId = "journalId"
        )
    }
}