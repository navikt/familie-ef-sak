package no.nav.familie.ef.sak.no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.AktivitetspliktigAlder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AktivitetspliktigAlderTest {
    @Test
    fun `barn regnes som 6 mnd gammelt når fødselsdato er mer enn 182 dager siden`() {
        val fødselsdato = LocalDate.now().minusDays(183)
        assertThat(AktivitetspliktigAlder.fromFødselsdato(fødselsdato)).isEqualTo(AktivitetspliktigAlder.SEKS_MND)
    }

    @Test
    fun `returner 1 år alder-enum dersom fødselsdato er mer enn ett år siden og innenfor cutoff`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(1)
        assertThat(AktivitetspliktigAlder.fromFødselsdato(fødselsdato)).isEqualTo(AktivitetspliktigAlder.ETT_ÅR)
    }

    @Test
    fun `returner null dersom fødselsdato er mer enn ett år siden og utenfor cutoff`() {
        val fødselsdato = LocalDate.now().minusYears(1).minusDays(9)
        assertThat(AktivitetspliktigAlder.fromFødselsdato(fødselsdato)).isNull()
    }

    @Test
    fun `returner null dersom fødselsdato er mer enn 6 mnd siden og utenfor cutoff`() {
        val fødselsdato = LocalDate.now().minusMonths(6).minusDays(8)
        assertThat(AktivitetspliktigAlder.fromFødselsdato(fødselsdato)).isNull()
    }
}
