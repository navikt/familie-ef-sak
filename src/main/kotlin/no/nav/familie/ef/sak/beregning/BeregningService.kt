package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.util.Periode
import no.nav.familie.ef.sak.util.isEqualOrAfter
import no.nav.familie.ef.sak.util.isEqualOrBefore
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
        feilHvis(vedtaksperioder.zipWithNext { a, b -> a.tildato.isEqualOrAfter(b.fradato) }
                         .any { it }) { "Vedtaksperioder ${vedtaksperioder} overlapper" }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperioder: List<Periode>) {
        feilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        feilHvis(inntektsperioder.zipWithNext { a, b -> a.startDato.isBefore(b.startDato) && a.sluttDato.isBefore(b.sluttDato) }
                         .any { !it }) { "Inntektsperioder må være sortert" }

        feilHvis(vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.fradato.isAfter(vedtaksperiode.tildato)})
        {"Fravedtaksdato må være etter vedtakstildato"}


        feilHvis(vedtaksperioder.zipWithNext { a, b -> a.fradato.isBefore(b.fradato) && a.tildato.isBefore(b.tildato) }
                         .any { !it }) { "Vedtaksperioder må være sortert" }

        feilHvis(!inntektsperioder.first().startDato.isEqualOrBefore(vedtaksperioder.first().fradato)) {
            "Inntektsperioder $inntektsperioder begynner etter vedtaksperioder $vedtaksperioder"
        }

        feilHvis(!inntektsperioder.last().sluttDato.isEqualOrAfter(vedtaksperioder.last().tildato)) {
            "Inntektsperioder $inntektsperioder slutter før vedtaksperioder $vedtaksperioder "
        }

        feilHvis(inntektsperioder.any { it.inntekt < BigDecimal.ZERO }) { "Inntekten kan ikke være negativt" }
        feilHvis(inntektsperioder.any { it.samordningsfradrag < BigDecimal.ZERO }) { "Samordningsfradraget kan ikke være negativt" }

        feilHvis(inntektsperioder.zipWithNext { a, b -> a.sluttDato.isEqual(b.startDato.minusDays(1)) }
                         .any { !it }) { "Inntektsperioder ${inntektsperioder} overlapper eller er ikke sammenhengde" }
    }
}