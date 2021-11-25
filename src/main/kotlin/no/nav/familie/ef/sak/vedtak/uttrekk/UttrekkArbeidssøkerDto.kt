package no.nav.familie.ef.sak.vedtak.uttrekk

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class UttrekkArbeidssøkereDto(
        val årMåned: YearMonth,
        val antallTotalt: Int,
        val antallKontrollert: Int,
        val arbeidssøkere: List<UttrekkArbeidsssøkerDto>
)

data class UttrekkArbeidsssøkerDto(
        val id: UUID,
        val fagsakId: UUID,
        val behandlingIdForVedtak: UUID,
        val kontrollert: Boolean,
        val tidKontrollert: LocalDateTime?
)

fun UttrekkArbeidssøkere.tilDto() =
        UttrekkArbeidsssøkerDto(id = this.id,
                                fagsakId = this.fagsakId,
                                behandlingIdForVedtak = this.vedtakId,
                                kontrollert = this.kontrollert ?: false,
                                tidKontrollert = tidKontrollert())

private fun UttrekkArbeidssøkere.tidKontrollert(): LocalDateTime? {
    return if (this.kontrollert != null) this.sporbar.endret.endretTid else null
}