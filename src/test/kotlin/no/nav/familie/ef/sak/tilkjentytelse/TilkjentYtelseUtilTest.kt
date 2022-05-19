package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseUtil.tilPeriodeType
import no.nav.familie.kontrakter.ef.iverksett.Periodetype
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TilkjentYtelseUtilTest {

    @Test
    internal fun `mapper stønadstype til riktig periodetype`() {
        assertThat(StønadType.OVERGANGSSTØNAD.tilPeriodeType()).isEqualTo(Periodetype.MÅNED)
        assertThat(StønadType.BARNETILSYN.tilPeriodeType()).isEqualTo(Periodetype.MÅNED)
        assertThat(StønadType.SKOLEPENGER.tilPeriodeType()).isEqualTo(Periodetype.ENGANGSUTBETALING)
    }
}