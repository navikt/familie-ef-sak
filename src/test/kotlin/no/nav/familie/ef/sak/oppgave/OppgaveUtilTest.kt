package no.nav.familie.ef.sak.oppgave

import no.nav.familie.ef.sak.felles.util.DatoUtil.dagensDatoMedTid
import no.nav.familie.ef.sak.felles.util.dagensDatoMedTidNorskFormat
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.finnPersonidentForOppgave
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.identGruppe
import no.nav.familie.ef.sak.oppgave.OppgaveUtil.lagOpprettOppgavebeskrivelse
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
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

    @Test
    fun `skal finne personident for oppgave med folkeregisterident eller NPID`() {
        val fnr = OppgaveIdentV2("12345678901", IdentGruppe.FOLKEREGISTERIDENT)
        val npid = OppgaveIdentV2("01226512345", IdentGruppe.NPID)
        val aktørId = OppgaveIdentV2("1234567890123", IdentGruppe.AKTOERID)

        assertThat(finnPersonidentForOppgave(Oppgave(identer = listOf(aktørId, npid, fnr)))).isEqualTo(fnr.ident)
        assertThat(finnPersonidentForOppgave(Oppgave(identer = listOf(aktørId, npid)))).isEqualTo(npid.ident)
        assertThat(finnPersonidentForOppgave(Oppgave(identer = listOf(aktørId)))).isNull()
    }

    @Test
    fun `skal utlede identgruppe for oppgave`() {
        assertThat(identGruppe("12345678901")).isEqualTo(IdentGruppe.FOLKEREGISTERIDENT)
        assertThat(identGruppe("01226512345")).isEqualTo(IdentGruppe.NPID)
    }
}
