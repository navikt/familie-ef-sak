package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class ValiderOmregningService(
    private val vedtakService: VedtakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val beregningService: BeregningService,
    private val vedtakHistorikkService: VedtakHistorikkService,
    private val featureToggleService: FeatureToggleService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun validerHarSammePerioderSomTidligereVedtak(
        data: InnvilgelseOvergangsstønad,
        saksbehandling: Saksbehandling
    ) {
        if (!saksbehandling.erOmregning || saksbehandling.erMaskinellOmregning) {
            return
        }
        if (featureToggleService.isEnabled(Toggle.G_OMREGNING_REVURDER_HOPP_OVER_VALIDER_TIDLIGERE_VEDTAK)) {
            logger.warn("Skipper validering av tidligere perioder vid g-omregning for behandling=${saksbehandling.id}")
            return
        }
        val tidligerePerioder = hentVedtakshistorikkFraNyesteGrunnbeløp(saksbehandling)
        validerHarSammePerioderSomTidligereVedtak(data, tidligerePerioder)
    }

    private fun validerHarSammePerioderSomTidligereVedtak(
        data: InnvilgelseOvergangsstønad,
        tidligerePerioder: Map<YearMonth, VedtaksperiodeDto>
    ) {
        brukerfeilHvis(tidligerePerioder.isEmpty()) {
            "Denne skal ikke g-omregnes då den ikke har noen tidligere perioder som er etter $nyesteGrunnbeløpGyldigFraOgMed"
        }
        brukerfeilHvis(data.perioder.size != tidligerePerioder.size) {
            val tidligereDatoer = tidligerePerioder.values.joinToString(", ") { "${it.årMånedFra}-${it.årMånedTil}" }
            "Antall vedtaksperioder er ulikt fra tidligere vedtak, tidligerePerioder=$tidligereDatoer"
        }
        data.perioder.forEach {
            val fra = it.årMånedFra
            val tidligerePeriode = tidligerePerioder[fra]
                ?: throw ApiFeil("Finner ikke periode fra $fra", HttpStatus.BAD_REQUEST)
            brukerfeilHvis(tidligerePeriode.årMånedTil != it.årMånedTil) {
                "Perioden fra $fra har annet tom-dato(${it.årMånedTil} enn " +
                    "tidligere periode (${tidligerePeriode.årMånedTil})"
            }
            brukerfeilHvis(
                tidligerePeriode.aktivitet != AktivitetType.MIGRERING &&
                    tidligerePeriode.aktivitet != it.aktivitet
            ) {
                "Perioden fra $fra har annen aktivitet(${it.aktivitet} enn tidligere periode (${tidligerePeriode.aktivitet})"
            }
            brukerfeilHvis(
                tidligerePeriode.periodeType != VedtaksperiodeType.MIGRERING &&
                    tidligerePeriode.periodeType != it.periodeType
            ) {
                "Perioden fra $fra har annen type periode (${it.periodeType} enn " +
                    "tidligere periode (${tidligerePeriode.periodeType})"
            }
        }
    }

    private fun hentVedtakshistorikkFraNyesteGrunnbeløp(saksbehandling: Saksbehandling) =
        vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(
            saksbehandling.fagsakId,
            YearMonth.from(nyesteGrunnbeløpGyldigFraOgMed)
        )
            .perioder
            .filter { it.periodeType != VedtaksperiodeType.SANKSJON }
            .associateBy { it.årMånedFra }

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
                val inntektsperiodeForAndel = Inntektsperiode(
                    andel.stønadFom,
                    andel.stønadTom,
                    andel.inntekt.toBigDecimal(),
                    andel.samordningsfradrag.toBigDecimal()
                )
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
