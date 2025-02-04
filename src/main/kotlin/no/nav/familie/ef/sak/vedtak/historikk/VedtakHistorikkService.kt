package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
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
    private val andelsHistorikkService: AndelsHistorikkService,
    private val barnService: BarnService,
    private val behandlingService: BehandlingService,
    private val vedtakService: VedtakService,
) {
    fun hentVedtakFraDato(
        behandlingId: UUID,
        fra: YearMonth,
    ): VedtakDto {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return when (fagsak.stønadstype) {
            StønadType.OVERGANGSSTØNAD -> hentVedtakMedSamordningsfradragType(behandlingId, fagsak, fra)
            StønadType.BARNETILSYN -> hentVedtakForBarnetilsynFraDato(fagsak, behandlingId, fra)
            StønadType.SKOLEPENGER -> error("Støtter ikke henting av skolepenger")
        }
    }

    fun hentVedtakForOvergangsstønadFraDato(
        fagsakId: UUID,
        fra: YearMonth,
    ): InnvilgelseOvergangsstønad = hentVedtakForOvergangsstønadFraDato(fagsakService.hentFagsak(fagsakId), fra)

    /**
     * Slår sammen perioder som er sammenhengende, med samme aktivitet, og samme periodetype, unntatt sanksjoner
     * Slår sammen inntekter som er sammenhengende, med samme inntekt og samordningsfradrag
     */
    private fun hentVedtakForOvergangsstønadFraDato(
        fagsak: Fagsak,
        fra: YearMonth,
    ): InnvilgelseOvergangsstønad {
        val historikk = hentAktivHistorikk(fagsak, StønadType.OVERGANGSSTØNAD)
        return InnvilgelseOvergangsstønad(
            periodeBegrunnelse = null,
            inntektBegrunnelse = null,
            perioder = mapPerioder(historikk, fra),
            inntekter = mapInntekter(historikk, fra),
            samordningsfradragType = null,
        )
    }

    private fun hentVedtakMedSamordningsfradragType(
        behandlingId: UUID,
        fagsak: Fagsak,
        fra: YearMonth,
    ): InnvilgelseOvergangsstønad {
        if (fagsak.stønadstype != StønadType.OVERGANGSSTØNAD) {
            error("Kan kun utlede samordningsfradragstype på vedtak som gjelder overgangsstønad")
        }
        val forrigeBehandlingId = behandlingService.hentBehandling(behandlingId).forrigeBehandlingId
        val forrigeSamordningsfradragType = forrigeBehandlingId?.let { vedtakService.hentVedtak(it).samordningsfradragType }
        return hentVedtakForOvergangsstønadFraDato(fagsak, fra).copy(samordningsfradragType = forrigeSamordningsfradragType)
    }

    private fun hentVedtakForBarnetilsynFraDato(
        fagsak: Fagsak,
        behandlingId: UUID,
        fra: YearMonth,
    ): InnvilgelseBarnetilsyn {
        val historikk = hentAktivHistorikk(fagsak, StønadType.BARNETILSYN)
        val perioder = mapBarnetilsynPerioder(historikk, fra, behandlingId)
        return InnvilgelseBarnetilsyn(
            begrunnelse = null,
            perioder = perioder,
            perioderKontantstøtte = mapUtgifterBarnetilsyn(historikk, fra) { it.kontantstøtte },
            kontantstøtteBegrunnelse = null,
            tilleggsstønad =
                mapUtgifterBarnetilsyn(historikk, fra) { it.tilleggsstønad }.let {
                    TilleggsstønadDto(it, null)
                },
        )
    }

    private fun hentAktivHistorikk(
        fagsak: Fagsak,
        forventetStønadstype: StønadType,
    ): List<AndelHistorikkDto> {
        feilHvis(fagsak.stønadstype != forventetStønadstype) {
            "Kan kun hente data for $forventetStønadstype fra denne, " +
                "men prøvde å hente for ${fagsak.stønadstype} fagsak=${fagsak.id}"
        }
        return hentAktivHistorikk(fagsak.id)
    }

    private fun mapPerioder(
        historikk: List<AndelHistorikkDto>,
        fra: YearMonth,
    ): List<VedtaksperiodeDto> =
        historikk
            .slåSammen { a, b ->
                sammenhengende(a, b) &&
                    (
                        a.vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad &&
                            b.vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad &&
                            a.vedtaksperiode.aktivitet == b.vedtaksperiode.aktivitet &&
                            a.vedtaksperiode.periodeType == b.vedtaksperiode.periodeType
                    )
            }.fraDato(fra)
            .map {
                VedtaksperiodeDto(
                    årMånedFra = it.andel.periode.fom,
                    årMånedTil = it.andel.periode.tom,
                    periode = it.andel.periode,
                    aktivitet = it.aktivitet ?: error("Mangler aktivitet data=$it"),
                    periodeType = it.periodeType ?: error("Mangler periodetype data=$it"),
                    sanksjonsårsak = it.sanksjonsårsak,
                )
            }

    private fun mapInntekter(
        historikk: List<AndelHistorikkDto>,
        fra: YearMonth,
    ): List<Inntekt> =
        historikk
            .filter { it.periodeType != VedtaksperiodeType.SANKSJON }
            .slåSammen { a, b ->
                (
                    a.vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad &&
                        b.vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad &&
                        a.vedtaksperiode.inntekt.dagsats nullOrEquals b.vedtaksperiode.inntekt.dagsats &&
                        a.vedtaksperiode.inntekt.månedsinntekt nullOrEquals b.vedtaksperiode.inntekt.månedsinntekt &&
                        a.vedtaksperiode.inntekt.forventetInntekt nullOrEquals b.vedtaksperiode.inntekt.forventetInntekt &&
                        a.vedtaksperiode.inntekt.samordningsfradrag nullOrEquals b.vedtaksperiode.inntekt.samordningsfradrag
                )
            }.fraDato(fra)
            .mapNotNull {
                // fraDato korter av andelen men ikke vedtaksperiode/inntekt, så riktig dato må brukes
                if (it.vedtaksperiode is VedtakshistorikkperiodeOvergangsstønad) {
                    it.andel.periode.fom to it.vedtaksperiode.inntekt
                } else {
                    null
                }
            }.map { (årMånedFra, inntekt) ->
                Inntekt(
                    årMånedFra = årMånedFra,
                    dagsats = inntekt.dagsats,
                    månedsinntekt = inntekt.månedsinntekt,
                    forventetInntekt = inntekt.forventetInntekt,
                    samordningsfradrag = inntekt.samordningsfradrag,
                )
            }

    private infix fun BigDecimal?.nullOrEquals(other: BigDecimal?) =
        this == null &&
            other == null ||
            (this !== null && other != null && this.compareTo(other) == 0)

    private fun mapUtgifterBarnetilsyn(
        historikk: List<AndelHistorikkDto>,
        fra: YearMonth,
        beløp: (AndelMedGrunnlagDto) -> Int,
    ): List<PeriodeMedBeløpDto> =
        historikk
            .filter { beløp(it.andel) > 0 }
            .slåSammen { a, b ->
                sammenhengende(a, b) &&
                    beløp(a.andel) == beløp(b.andel)
            }.fraDato(fra)
            .map {
                PeriodeMedBeløpDto(
                    årMånedFra = it.andel.periode.fom,
                    årMånedTil = it.andel.periode.tom,
                    periode = it.andel.periode,
                    beløp(it.andel),
                )
            }

    private fun mapBarnetilsynPerioder(
        historikk: List<AndelHistorikkDto>,
        fra: YearMonth,
        behandlingId: UUID,
    ): List<UtgiftsperiodeDto> {
        val barnMap = mapHistoriskeBarn(behandlingId, historikk)
        return historikk
            .slåSammen { a, b ->
                sammenhengende(a, b) &&
                    a.andel.barn.toSet() == b.andel.barn.toSet() &&
                    a.andel.utgifter.compareTo(b.andel.utgifter) == 0 &&
                    a.aktivitetBarnetilsyn == b.aktivitetBarnetilsyn &&
                    a.periodetypeBarnetilsyn == b.periodetypeBarnetilsyn &&
                    a.periodetypeBarnetilsyn != PeriodetypeBarnetilsyn.SANKSJON_1_MND
            }.fraDato(fra)
            .map {
                UtgiftsperiodeDto(
                    årMånedFra = it.andel.periode.fom,
                    årMånedTil = it.andel.periode.tom,
                    periode = it.andel.periode,
                    barn = it.andel.barn.map { barnId -> barnMap[barnId] ?: error("Fant ikke match for barn=$barnId") },
                    utgifter = it.andel.utgifter.toInt(),
                    aktivitetstype = it.aktivitetBarnetilsyn,
                    periodetype = it.periodetypeBarnetilsyn ?: error("Mangler periodetype $it"),
                    sanksjonsårsak = it.sanksjonsårsak,
                )
            }
    }

    private fun mapHistoriskeBarn(
        behandlingId: UUID,
        historikk: List<AndelHistorikkDto>,
    ): Map<UUID, UUID> {
        val historiskeBarnIder = historikk.flatMap { it.andel.barn }.toSet()
        return barnService.kobleBarnForBarnetilsyn(behandlingId, historiskeBarnIder)
    }

    fun hentAktivHistorikk(fagsakId: UUID): List<AndelHistorikkDto> =
        andelsHistorikkService
            .hentHistorikk(fagsakId, null)
            .filter { it.erAktivVedtaksperiode() }
            .sortedBy { it.andel.periode.fom }
}

/**
 * Noter! Denne setter kun fra-dato på andelen, og ikke vedtaksperiode/inntekt
 */
fun List<AndelHistorikkDto>.fraDato(fra: YearMonth): List<AndelHistorikkDto> =
    this.mapNotNull {
        if (it.andel.periode.fom >= fra) {
            it
        } else if (it.andel.periode.tom >= fra) {
            it.copy(andel = it.andel.copy(periode = it.andel.periode.copy(fom = fra)))
        } else {
            null
        }
    }
