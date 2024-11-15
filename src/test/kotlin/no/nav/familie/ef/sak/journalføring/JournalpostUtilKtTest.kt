package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.ef.sak.DokumentBrevkode
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottaker
import no.nav.familie.kontrakter.felles.journalpost.AvsenderMottakerIdType
import no.nav.familie.kontrakter.felles.journalpost.DokumentInfo
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariant
import no.nav.familie.kontrakter.felles.journalpost.Dokumentvariantformat
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class JournalpostUtilKtTest {
    @Test
    internal fun `journalpost - avsendermottaker mangler for inngående journalpost`() {
        val journalpostUtenAvsenderMottaker =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                emptyList(),
                journalposttype = Journalposttype.I,
            )
        assertThat(journalpostUtenAvsenderMottaker.harUgyldigAvsenderMottaker()).isTrue
    }

    @Test
    internal fun `journalpost - avsendermottaker mangler for utgående journalpost`() {
        val journalpostUtenAvsenderMottaker =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                emptyList(),
                journalposttype = Journalposttype.U,
            )
        assertThat(journalpostUtenAvsenderMottaker.harUgyldigAvsenderMottaker()).isTrue
    }

    @Test
    internal fun `journalpost - avsendermottaker mangler for notat-journalpost - skal ikke bety noe`() {
        val journalpostUtenAvsenderMottaker =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                emptyList(),
                journalposttype = Journalposttype.N,
            )
        assertThat(journalpostUtenAvsenderMottaker.harUgyldigAvsenderMottaker()).isFalse
    }

    @Test
    internal fun `journalpost - avsendermottaker finnes for inngående journalpost`() {
        val journalpostUtenAvsenderMottaker =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                emptyList(),
                journalposttype = Journalposttype.I,
                avsenderMottaker =
                    AvsenderMottaker(
                        id = "a",
                        type = AvsenderMottakerIdType.FNR,
                        navn = "Ola",
                        land = "Norge",
                        erLikBruker = true,
                    ),
            )
        assertThat(journalpostUtenAvsenderMottaker.harUgyldigAvsenderMottaker()).isFalse
    }

    @Test
    internal fun `journalpost - avsendermottaker finnes for inngående journalpost, men navn er tom streng`() {
        val journalpostUtenAvsenderMottaker =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                emptyList(),
                journalposttype = Journalposttype.I,
                avsenderMottaker =
                    AvsenderMottaker(
                        id = "a",
                        type = AvsenderMottakerIdType.FNR,
                        navn = "",
                        land = "Norge",
                        erLikBruker = true,
                    ),
            )
        assertThat(journalpostUtenAvsenderMottaker.harUgyldigAvsenderMottaker()).isTrue()
    }

    @Test
    internal fun `journalpost - avsendermottaker finnes for inngående journalpost, men mangler navn`() {
        val journalpostUtenAvsenderMottaker =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                emptyList(),
                journalposttype = Journalposttype.I,
                avsenderMottaker =
                    AvsenderMottaker(
                        id = "a",
                        type = AvsenderMottakerIdType.FNR,
                        navn = "",
                        land = "Norge",
                        erLikBruker = true,
                    ),
            )
        assertThat(journalpostUtenAvsenderMottaker.harUgyldigAvsenderMottaker()).isTrue()
    }

    @Test
    internal fun `harStrukturertSøknad - overgangsstønad med søknad skal returnere true`() {
        val journalpost =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                listOf(dokumentSøknad(DokumentBrevkode.OVERGANGSSTØNAD)),
            )
        assertThat(journalpost.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - barnetilsyn med søknad skal returnere true`() {
        val journalpost =
            lagjournalpost(
                behandlingstemaBarnetilsyn,
                listOf(dokumentSøknad(DokumentBrevkode.BARNETILSYN)),
            )
        assertThat(journalpost.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - skolepenger med søknad skal returnere true`() {
        val journalpost =
            lagjournalpost(
                behandlingstemaSkolepenger,
                listOf(dokumentSøknad(DokumentBrevkode.SKOLEPENGER)),
            )
        assertThat(journalpost.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - overgangsstønad med søknad og vedlegg skal returnere true`() {
        val journalpost =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                listOf(
                    dokumentUkjent,
                    dokumentEttersending(DokumentBrevkode.OVERGANGSSTØNAD),
                    dokumentSøknad(DokumentBrevkode.OVERGANGSSTØNAD),
                ),
            )
        assertThat(journalpost.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - journalpost uten behandlingstema, men brevkode skolepenger skal returnere true`() {
        val journalpostSkolepengeSøknad =
            lagjournalpost(
                behandlingstema = null,
                listOf(dokumentSøknad(DokumentBrevkode.SKOLEPENGER)),
            )
        assertThat(journalpostSkolepengeSøknad.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - journalpost uten behandlingstema, men brevkode overgangsstønad skal returnere true`() {
        val journalpostOvergangsstønad =
            lagjournalpost(
                behandlingstema = null,
                listOf(dokumentSøknad(DokumentBrevkode.OVERGANGSSTØNAD)),
            )
        assertThat(journalpostOvergangsstønad.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - journalpost uten behandlingstema, men brevkode barnetilsyn skal returnere true`() {
        val journalpostBarnetilsynSøknad =
            lagjournalpost(
                behandlingstema = null,
                listOf(dokumentSøknad(DokumentBrevkode.BARNETILSYN)),
            )
        assertThat(journalpostBarnetilsynSøknad.harStrukturertSøknad()).isTrue
    }

    @Test
    internal fun `harStrukturertSøknad - overgangsstønad med med ukjent dokument skal returnere false`() {
        val journalpost =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                listOf(dokumentUkjent),
            )
        assertThat(journalpost.harStrukturertSøknad()).isFalse
    }

    @Test
    internal fun `harStrukturertSøknad - overgangsstønad med med ettersendingsdokument skal returnere false`() {
        val journalpost =
            lagjournalpost(
                behandlingstemaOvergangsstønad,
                listOf(dokumentEttersending(DokumentBrevkode.OVERGANGSSTØNAD)),
            )
        assertThat(journalpost.harStrukturertSøknad()).isFalse
    }

    private val journalpostId = "98765"
    private val dokumentInfoIdMedJsonVerdi = "12345"
    private val behandlingstemaOvergangsstønad = "ab0071"
    private val behandlingstemaSkolepenger = "ab0177"
    private val behandlingstemaBarnetilsyn = "ab0028"

    fun dokumentSøknad(brevkode: DokumentBrevkode) =
        DokumentInfo(
            dokumentInfoIdMedJsonVerdi,
            "Vedlegg1",
            brevkode = brevkode.verdi,
            dokumentvarianter =
                listOf(
                    Dokumentvariant(Dokumentvariantformat.ORIGINAL, saksbehandlerHarTilgang = true),
                    Dokumentvariant(Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true),
                ),
        )

    fun dokumentEttersending(brevkode: DokumentBrevkode) =
        DokumentInfo(
            "99999",
            "Vedlegg2",
            brevkode = brevkode.verdi,
            dokumentvarianter =
                listOf(Dokumentvariant(Dokumentvariantformat.ARKIV, saksbehandlerHarTilgang = true)),
        )

    val dokumentUkjent =
        DokumentInfo(
            "23456",
            "Vedlegg3",
            brevkode = "XYZ",
        )

    fun lagjournalpost(
        behandlingstema: String?,
        dokumenter: List<DokumentInfo>,
        journalposttype: Journalposttype = Journalposttype.I,
        avsenderMottaker: AvsenderMottaker? = null,
    ) = Journalpost(
        journalpostId = journalpostId,
        journalposttype = journalposttype,
        avsenderMottaker = avsenderMottaker,
        journalstatus = Journalstatus.MOTTATT,
        tema = "ENF",
        behandlingstema = behandlingstema,
        dokumenter = dokumenter,
        tittel = "Tittel",
    )
}
