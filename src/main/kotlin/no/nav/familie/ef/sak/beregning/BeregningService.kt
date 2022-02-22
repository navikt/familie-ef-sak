package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.felles.util.isEqualOrAfter
import no.nav.familie.ef.sak.felles.util.isEqualOrBefore
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
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
        brukerfeilHvis(vedtaksperioder.zipWithNext { a, b -> a.tildato.isEqualOrAfter(b.fradato) }
                               .any { it }) { "Vedtaksperioder $vedtaksperioder overlapper" }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperioder: List<Periode>) {
        brukerfeilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        brukerfeilHvis(inntektsperioder.zipWithNext { a, b -> a.startDato.isBefore(b.startDato) && a.sluttDato.isBefore(b.sluttDato) }
                               .any { !it }) { "Inntektsperioder må være sortert" }

        brukerfeilHvis(vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.fradato.isAfter(vedtaksperiode.tildato) })
        { "Fravedtaksdato må være etter vedtakstildato" }


        brukerfeilHvis(vedtaksperioder.zipWithNext { a, b -> a.fradato.isBefore(b.fradato) && a.tildato.isBefore(b.tildato) }
                               .any { !it }) { "Vedtaksperioder må være sortert" }

        brukerfeilHvis(!inntektsperioder.first().startDato.isEqualOrBefore(vedtaksperioder.first().fradato)) {
            "Inntektsperioder $inntektsperioder begynner etter vedtaksperioder $vedtaksperioder"
        }

        brukerfeilHvis(!inntektsperioder.last().sluttDato.isEqualOrAfter(vedtaksperioder.last().tildato)) {
            "Inntektsperioder $inntektsperioder slutter før vedtaksperioder $vedtaksperioder "
        }

        brukerfeilHvis(inntektsperioder.any { it.inntekt < BigDecimal.ZERO }) { "Inntekten kan ikke være negativt" }
        brukerfeilHvis(inntektsperioder.any {
            it.samordningsfradrag < BigDecimal.ZERO
        }) { "Samordningsfradraget kan ikke være negativt" }

        brukerfeilHvis(inntektsperioder.zipWithNext { a, b -> a.sluttDato.isEqual(b.startDato.minusDays(1)) }
                               .any { !it }) { "Inntektsperioder $inntektsperioder overlapper eller er ikke sammenhengde" }
    }
}