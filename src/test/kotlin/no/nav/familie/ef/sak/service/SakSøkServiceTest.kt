package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.Kjønn
import no.nav.familie.ef.sak.integration.dto.pdl.KjønnType
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøkerKort
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
                             navn = listOf(Navn("Fornavn", "mellomnavn", "Etternavn")))
        val sakSøk = sakSøkService.finnSakForPerson(personIdent)
        assertThat(sakSøk.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(sakSøk.data?.sakId).isEqualTo(id)
        assertThat(sakSøk.data?.personIdent).isEqualTo(personIdent)
        assertThat(sakSøk.data?.kjønn).isEqualTo(no.nav.familie.ef.sak.api.dto.Kjønn.MANN)
        assertThat(sakSøk.data?.navn!!.visningsnavn).isEqualTo("Fornavn mellomnavn Etternavn")
    }
}