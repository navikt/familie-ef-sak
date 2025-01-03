package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import java.time.YearMonth
import java.util.UUID

data class NæringsinntektDataForBeregning(
    val oppgave: Oppgave,
    val personIdent: String,
    val fagsak: Fagsak,
    val behandlingId: UUID,
    val tilkjentYtelse: TilkjentYtelse,
    val fjoråretsNæringsinntekt: Int,
    val fjoråretsPersonInntekt: Int,
    val forventetInntektIFjor: Int,
) {
    private val årstallIFjor = YearMonth.now().year - 1

    fun skalKontrolleres(): Boolean = antallMånederMedVedtakForÅr(årstallIFjor) > 3

    fun oppfyllerAktivitetsplikt() = fjoråretsNæringsinntekt > INNTEKTSGRENSE_NÆRING || (fjoråretsPersonInntekt + fjoråretsNæringsinntekt) > INNTEKTSGRENSE_PERSON_OG_NÆRING

    fun har10ProsentØkningEllerMer() = (forventetInntektIFjor * 1.1) <= (fjoråretsPersonInntekt + fjoråretsNæringsinntekt)

    fun antallMånederMedVedtakForÅr(
        årstall: Int,
    ): Long {
        val perioder = tilkjentYtelse.andelerTilkjentYtelse.map { it.periode.snitt(Månedsperiode(YearMonth.of(årstall, 1), YearMonth.of(årstall, 12))) }
        return perioder.sumOf { it?.lengdeIHeleMåneder() ?: 0 }
    }

    companion object {
        private const val INNTEKTSGRENSE_NÆRING = 50_000
        private const val INNTEKTSGRENSE_PERSON_OG_NÆRING = 190_000
    }
}
