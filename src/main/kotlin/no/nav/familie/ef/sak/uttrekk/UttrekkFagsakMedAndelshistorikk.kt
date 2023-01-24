package no.nav.familie.ef.sak.uttrekk

import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.YearMonth
import java.util.UUID

data class UttrekkFagsakMedAndelshistorikk(val fagsakId: UUID, val andelshistorikk: List<AndelHistorikkDto>) {

    fun harAndelOgManglerTilsyn(år: Int) =
        andelshistorikk.any { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }

    fun harAvsluttetPeriodeMedManglendeTilsyn(år: Int): Boolean =
        andelshistorikk.filter { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }
            .all { it.andel.periode.tom <= YearMonth.now() }

    fun antallMånederMedManglendeTilsynSomErAvsluttet(år: Int): Long =
        andelshistorikk.filter { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }
            .sumOf { it.andel.periode.lengdeIHeleMåneder() }

    fun beløpForManglendeTilsynSomErAvsluttet(år: Int): Long =
        andelshistorikk.filter { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }
            .sumOf { it.andel.beløp * it.andel.periode.lengdeIHeleMåneder() }
}

private fun Månedsperiode.harPeriodeI(år: Int): Boolean {
    val periodeForÅr = Månedsperiode(fom = YearMonth.of(år, 1), tom = YearMonth.of(år, 12))
    return periodeForÅr overlapper this
}
