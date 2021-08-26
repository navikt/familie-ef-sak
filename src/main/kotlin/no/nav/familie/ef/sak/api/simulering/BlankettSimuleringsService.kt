package no.nav.familie.ef.sak.api.simulering

import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.api.beregning.tilPerioder
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import org.springframework.stereotype.Service


@Service
class BlankettSimuleringsService(val beregningService: BeregningService) {

    fun genererTilkjentYtelseForBlankett(vedtak: VedtakDto?,
                                                 behandling: Behandling,
                                                 fagsak: Fagsak): TilkjentYtelseMedMetadata {
        val andeler = when (vedtak) {
            is Innvilget -> {
                beregningService.beregnYtelse(vedtak.perioder.tilPerioder(),
                                              vedtak.inntekter.tilInntektsperioder())
                        .map {
                            AndelTilkjentYtelse(beløp = it.beløp.toInt(),
                                                stønadFom = it.periode.fradato,
                                                stønadTom = it.periode.tildato,
                                                kildeBehandlingId = behandling.id,
                                                inntektsreduksjon = it.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0,
                                                inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                                                samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                                                personIdent = fagsak.hentAktivIdent())
                        }
            }
            else -> emptyList()
        }


        val tilkjentYtelseForBlankett = TilkjentYtelse(
                personident = fagsak.hentAktivIdent(),
                behandlingId = behandling.id,
                andelerTilkjentYtelse = andeler,
                type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING)

        return tilkjentYtelseForBlankett
                .tilIverksettMedMetaData(
                        saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                        stønadstype = fagsak.stønadstype,
                        eksternBehandlingId = behandling.eksternId.id,
                        eksternFagsakId = fagsak.eksternId.id
                )
    }
}