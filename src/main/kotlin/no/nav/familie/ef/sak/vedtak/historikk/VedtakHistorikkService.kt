package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
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
) {

    /**
     * Slår sammen perioder som er sammenhengende, med samme aktivitet, og samme periodetype, unntatt sanksjoner
     * Slår sammen inntekter som er sammenhengende, med samme inntekt og samordningsfradrag
     */
    fun hentVedtakForOvergangsstønadFraDato(fagsakId: UUID, fra: YearMonth): InnvilgelseOvergangsstønad {
        val stønadstype = fagsakService.hentFagsak(fagsakId).stønadstype
        feilHvis(stønadstype != StønadType.OVERGANGSSTØNAD) {
            "Kan kun hente data for overgangsstønad fra denne, men prøvde å hente for $stønadstype fagsak=$fagsakId"
        }
        val historikk = hentAktivHistorikk(fagsakId)
        return InnvilgelseOvergangsstønad(
            periodeBegrunnelse = null,
            inntektBegrunnelse = null,
            perioder = mapPerioder(historikk, fra),
            inntekter = mapInntekter(historikk, fra),
            samordningsfradragType = null
        )
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

    private fun hentAktivHistorikk(fagsakId: UUID): List<AndelHistorikkDto> {
        return andelsHistorikkService.hentHistorikk(fagsakId, null)
            .filter { it.erIkkeFjernet() }
            .sortedBy { it.andel.periode.fom }
    }

    private fun List<AndelHistorikkDto>.fraDato(fra: YearMonth): List<AndelHistorikkDto> {
        val dato = fra.atDay(1)
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
}
