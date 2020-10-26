package no.nav.familie.ef.sak.api.journalføring

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAktørId
import no.nav.familie.ef.sak.integration.dto.pdl.PdlHentIdenter
import no.nav.familie.ef.sak.integration.dto.pdl.PdlIdent
import no.nav.familie.ef.sak.service.JournalføringService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.journalpost.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class JournalføringControllerTest {


    val journalføringService = mockk<JournalføringService>()
    val pdlClient = mockk<PdlClient>()
    val tilgangService: TilgangService = mockk()
    val journalføringController = JournalføringController(journalføringService, pdlClient, tilgangService)

    @Test
    internal fun `skal hente journalpost med personident utledet fra pdl`() {


        every {
            pdlClient.hentPersonident(aktørId)
        } returns PdlHentIdenter(PdlAktørId(listOf(PdlIdent(personIdentFraPdl))))

        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedAktørId

        // TODO: Vil vi teste tilgangskontrollen her?
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        val journalpostResponse = journalføringController.hentJournalPost("1234")
        Assertions.assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        Assertions.assertThat(journalpostResponse.data?.journalpost?.journalpostId).isEqualTo("1234")
    }

    @Test
    internal fun `skal hente journalpost med personident fra journalposten`() {

        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedFødselsnummer

        // TODO: Vil vi teste tilgangskontrollen her?
        every {
            tilgangService.validerTilgangTilPersonMedBarn(any())
        } just Runs

        val journalpostResponse = journalføringController.hentJournalPost("1234")
        Assertions.assertThat(journalpostResponse.data?.personIdent).isEqualTo(personIdentFraPdl)
        Assertions.assertThat(journalpostResponse.data?.journalpost?.journalpostId).isEqualTo("1234")
    }

    @Test
    internal fun `skal feile hvis journalpost mangler bruker`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostUtenBruker

        assertThrows<IllegalStateException> { journalføringController.hentJournalPost("1234") }
    }


    @Test
    internal fun `skal feile hvis journalpost har orgnr`() {
        every {
            journalføringService.hentJournalpost(any())
        } returns journalpostMedOrgnr

        assertThrows<IllegalStateException> { journalføringController.hentJournalPost("1234") }
    }

    val aktørId = "11111111111"
    val personIdentFraPdl = "12345678901"

    val journalpostMedAktørId = Journalpost(journalpostId = "1234",
                                  journalposttype = Journalposttype.I,
                                  journalstatus = Journalstatus.MOTTATT,
                                  tema = "ENF",
                                  behandlingstema = "ab0071",
                                  tittel = "abrakadabra",
                                  bruker = Bruker(type = BrukerIdType.AKTOERID, id = aktørId),
                                  journalforendeEnhet = "4817",
                                  kanal = "SKAN_IM",
                                  dokumenter = listOf(
                                          DokumentInfo(
                                                  dokumentInfoId = "12345",
                                                  tittel = "Tittel",
                                                  brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                                  dokumentvarianter = listOf(Dokumentvariant(variantformat = "ARKIV"))
                                          )
                                  )
    )

    val journalpostMedFødselsnummer = journalpostMedAktørId.copy(
        bruker = Bruker(type = BrukerIdType.FNR, id = personIdentFraPdl)
    )
    val journalpostUtenBruker = journalpostMedAktørId.copy(
        bruker = null
    )
    val journalpostMedOrgnr = journalpostMedAktørId.copy(
            bruker = Bruker(type = BrukerIdType.ORGNR, id = "12345")
    )

}