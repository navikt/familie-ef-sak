package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.felles.util.DatoUtil.dagensDatoMedTid
import no.nav.familie.ef.sak.felles.util.dagensDatoMedTidNorskFormat
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.lagOpprettOppgavebeskrivelse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.format.DateTimeFormatter

class OppgaveUtilTest {
    @Test
    fun `skal legge på metadata med dato på riktig format`() {
        val lagOpprettOppgavebeskrivelse = lagOpprettOppgavebeskrivelse(null)
        val datoMedRiktigFormat = dagensDatoMedTid().format(DateTimeFormatter.ofPattern("dd.MM.yyyy' 'HH:mm"))
        assertThat(lagOpprettOppgavebeskrivelse).contains(datoMedRiktigFormat)
    }

    @Test
    fun `skal legge på oppgave opprettet dersom beskrivelse er null`() {
        val lagOpprettOppgavebeskrivelse = lagOpprettOppgavebeskrivelse(null)
        assertThat(lagOpprettOppgavebeskrivelse).contains("Oppgave opprettet")
    }

    @Test
    fun `skal legge på beskrivelse hvis beskrivelse ikke er null`() {
        val beskrivelse = "Dette er tekst"
        val forventetOppgavebeskrivelse = "--- ${dagensDatoMedTidNorskFormat()} (familie-ef-sak) --- \n$beskrivelse"
        val lagOpprettOppgavebeskrivelse = lagOpprettOppgavebeskrivelse(beskrivelse)
        assertThat(lagOpprettOppgavebeskrivelse).isEqualTo(forventetOppgavebeskrivelse)
    }
}
