package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.erSammenhengende
import no.nav.familie.kontrakter.felles.harOverlappende
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService {

    fun beregnYtelse(
        vedtaksperioder: List<Månedsperiode>,
        inntektsperioder: List<Inntektsperiode>,
    ): List<Beløpsperiode> {
        validerInnteksperioder(inntektsperioder, vedtaksperioder)
        validerVedtaksperioder(vedtaksperioder)

        val beløpForInnteksperioder = inntektsperioder.flatMap {
            BeregningUtils.beregnStønadForInntekt(it)
        }

        return vedtaksperioder.flatMap {
            BeregningUtils.finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder, it)
        }
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<Månedsperiode>) {
        brukerfeilHvis(
            vedtaksperioder.harOverlappende(),
        ) { "Vedtaksperioder $vedtaksperioder overlapper" }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperioder: List<Månedsperiode>) {
        brukerfeilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        brukerfeilHvis(
            inntektsperioder.zipWithNext { a, b -> a.periode < b.periode }.any { !it },
        ) { "Inntektsperioder må være sortert" }

        brukerfeilHvis(
            vedtaksperioder.zipWithNext { a, b -> a < b }.any { !it },
        ) { "Vedtaksperioder må være sortert" }

        brukerfeilHvis(inntektsperioder.first().periode.fom > vedtaksperioder.first().fom) {
            "Inntektsperioder $inntektsperioder begynner etter vedtaksperioder $vedtaksperioder"
        }

        brukerfeilHvis(inntektsperioder.last().periode.tom < vedtaksperioder.last().tom) {
            "Inntektsperioder $inntektsperioder slutter før vedtaksperioder $vedtaksperioder "
        }

        brukerfeilHvis(inntektsperioder.any { it.inntekt < BigDecimal.ZERO }) { "Inntekten kan ikke være negativt" }
        brukerfeilHvis(
            inntektsperioder.any {
                (it.dagsats ?: BigDecimal.ZERO) < BigDecimal.ZERO
            },
        ) { "Dagsats kan ikke være negativt" }
        brukerfeilHvis(
            inntektsperioder.any {
                (it.månedsinntekt ?: BigDecimal.ZERO) < BigDecimal.ZERO
            },
        ) { "Månedsinntekt kan ikke være negativt" }
        brukerfeilHvis(
            inntektsperioder.any {
                it.samordningsfradrag < BigDecimal.ZERO
            },
        ) { "Samordningsfradraget kan ikke være negativt" }

        brukerfeilHvis(
            inntektsperioder.map { it.periode }.harOverlappende() ||
                !inntektsperioder.map { it.periode }.erSammenhengende(),
        ) { "Inntektsperioder $inntektsperioder overlapper eller er ikke sammenhengde" }
    }

    fun hentNyesteGrunnbeløpOgAntallGrunnløpsperioderTilbakeITid(antall: Int): List<Grunnbeløp> {
        return Grunnbeløpsperioder.grunnbeløpsperioder.subList(0, antall)
    }

    fun grunnbeløpsperiodeDTO(gl: Grunnbeløp): GrunnbeløpDTO {
        val periode = gl.periode
        val grunnbeløp = gl.grunnbeløp
        val grunnbeløpMåned = gl.perMnd
        val gjennomsnittPerÅr = gl.gjennomsnittPerÅr
        val seksGangerGrunnbeløp = 6.toBigDecimal() * grunnbeløp
        val seksGangerGrunnbeløpPerMåned = 6.toBigDecimal() * grunnbeløpMåned
        return GrunnbeløpDTO(
            periode = periode,
            grunnbeløp = grunnbeløp,
            grunnbeløpMåned = grunnbeløpMåned,
            gjennomsnittPerÅr = gjennomsnittPerÅr,
            seksGangerGrunnbeløp = seksGangerGrunnbeløp,
            seksGangerGrunnbeløpPerMåned = seksGangerGrunnbeløpPerMåned,
        )
    }

    fun listeMedGrunnbeløpTilDTO(grunnbeløp: List<Grunnbeløp>): List<GrunnbeløpDTO> {
        return grunnbeløp.map { grunnbeløpsperiodeDTO(it) }
    }
}
