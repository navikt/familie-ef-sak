package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.felles.util.harPåfølgendeMåned
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class VedtakHistorikkService(
        private val tilkjentYtelseService: TilkjentYtelseService,
) {

    fun hentVedtakForOvergangsstønadFraDato(fagsakId: UUID, fra: YearMonth): InnvilgelseOvergangsstønad {
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
                    VedtaksperiodeDto(YearMonth.from(it.andel.stønadFra),
                                      YearMonth.from(it.andel.stønadTil),
                                      it.aktivitet ?: error("Mangler aktivitet data=$it"),
                                      it.periodeType ?: error("Mangler periodetype data=$it"))
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
                    Inntekt(YearMonth.from(it.andel.stønadFra),
                            BigDecimal(it.andel.inntekt),
                            BigDecimal(it.andel.samordningsfradrag))
                }
    }

    private fun hentAktivHistorikk(fagsakId: UUID): List<AndelHistorikkDto> {
        return tilkjentYtelseService.hentHistorikk(fagsakId, null)
                .filter { it.erIkkeFjernet() }
                .sortedBy { it.andel.stønadFra }
    }

    private fun List<AndelHistorikkDto>.fraDato(fra: YearMonth): List<AndelHistorikkDto> {
        val dato = fra.atDay(1)
        return this.mapNotNull {
            if (it.andel.stønadFra >= dato) {
                it
            } else if (it.andel.stønadTil > dato) {
                it.copy(andel = it.andel.copy(stønadFra = dato))
            } else {
                null
            }
        }
    }

    private fun List<AndelHistorikkDto>.slåSammen(harSammeVerdi: (AndelHistorikkDto, AndelHistorikkDto) -> Boolean): List<AndelHistorikkDto> {
        return this.fold(mutableListOf()) { acc, entry ->
            val last = acc.lastOrNull()
            if (last != null && harSammeVerdi(last, entry)) {
                acc.removeLast()
                acc.add(last.copy(andel = last.andel.copy(stønadTil = entry.andel.stønadTil)))
            } else {
                acc.add(entry)
            }
            acc
        }
    }

    private fun sammenhengende(first: AndelHistorikkDto,
                               second: AndelHistorikkDto) =
            first.andel.stønadTil.harPåfølgendeMåned(second.andel.stønadFra)

}