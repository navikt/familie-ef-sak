package no.nav.familie.ef.sak.vedtak.uttrekk

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class UttrekkArbeidssøkereDto(
        val årMåned: YearMonth,
        val antallTotalt: Int,
        val antallSjekket: Int,
        val arbeidssøkere: List<UttrekkArbeidsssøkerDto>
)

data class UttrekkArbeidsssøkerDto(
        val id: UUID,
        val fagsakId: UUID,
        val behandlingIdForVedtak: UUID,
        val sjekket: Boolean,
        val tidSjekket: LocalDateTime
)

fun UttrekkArbeidssøkere.tilDto() =
        UttrekkArbeidsssøkerDto(id = this.id,
                                fagsakId = this.fagsakId,
                                behandlingIdForVedtak = this.vedtakId,
                                sjekket = this.sjekket,
                                tidSjekket = this.sporbar.endret.endretTid)