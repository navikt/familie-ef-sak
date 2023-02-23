package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.erSammenhengende
import no.nav.familie.kontrakter.felles.harOverlappende
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class BeregningService(
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService
) {

    fun beregnYtelse(vedtaksperioder: List<Månedsperiode>, inntektsperioder: List<Inntektsperiode>): List<Beløpsperiode> {
        validerInnteksperioder(inntektsperioder, vedtaksperioder)
        validerVedtaksperioder(vedtaksperioder)

        val beløpForInnteksperioder = inntektsperioder.flatMap { BeregningUtils.beregnStønadForInntekt(it) }

        return vedtaksperioder.flatMap {
            BeregningUtils.finnStartDatoOgSluttDatoForBeløpsperiode(beløpForInnteksperioder, it)
        }
    }

    private fun validerVedtaksperioder(vedtaksperioder: List<Månedsperiode>) {
        brukerfeilHvis(
            vedtaksperioder.harOverlappende()
        ) { "Vedtaksperioder $vedtaksperioder overlapper" }
    }

    private fun validerInnteksperioder(inntektsperioder: List<Inntektsperiode>, vedtaksperioder: List<Månedsperiode>) {
        brukerfeilHvis(inntektsperioder.isEmpty()) {
            "Inntektsperioder kan ikke være tom liste"
        }

        brukerfeilHvis(
            inntektsperioder.zipWithNext { a, b -> a.periode < b.periode }.any { !it }
        ) { "Inntektsperioder må være sortert" }

        brukerfeilHvis(
            vedtaksperioder.zipWithNext { a, b -> a < b }.any { !it }
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
                it.samordningsfradrag < BigDecimal.ZERO
            }
        ) { "Samordningsfradraget kan ikke være negativt" }

        brukerfeilHvis(
            inntektsperioder.map { it.periode }.harOverlappende() ||
                !inntektsperioder.map { it.periode }.erSammenhengende()
        ) { "Inntektsperioder $inntektsperioder overlapper eller er ikke sammenhengde" }
    }

    fun hentSisteInntektFraForrigeIverksatteBehandling(behandlingId: UUID): BigDecimal {
        val fagsakId = behandlingService.hentBehandling(behandlingId).fagsakId
        val forrigeIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
            ?: throw Feil("Finnes ikke en tidligere vedtak med beløpsperioder for fagsak med id=$fagsakId")

        val vedtakForBehandling = vedtakService.hentVedtak(forrigeIverksatteBehandling.id)

        return vedtakForBehandling.inntekter?.inntekter?.last()?.inntekt ?: throw Feil("Finner ingen inntak på behandling med id=${vedtakForBehandling.behandlingId}")
    }
}
