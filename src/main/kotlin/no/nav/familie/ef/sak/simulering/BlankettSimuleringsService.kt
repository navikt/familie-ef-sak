package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.tilkjentytelse.tilTilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilPerioder
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
import org.springframework.stereotype.Service


@Service
class BlankettSimuleringsService(val beregningService: BeregningService) {

    fun genererTilkjentYtelseForBlankett(vedtak: VedtakDto?,
                                         saksbehandling: Saksbehandling): TilkjentYtelseMedMetadata {
        val andeler = when (vedtak) {
            is Innvilget -> {
                beregningService.beregnYtelse(vedtak.perioder.tilPerioder(),
                                              vedtak.inntekter.tilInntektsperioder())
                        .map {
                            AndelTilkjentYtelse(beløp = it.beløp.toInt(),
                                                stønadFom = it.periode.fradato,
                                                stønadTom = it.periode.tildato,
                                                kildeBehandlingId = saksbehandling.id,
                                                inntektsreduksjon = it.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0,
                                                inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                                                samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                                                personIdent = saksbehandling.ident)
                        }
            }
            else -> emptyList()
        }


        val tilkjentYtelseForBlankett = TilkjentYtelse(personident = saksbehandling.ident,
                                                       behandlingId = saksbehandling.id,
                                                       andelerTilkjentYtelse = andeler,
                                                       type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING)

        return tilkjentYtelseForBlankett.tilTilkjentYtelseMedMetaData(
                saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                stønadstype = saksbehandling.stønadstype,
                eksternBehandlingId = saksbehandling.eksternId,
                eksternFagsakId = saksbehandling.eksternFagsakId
        )
    }
}