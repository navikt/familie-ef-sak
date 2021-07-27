package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.Behandlingsjournalpost
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedleggServiceTest {


    private val behandlingService = mockk<BehandlingService>()
    private val journalføringService = mockk<JournalføringService>()

    private val vedleggService = VedleggService(behandlingService, journalføringService)

    @BeforeEach
    internal fun setUp() {
        val behandling = behandling(fagsak())
        every {
            journalføringService.finnJournalposter(any())
        } returns listOf(journalpostSøknad, journalpostEttersendelse)
        every {
            journalføringService.hentJournalpost(journalpostSøknad.journalpostId)
        } returns journalpostSøknad
        every {
            journalføringService.hentJournalpost(journalpostEttersendelse.journalpostId)
        } returns journalpostEttersendelse

        every {
            behandlingService.hentBehandling(any())
        } returns behandling
        every {
            behandlingService.hentAktivIdent(any())
        } returns "1234"
        every {
            behandlingService.hentBehandlingsjournalposter(any())
        } answers {
            val behandlingId = firstArg<UUID>()
            listOf(
                    Behandlingsjournalpost(behandlingId, journalpostSøknad.journalpostId, journalpostSøknad.journalposttype),
                    Behandlingsjournalpost(behandlingId,
                                           journalpostEttersendelse.journalpostId,
                                           journalpostEttersendelse.journalposttype)
            )
        }

    }

    @Test
    internal fun `skal mappe journalposter fra behandlingen`() {
        val journalposter = vedleggService.finnJournalposter(UUID.randomUUID())
        assertThat(journalposter.dokumenterKnyttetTilBehandlingen).hasSize(5)
        assertThat(journalposter.andreDokumenter).isEmpty()
    }

    @Test
    internal fun `skal ikke hente journalposter 2 ganger hvis de allerede finnes i finnJournalposter`() {
        vedleggService.finnJournalposter(UUID.randomUUID())
        verify(exactly = 1) { journalføringService.finnJournalposter(any()) }
        verify(exactly = 0) { journalføringService.hentJournalpost(any()) }
    }

    @Test
    internal fun `skal hente journalpost direke hvis den ikke finnes blant de siste funnet journalpostene`() {
        every { journalføringService.finnJournalposter(any()) } returns emptyList()
        vedleggService.finnJournalposter(UUID.randomUUID())

        verify(exactly = 1) { journalføringService.finnJournalposter(any()) }
        verify(exactly = 2) { journalføringService.hentJournalpost(any()) }
    }

    @Test
    internal fun `skal hente dokumenter fra alle journalposter for en behandling`() {
        val journalPoster = vedleggService.finnJournalposter(UUID.randomUUID())
        val alleVedlegg = journalPoster.andreDokumenter + journalPoster.dokumenterKnyttetTilBehandlingen

        val søknad = alleVedlegg.find { it.dokumentinfoId == søknadsdokument.dokumentInfoId }
        assertThat(søknad).isNotNull
        assertThat(søknad!!.filnavn).isEqualTo("FilnavnDok1")
        assertThat(søknad.tittel).isEqualTo(søknadsdokument.tittel)
        assertThat(søknad.journalpostId).isEqualTo(journalpostSøknad.journalpostId)

        val syktBarn = alleVedlegg.find { it.dokumentinfoId == syktBarnDokument.dokumentInfoId }
        assertThat(syktBarn).isNotNull
        assertThat(syktBarn!!.filnavn).isEqualTo("FilnavnDok2")
        assertThat(syktBarn.tittel).isEqualTo(syktBarnDokument.tittel)
        assertThat(syktBarn.journalpostId).isEqualTo(journalpostSøknad.journalpostId)

        val ukjentKontrakt = alleVedlegg.find { it.dokumentinfoId == ukjentDokument.dokumentInfoId }
        assertThat(ukjentKontrakt).isNotNull
        assertThat(ukjentKontrakt!!.filnavn).isNull()
        assertThat(ukjentKontrakt.tittel).isEqualTo(ukjentKontrakt.tittel)
        assertThat(ukjentKontrakt.journalpostId).isEqualTo(journalpostSøknad.journalpostId)

        val samboerkontrakt = alleVedlegg.find { it.dokumentinfoId == samboerdokument.dokumentInfoId }
        assertThat(samboerkontrakt).isNotNull
        assertThat(samboerkontrakt!!.filnavn).isEqualTo("FilnavnDok3")
        assertThat(samboerkontrakt.tittel).isEqualTo(samboerdokument.tittel)
        assertThat(samboerkontrakt.journalpostId).isEqualTo(journalpostEttersendelse.journalpostId)

        val skilsmissepapirer = alleVedlegg.find { it.dokumentinfoId == skilsmissedokument.dokumentInfoId }
        assertThat(skilsmissepapirer).isNotNull
        assertThat(skilsmissepapirer!!.filnavn).isEqualTo("FilnavnDok4")
        assertThat(skilsmissepapirer.tittel).isEqualTo(skilsmissedokument.tittel)
        assertThat(skilsmissepapirer.journalpostId).isEqualTo(journalpostEttersendelse.journalpostId)

    }

    val søknadsdokument = DokumentInfo(dokumentInfoId = "111",
                                       tittel = "Søknad om overgangsstønad - dokument 1",
                                       brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                       dokumentvarianter = listOf(Dokumentvariant(filnavn = "FilnavnDok1",
                                                                                  variantformat = Dokumentvariantformat.ARKIV),
                                                                  Dokumentvariant(variantformat = Dokumentvariantformat.ORIGINAL)
                                       ))

    val syktBarnDokument = DokumentInfo(dokumentInfoId = "222",
                                        tittel = "Sykt barn",
                                        dokumentvarianter = listOf(Dokumentvariant(filnavn = "FilnavnDok2",
                                                                                   variantformat = Dokumentvariantformat.ARKIV)))

    val ukjentDokument = DokumentInfo(dokumentInfoId = "404",
                                      tittel = "Ukjent tittel",
                                      dokumentvarianter = null)

    val samboerdokument = DokumentInfo(dokumentInfoId = "333",
                                       tittel = "Samboerkontrakt",
                                       brevkode = DokumentBrevkode.OVERGANGSSTØNAD.verdi,
                                       dokumentvarianter = listOf(Dokumentvariant(filnavn = "FilnavnDok3",
                                                                                  variantformat = Dokumentvariantformat.ARKIV)))
    val skilsmissedokument = DokumentInfo(dokumentInfoId = "444",
                                          tittel = "Skilsmissepapirer",
                                          dokumentvarianter = listOf(Dokumentvariant(filnavn = "FilnavnDok4",
                                                                                     variantformat = Dokumentvariantformat.ARKIV)))

    val journalpostSøknad = Journalpost(journalpostId = "1",
                                        journalposttype = Journalposttype.I,
                                        journalstatus = Journalstatus.MOTTATT,
                                        dokumenter = listOf(søknadsdokument, syktBarnDokument, ukjentDokument)
    )

    val journalpostEttersendelse = Journalpost(journalpostId = "2",
                                               journalposttype = Journalposttype.I,
                                               journalstatus = Journalstatus.MOTTATT,
                                               dokumenter = listOf(samboerdokument, skilsmissedokument)
    )


}
