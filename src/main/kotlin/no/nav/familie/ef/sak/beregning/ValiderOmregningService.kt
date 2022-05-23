package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service

@Service
class ValiderOmregningService(private val vedtakService: VedtakService,
                              private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                              private val beregningService: BeregningService) {

    fun validerHarGammelGOgKanLagres(saksbehandling: Saksbehandling) {
        if (saksbehandling.stønadstype != StønadType.OVERGANGSSTØNAD) return
        if (vedtakService.hentVedtak(saksbehandling.id).resultatType != ResultatType.INNVILGE) return
        if (saksbehandling.forrigeBehandlingId == null) return
        val forrigeTilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(saksbehandling.forrigeBehandlingId) ?: return
        if (forrigeTilkjentYtelse.grunnbeløpsdato >= nyesteGrunnbeløpGyldigFraOgMed) return

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id) ?: return
        if (tilkjentYtelse.grunnbeløpsdato < nyesteGrunnbeløpGyldigFraOgMed) return

        tilkjentYtelse.andelerTilkjentYtelse
                .filter { it.stønadTom > nyesteGrunnbeløpGyldigFraOgMed }
                .forEach { andel ->
                    val inntektsperiodeForAndel = Inntektsperiode(andel.stønadFom,
                                                                  andel.stønadTom,
                                                                  andel.inntekt.toBigDecimal(),
                                                                  andel.samordningsfradrag.toBigDecimal())
                    val beregnetAndel = beregningService.beregnYtelse(listOf(andel.periode), listOf(inntektsperiodeForAndel))
                    brukerfeilHvis(beregnetAndel.size != 1 || beregnetAndel.first().beløp.toInt() != andel.beløp) {
                        feilmeldingForFeilGBeløp(andel)
                    }
                }

    }


    private fun feilmeldingForFeilGBeløp(andel: AndelTilkjentYtelse) =
            "Kan ikke fullføre behandling: Det må revurderes fra " +
            "${maxOf(andel.stønadFom, nyesteGrunnbeløpGyldigFraOgMed)} for at beregning av ny G blir riktig"
}