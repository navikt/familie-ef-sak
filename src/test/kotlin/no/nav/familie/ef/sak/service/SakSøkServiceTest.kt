package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.Barn
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.ef.sak.repository.domain.Søker
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

internal class SakSøkServiceTest {

    private lateinit var sakRepository: SakRepository
    private lateinit var pdlClient: PdlClient

    private lateinit var sakSøkService: SakSøkService

    @BeforeEach
    fun setUp() {
        sakRepository = mockk()
        pdlClient = mockk()
        sakSøkService = SakSøkService(sakRepository, pdlClient)
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
        every { sakRepository.findBySøkerFødselsnummer(any()) } returns listOf(Sak(
                id = id,
                søknad = byteArrayOf(12),
                saksnummer = "1",
                søker = Søker(personIdent, "Navn"),
                barn = setOf(Barn(fødselsdato = LocalDate.now(), harSammeAdresse = true, fødselsnummer = null, navn = "Navn")),
                journalpostId = "journalId"
        ))
        every { pdlClient.hentSøkerKort(any()) } returns
                PdlSøkerKort(kjønn = listOf(Kjønn(kjønn = KjønnType.MANN)),
                             navn = listOf(Navn("Fornavn", "mellomnavn", "Etternavn")),
                             adressebeskyttelse = listOf(Adressebeskyttelse(AdressebeskyttelseGradering.FORTROLIG)),
                             folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("utflyttet", "ikkeBosatt")))
        val sakSøk = sakSøkService.finnSakForPerson(personIdent)
        assertThat(sakSøk.status).isEqualTo(Ressurs.Status.SUKSESS)
        sakSøk.data!!.let {
            assertThat(it.sakId).isEqualTo(id)
            assertThat(it.personIdent).isEqualTo(personIdent)
            assertThat(it.kjønn).isEqualTo(no.nav.familie.ef.sak.api.dto.Kjønn.MANN)
            assertThat(it.navn.visningsnavn).isEqualTo("Fornavn mellomnavn Etternavn")
            assertThat(it.adressebeskyttelse).isEqualTo(no.nav.familie.ef.sak.api.dto.Adressebeskyttelse.FORTROLIG)
            assertThat(it.folkeregisterpersonstatus).isEqualTo(no.nav.familie.ef.sak.api.dto.Folkeregisterpersonstatus.UTFLYTTET)
        }
    }
}