package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkUtil.sammenhengende
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkUtil.slåSammen
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class VedtakHistorikkService(
    private val fagsakService: FagsakService,
    private val andelsHistorikkService: AndelsHistorikkService
) {

    fun hentVedtakFraDato(fagsakId: UUID, fra: YearMonth): VedtakDto {
        val stønadstype = fagsakService.hentFagsak(fagsakId).stønadstype
        return when (stønadstype) {
            StønadType.OVERGANGSSTØNAD -> hentVedtakForOvergangsstønadFraDato(fagsakId, fra)
            StønadType.BARNETILSYN -> hentVedtakForBarnetilsynFraDato(fagsakId, fra)
            StønadType.SKOLEPENGER -> error("Støtter ikke henting av skolepenger ")
        }
    }

    fun hentVedtakForOvergangsstønadFraDato(fagsakId: UUID, fra: YearMonth): InnvilgelseOvergangsstønad {
        return hentVedtakForOvergangsstønadFraDato(fagsakService.hentFagsak(fagsakId), fra)
    }

    /**
     * Slår sammen perioder som er sammenhengende, med samme aktivitet, og samme periodetype, unntatt sanksjoner
     * Slår sammen inntekter som er sammenhengende, med samme inntekt og samordningsfradrag
     */
    fun hentVedtakForOvergangsstønadFraDato(fagsak: Fagsak, fra: YearMonth): InnvilgelseOvergangsstønad {
        val historikk = hentAktivHistorikk(fagsak, StønadType.OVERGANGSSTØNAD)
        return InnvilgelseOvergangsstønad(
            periodeBegrunnelse = null,
            inntektBegrunnelse = null,
            perioder = mapPerioder(historikk, fra),
            inntekter = mapInntekter(historikk, fra),
            samordningsfradragType = null
        )
    }

    fun hentVedtakForBarnetilsynFraDato(fagsakId: UUID, fra: YearMonth): InnvilgelseBarnetilsyn {
        return hentVedtakForBarnetilsynFraDato(fagsakService.hentFagsak(fagsakId), fra)
    }

    fun hentVedtakForBarnetilsynFraDato(fagsak: Fagsak, fra: YearMonth): InnvilgelseBarnetilsyn {
        val historikk = hentAktivHistorikk(fagsak, StønadType.BARNETILSYN)
        val perioder = mapBarnetilsynPerioder(historikk, fra)
        // TODO må mappe barn til siste behandlingen sine barn id'er
        return InnvilgelseBarnetilsyn(
            begrunnelse = null,
            perioder = perioder,
            perioderKontantstøtte = mapUtgifterBarnetilsyn(historikk, fra) { it.kontantstøtte },
            tilleggsstønad = mapUtgifterBarnetilsyn(historikk, fra) { it.tilleggsstønad }.let {
                TilleggsstønadDto(harTilleggsstønad = it.isNotEmpty(), it, null)
            },
        )
    }

    private fun hentAktivHistorikk(fagsak: Fagsak, forventetStønadstype: StønadType): List<AndelHistorikkDto> {
        feilHvis(fagsak.stønadstype != forventetStønadstype) {
            "Kan kun hente data for $forventetStønadstype fra denne, " +
                "men prøvde å hente for ${fagsak.stønadstype} fagsak=${fagsak.id}"
        }
        return hentAktivHistorikk(fagsak.id)
    }

    private fun mapUtgifterBarnetilsyn(
        historikk: List<AndelHistorikkDto>,
        fra: YearMonth,
        utgift: (AndelMedGrunnlagDto) -> Int
    ): List<PeriodeMedBeløpDto> {
        return historikk
            .filter { utgift(it.andel) > 0 } // riktig?
            .slåSammen { a, b ->
                sammenhengende(a, b) &&
                    utgift(a.andel) == utgift(b.andel)
            }
            .fraDato(fra)
            .map {
                PeriodeMedBeløpDto(
                    årMånedFra = it.andel.periode.fom,
                    årMånedTil = it.andel.periode.tom,
                    periode = it.andel.periode,
                    utgift(it.andel)
                )
            }
    }

    fun mapBarnetilsynPerioder(historikk: List<AndelHistorikkDto>, fra: YearMonth): List<UtgiftsperiodeDto> {
        return historikk
            .slåSammen { a, b ->
                sammenhengende(a, b) &&
                    a.andel.barn == b.andel.barn &&
                    a.andel.utgifter.compareTo(b.andel.utgifter) == 0
                // midlertidlig opphør?
            }
            .fraDato(fra)
            .map {
                UtgiftsperiodeDto(
                    årMånedFra = it.andel.periode.fom,
                    årMånedTil = it.andel.periode.tom,
                    periode = it.andel.periode,
                    barn = it.andel.barn,
                    utgifter = it.andel.utgifter.toInt(),
                    erMidlertidigOpphør = false
                )
            }
    }

    private fun mapPerioder(historikk: List<AndelHistorikkDto>, fra: YearMonth): List<VedtaksperiodeDto> {
        return historikk
            .slåSammen { a, b ->
                sammenhengende(a, b) &&
                    a.aktivitet == b.aktivitet &&
                    a.periodeType == b.periodeType &&
                    a.periodeType != VedtaksperiodeType.SANKSJON
            }
            .fraDato(fra)
            .map {
                VedtaksperiodeDto(
                    it.andel.periode.fom,
                    it.andel.periode.tom,
                    it.andel.periode,
                    it.aktivitet ?: error("Mangler aktivitet data=$it"),
                    it.periodeType ?: error("Mangler periodetype data=$it")
                )
            }
    }

    private fun mapInntekter(historikk: List<AndelHistorikkDto>, fra: YearMonth): List<Inntekt> {
        return historikk
            .filter { it.periodeType != VedtaksperiodeType.SANKSJON }
            .slåSammen { a, b ->
                a.andel.inntekt == b.andel.inntekt &&
                    a.andel.samordningsfradrag == b.andel.samordningsfradrag
            }
            .fraDato(fra)
            .map {
                Inntekt(
                    it.andel.periode.fom,
                    BigDecimal(it.andel.inntekt),
                    BigDecimal(it.andel.samordningsfradrag)
                )
            }
    }

    fun hentAktivHistorikk(fagsakId: UUID): List<AndelHistorikkDto> {
        return andelsHistorikkService.hentHistorikk(fagsakId, null)
            .filter { it.erIkkeFjernet() }
            .sortedBy { it.andel.periode.fom }
    }
}

fun List<AndelHistorikkDto>.fraDato(fra: YearMonth): List<AndelHistorikkDto> {
    return this.mapNotNull {
        if (it.andel.periode.fom >= fra) {
            it
        } else if (it.andel.periode.tom >= fra) {
            it.copy(andel = it.andel.copy(periode = it.andel.periode.copy(fom = fra)))
        } else {
            null
        }
    }
}
