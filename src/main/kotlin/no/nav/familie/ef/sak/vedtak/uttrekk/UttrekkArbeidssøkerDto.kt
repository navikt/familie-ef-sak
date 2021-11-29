package no.nav.familie.ef.sak.vedtak.uttrekk

import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

/**
 * @param antallTotalt er antallet for selve queryn(visKontrollerte), så hvis man har 2 kontrollerte og 3 som ikke er,
 * så blir [antallTotalt] 3 hvis visKontrollerte=false, og 5 hvis true
 * @param antallManglerTilgang er kun antallet for denne side, den gir ikke antall som mangler totalt
 */
data class UttrekkArbeidssøkereDto(
        val årMåned: YearMonth,
        val antallTotalt: Int,
        val antallKontrollert: Int,
        val arbeidssøkere: List<UttrekkArbeidsssøkerDto>,
        val antallManglerTilgang: Int
)

data class UttrekkArbeidsssøkerDto(
        val id: UUID,
        val fagsakId: UUID,
        val behandlingIdForVedtak: UUID,
        val personIdent: String,
        val navn: String,
        val kontrollert: Boolean,
        val kontrollertTid: LocalDateTime?,
        val kontrollertAv: String?
)

fun UttrekkArbeidssøkere.tilDto(personIdent: String, navn: String) =
        UttrekkArbeidsssøkerDto(id = this.id,
                                fagsakId = this.fagsakId,
                                behandlingIdForVedtak = this.vedtakId,
                                personIdent = personIdent,
                                navn = navn,
                                kontrollert = this.kontrollert,
                                kontrollertTid = this.kontrollertTid,
                                kontrollertAv = this.kontrollertAv)
