package no.nav.familie.ef.sak.forvaltning.uttrekk

import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import java.time.YearMonth
import java.util.UUID

data class UttrekkFagsakMedAndelshistorikk(
    val fagsakId: UUID,
    val andelshistorikk: List<AndelHistorikkDto>,
) {
    fun harAndelOgManglerTilsyn(år: Int) = andelshistorikk.any { it.andel.periode.harPeriodeI(år) && it.aktivitet?.manglerTilsyn() ?: false }

    fun harAvsluttetPeriodeMedManglendeTilsyn(): Boolean =
        andelshistorikk
            .filter { it.aktivitet?.manglerTilsyn() ?: false }
            .all { it.andel.periode.tom <= YearMonth.now() }

    fun antallMånederMedManglendeTilsynSomErAvsluttet(): Long =
        andelshistorikk
            .filter { it.aktivitet?.manglerTilsyn() ?: false }
            .sumOf { it.andel.periode.lengdeIHeleMåneder() }

    fun beløpForManglendeTilsynSomErAvsluttet(): Long =
        andelshistorikk
            .filter { it.aktivitet?.manglerTilsyn() ?: false }
            .sumOf { it.andel.beløp * it.andel.periode.lengdeIHeleMåneder() }

    fun tidligsteFom(): YearMonth {
        val andeler = andelshistorikk.filter { it.aktivitet?.manglerTilsyn() ?: false }
        return andeler
            .sortedBy { it.andel.periode.fom }
            .first()
            .andel.periode.fom
    }
}

private fun Månedsperiode.harPeriodeI(år: Int): Boolean {
    val periodeForÅr = Månedsperiode(fom = YearMonth.of(år, 1), tom = YearMonth.of(år, 12))
    return periodeForÅr overlapper this
}
