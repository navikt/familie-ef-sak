package no.nav.familie.ef.sak.tilkjentytelse.uttrekk

import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.YearMonth
import java.util.UUID

data class VedtakMedTilsynordningKanditat(val fagsakId: UUID, val andelshistorikk: List<AndelHistorikkDto>) {

    fun harAndelOgManglerTilsyn(år: Int) =
        andelshistorikk.any { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }

    fun harAvsluttetPeriodeMedManglendeTilsyn(år: Int): Boolean =
        andelshistorikk.filter { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }
            .all { it.andel.periode.tom <= YearMonth.now() }

    fun antallMånederMedManglendeTilsynSomErAvsluttet(år: Int): Long =
        andelshistorikk.filter { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }
            .map { it.andel.periode.lengdeIHeleMåneder() }.sum()

    fun beløpForManglendeTilsynSomErAvsluttet(år: Int): Long =
        andelshistorikk.filter { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }
            .map { it.andel.beløp * it.andel.periode.lengdeIHeleMåneder() }.sum()
}

private fun Månedsperiode.harPeriodeI(år: Int): Boolean {
    val janÅr = YearMonth.of(år, 1)
    val desÅr = YearMonth.of(år, 12)
    val periodeÅr = Månedsperiode(fom = janÅr, tom = desÅr)
    return periodeÅr overlapper this
}
