package no.nav.familie.ef.sak.journalføring

import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import no.nav.familie.kontrakter.felles.journalpost.Journalstatus
import no.nav.familie.kontrakter.felles.journalpost.RelevantDato
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class JournalpostDatoUtilTest {

    @Test
    internal fun `skal bruke dato mottatt som journalpostdato dersom den finnes`() {
        val dato = LocalDateTime.of(2022, 1, 10, 13, 15)
        val relevanteDatoer = listOf(RelevantDato(dato, "DATO_REGISTRERT"))
        val journalpost = opprettJournalpost(relevanteDatoer = relevanteDatoer)
        Assertions.assertThat(JournalpostDatoUtil.mestRelevanteDato(journalpost)).isEqualTo(dato)
    }

    @Test
    internal fun `skal bruke dato journalført som journalpostdato dersom dato mottatt ikke finnes`() {
        val dato = LocalDateTime.of(2022, 1, 10, 13, 15)
        val relevanteDatoer = listOf(
                RelevantDato(dato, "DATO_JOURNALFOERT"),
                RelevantDato(dato.plusDays(1), "DATO_DOKUMENT"),
                RelevantDato(dato.plusDays(2), "DATO_OPPRETTET"),
                RelevantDato(dato.plusDays(3), "DATO_UKJENT"),
        )
        val journalpost = opprettJournalpost(relevanteDatoer = relevanteDatoer)
        Assertions.assertThat(JournalpostDatoUtil.mestRelevanteDato(journalpost)).isEqualTo(dato)
    }

    @Test
    internal fun `skal bruke dato dokument som journalpostdato dersom dato mottatt og dato journalført ikke finnes`() {
        val dato = LocalDateTime.of(2022, 1, 10, 13, 15)
        val relevanteDatoer = listOf(
                RelevantDato(dato, "DATO_DOKUMENT"),
                RelevantDato(dato.plusDays(1), "DATO_OPPRETTET"),
                RelevantDato(dato.plusDays(2), "DATO_UKJENT"),
        )
        val journalpost = opprettJournalpost(relevanteDatoer = relevanteDatoer)
        Assertions.assertThat(JournalpostDatoUtil.mestRelevanteDato(journalpost)).isEqualTo(dato)
    }

    @Test
    internal fun `skal bruke dato opprettet som journalpostdato dersom dato mottatt, dato journalført og dato dokument ikke finnes`() {
        val dato = LocalDateTime.of(2022, 1, 10, 13, 15)
        val relevanteDatoer = listOf(
                RelevantDato(dato, "DATO_OPPRETTET"),
                RelevantDato(dato.plusDays(2), "DATO_UKJENT"),
        )
        val journalpost = opprettJournalpost(relevanteDatoer = relevanteDatoer)
        Assertions.assertThat(JournalpostDatoUtil.mestRelevanteDato(journalpost)).isEqualTo(dato)
    }

    @Test
    internal fun `skal returnere tilfeldig relevant dato for journalpostdato dersom ingen kjente datoer finnes`() {
        val dato = LocalDateTime.of(2022, 1, 10, 13, 15)
        val relevanteDatoer = listOf(
                RelevantDato(dato, "DATO_UKJENT"),
        )
        val journalpost = opprettJournalpost(relevanteDatoer = relevanteDatoer)
        Assertions.assertThat(JournalpostDatoUtil.mestRelevanteDato(journalpost)).isEqualTo(dato)
    }

    private fun opprettJournalpost(relevanteDatoer: List<RelevantDato>): Journalpost {
        // Kjente dato-typer "DATO_REGISTRERT", "DATO_JOURNALFOERT", "DATO_DOKUMENT", "DATO_OPPRETTET", "DATO_UKJENT"
        return Journalpost(journalpostId = "1234",
                           journalposttype = Journalposttype.I,
                           journalstatus = Journalstatus.FERDIGSTILT,
                           dokumenter = listOf(),
                           relevanteDatoer = relevanteDatoer)
    }


}