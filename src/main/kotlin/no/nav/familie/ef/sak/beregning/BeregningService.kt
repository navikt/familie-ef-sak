package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.kontrakter.felles.Periode
import no.nav.familie.kontrakter.felles.erSammenhengende
import no.nav.familie.kontrakter.felles.harOverlappende
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class BeregningService {

    fun beregnYtelse(vedtaksperioder: List<Periode>, inntektsperioder: List<Inntektsperiode>): List<Beløpsperiode> {

        validerInnteksperioder(inntektsperioder, vedtaksperioder)
        validerVedtaksperioder(vedtaksperioder)

        val beløpForInnteksperioder = inntektsperioder.flatMap { BeregningUtils.beregnStønadForInntekt(it) }

        return vedtaksperioder.flatMap {
            BeregningUtils.finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder, it)
        }
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<Periode>) {
        brukerfeilHvis(
            vedtaksperioder.harOverlappende()
        ) { "Vedtaksperioder $vedtaksperioder overlapper" }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperioder: List<Periode>) {
        brukerfeilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        brukerfeilHvis(
            inntektsperioder.zipWithNext { a, b -> a.periode < b.periode }.any { !it }
        ) { "Inntektsperioder må være sortert" }

        brukerfeilHvis(
            vedtaksperioder.zipWithNext { a, b -> a < b }.any { !it }
        ) { "Vedtaksperioder må være sortert" }

        brukerfeilHvis(inntektsperioder.first().periode.fomDato > vedtaksperioder.first().fomDato) {
            "Inntektsperioder $inntektsperioder begynner etter vedtaksperioder $vedtaksperioder"
        }

        brukerfeilHvis(inntektsperioder.last().periode.tomDato < vedtaksperioder.last().tomDato) {
            "Inntektsperioder $inntektsperioder slutter før vedtaksperioder $vedtaksperioder "
        }

        brukerfeilHvis(inntektsperioder.any { it.inntekt < BigDecimal.ZERO }) { "Inntekten kan ikke være negativt" }
        brukerfeilHvis(
            inntektsperioder.any {
                it.samordningsfradrag < BigDecimal.ZERO
            }
        ) { "Samordningsfradraget kan ikke være negativt" }

        brukerfeilHvis(
            inntektsperioder.map { it.periode }.harOverlappende() ||
                !inntektsperioder.map { it.periode }.erSammenhengende()
        ) { "Inntektsperioder $inntektsperioder overlapper eller er ikke sammenhengde" }
    }
}
