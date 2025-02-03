package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vedtak.historikk.fraDato
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class KopierVedtakService(
    val barnRepository: BarnRepository,
    val vedtakService: VedtakService,
    val vedtakHistorikkService: VedtakHistorikkService,
    val behandlingService: BehandlingService,
) {
    fun lagVedtakDtoBasertPåTidligereVedtaksperioder(
        fagsakId: UUID,
        forrigeBehandlingId: UUID,
        revurderingId: UUID,
    ): VedtakDto {
        validerRevurderingErSatsendring(revurderingId)
        val behandlingBarn = barnRepository.findByBehandlingId(revurderingId)
        return mapTilBarnetilsynVedtak(fagsakId, behandlingBarn, forrigeBehandlingId)
    }

    private fun validerRevurderingErSatsendring(revurderingId: UUID) {
        val revurdering = behandlingService.hentBehandling(revurderingId)
        feilHvis(revurdering.årsak != BehandlingÅrsak.SATSENDRING) { "Kan bare kopiere vedtak hvis behandlingsÅrsak er satsendring" }
    }

    fun mapTilBarnetilsynVedtak(
        fagsakId: UUID,
        behandlingBarn: List<BehandlingBarn>,
        forrigeBehandlingId: UUID,
    ): VedtakDto {
        val fraDato = BeregningBarnetilsynUtil.satserForBarnetilsyn.maxOf { it.periode.fom }
        val historikk = vedtakHistorikkService.hentAktivHistorikk(fagsakId).fraDato(YearMonth.from(fraDato))
        val forrigeVedtak = vedtakService.hentVedtak(forrigeBehandlingId)

        return InnvilgelseBarnetilsyn(
            perioder = mapUtgiftsperioder(historikk, behandlingBarn),
            resultatType = ResultatType.INNVILGE,
            perioderKontantstøtte = mapPerioderKontantstøtte(historikk),
            kontantstøtteBegrunnelse = forrigeVedtak.kontantstøtte?.begrunnelse,
            tilleggsstønad = mapTilleggsstønadDto(historikk, forrigeVedtak.tilleggsstønad?.begrunnelse),
            begrunnelse = "Satsendring barnetilsyn",
        )
    }

    private fun mapTilleggsstønadDto(
        historikk: List<AndelHistorikkDto>,
        begrunnelse: String?,
    ): TilleggsstønadDto =
        TilleggsstønadDto(
            historikk.any { it.andel.tilleggsstønad > 0 },
            historikk.filter { it.andel.tilleggsstønad > 0 }.map {
                PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.tilleggsstønad)
            },
            begrunnelse,
        )

    private fun mapPerioderKontantstøtte(historikk: List<AndelHistorikkDto>): List<PeriodeMedBeløpDto> =
        historikk
            .filter { kontantstøtte -> kontantstøtte.andel.kontantstøtte > 0 }
            .map {
                PeriodeMedBeløpDto(
                    periode = it.andel.periode,
                    beløp = it.andel.kontantstøtte,
                )
            }

    private fun mapUtgiftsperioder(
        historikk: List<AndelHistorikkDto>,
        behandlingBarn: List<BehandlingBarn>,
    ): List<UtgiftsperiodeDto> {
        val map =
            historikk.map {
                feilHvis(it.erSanksjon) {
                    "Støtter ikke sanksjon. Både erMidlertidigOpphør og sanksjonsårsak burde då settes"
                }
                UtgiftsperiodeDto(
                    årMånedFra = it.andel.periode.fom,
                    årMånedTil = it.andel.periode.tom,
                    periode = it.andel.periode,
                    barn = finnBehandlingBarnIdsGittTidligereAndelBarn(it.andel.barn, behandlingBarn),
                    utgifter = it.andel.utgifter.toInt(),
                    sanksjonsårsak = null,
                    periodetype = it.periodetypeBarnetilsyn ?: error("Mangler periodetype $it"),
                    aktivitetstype = it.aktivitetBarnetilsyn,
                )
            }
        return map.fyllUtPerioderUtenStønad()
    }

    private fun finnBehandlingBarnIdsGittTidligereAndelBarn(
        andelBarn: List<UUID>,
        behandlingBarn: List<BehandlingBarn>,
    ): List<UUID> {
        val tidligereValgteAndelBarn = barnRepository.findAllById(andelBarn).map { it.personIdent }
        return behandlingBarn.filter { it.personIdent in tidligereValgteAndelBarn }.map { it.id }
    }
}

private fun Månedsperiode.ikkePåfølgesAv(periode: Månedsperiode): Boolean = !this.påfølgesAv(periode)

private fun List<UtgiftsperiodeDto>.fyllUtPerioderUtenStønad(): List<UtgiftsperiodeDto> {
    val perioderUtenStønad = mutableListOf<UtgiftsperiodeDto>()

    this.sortedBy { it.periode.fom }.zipWithNext { denne, neste ->
        if (denne.periode.ikkePåfølgesAv(neste.periode)) {
            perioderUtenStønad.add(
                UtgiftsperiodeDto(
                    årMånedFra = denne.periode.tom.plusMonths(1),
                    årMånedTil = neste.periode.fom.minusMonths(1),
                    periode = Månedsperiode(fom = denne.periode.tom.plusMonths(1), tom = neste.periode.fom.minusMonths(1)),
                    barn = emptyList(),
                    utgifter = 0,
                    sanksjonsårsak = null,
                    periodetype = PeriodetypeBarnetilsyn.OPPHØR,
                    aktivitetstype = null,
                ),
            )
        }
    }

    return (this + perioderUtenStønad).sortedBy { it.periode.fom }
}
