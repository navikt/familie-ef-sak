package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.JournalpostClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.Behandlingsjournalpost
import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.journalpost.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VedleggServiceTest {


    private val behandlingService = mockk<BehandlingService>()
    private val journalpostClient = mockk<JournalpostClient>()

    private val vedleggService = VedleggService(behandlingService, journalpostClient)

    @BeforeEach
    internal fun setUp() {
        val behandling = behandling(fagsak())
        every {
            journalpostClient.finnJournalposter(any())
        } returns listOf(journalpostSøknad, journalpostEttersendelse)

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
                    Behandlingsjournalpost(behandlingId, journalpostEttersendelse.journalpostId, journalpostEttersendelse.journalposttype)
            )
        }

    }

    @Test
    internal fun `skal hente dokumenter fra alle journalposter for en behandling`() {
        val journalPoster = vedleggService.finnJournalposter(UUID.randomUUID())
        val alleVedlegg = journalPoster.andreDokumenter + journalPoster.dokumenterKnyttetTilBehandlingen

        val søknad = alleVedlegg.find { it.dokumentinfoId == søknadsdokument.dokumentInfoId }
        Assertions.assertThat(søknad).isNotNull
        Assertions.assertThat(søknad!!.filnavn).isEqualTo("FilnavnDok1")
        Assertions.assertThat(søknad.tittel).isEqualTo(søknadsdokument.tittel)
        Assertions.assertThat(søknad.journalpostId).isEqualTo(journalpostSøknad.journalpostId)

        val syktBarn = alleVedlegg.find { it.dokumentinfoId == syktBarnDokument.dokumentInfoId }
        Assertions.assertThat(syktBarn).isNotNull
        Assertions.assertThat(syktBarn!!.filnavn).isEqualTo("FilnavnDok2")
        Assertions.assertThat(syktBarn.tittel).isEqualTo(syktBarnDokument.tittel)
        Assertions.assertThat(syktBarn.journalpostId).isEqualTo(journalpostSøknad.journalpostId)

        val ukjentKontrakt = alleVedlegg.find { it.dokumentinfoId == ukjentDokument.dokumentInfoId }
        Assertions.assertThat(ukjentKontrakt).isNotNull
        Assertions.assertThat(ukjentKontrakt!!.filnavn).isNull()
        Assertions.assertThat(ukjentKontrakt.tittel).isEqualTo(ukjentKontrakt.tittel)
        Assertions.assertThat(ukjentKontrakt.journalpostId).isEqualTo(journalpostSøknad.journalpostId)

        val samboerkontrakt = alleVedlegg.find { it.dokumentinfoId == samboerdokument.dokumentInfoId }
        Assertions.assertThat(samboerkontrakt).isNotNull
        Assertions.assertThat(samboerkontrakt!!.filnavn).isEqualTo("FilnavnDok3")
        Assertions.assertThat(samboerkontrakt.tittel).isEqualTo(samboerdokument.tittel)
        Assertions.assertThat(samboerkontrakt.journalpostId).isEqualTo(journalpostEttersendelse.journalpostId)

        val skilsmissepapirer = alleVedlegg.find { it.dokumentinfoId == skilsmissedokument.dokumentInfoId }
        Assertions.assertThat(skilsmissepapirer).isNotNull
        Assertions.assertThat(skilsmissepapirer!!.filnavn).isEqualTo("FilnavnDok4")
        Assertions.assertThat(skilsmissepapirer.tittel).isEqualTo(skilsmissedokument.tittel)
        Assertions.assertThat(skilsmissepapirer.journalpostId).isEqualTo(journalpostEttersendelse.journalpostId)

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
