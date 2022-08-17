package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vilkår.VurderingService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AndelsHistorikkService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vedtakService: VedtakService,
    private val vurderingService: VurderingService,
    private val barnService: BarnService
) {

    fun hentHistorikk(fagsakId: UUID, tilOgMedBehandlingId: UUID?): List<AndelHistorikkDto> {
        val tilkjenteYtelser = tilkjentYtelseRepository.finnAlleIverksatteForFagsak(fagsakId)
        if (tilkjenteYtelser.isEmpty()) {
            return emptyList()
        }

        val behandlingIder = tilkjenteYtelser.map { it.behandlingId }.toSet()
        val vedtakForBehandlinger = vedtakService.hentVedtakForBehandlinger(behandlingIder)
        val behandlinger = behandlingService.hentBehandlinger(behandlingIder)
        // hent vilkår for viss type hvor behandlingIder sendes inn
        val aktivitetArbeid = vurderingService.aktivitetArbeidForBehandlingIds(behandlingIder)
        return AndelHistorikkBeregner.lagHistorikk(
            tilkjenteYtelser,
            vedtakForBehandlinger,
            behandlinger,
            tilOgMedBehandlingId,
            aktivitetArbeid
        )
    }

    fun utledLøpendeUtbetalingForBarnIBarnetilsyn(behandlingId: UUID): BarnMedLøpendeStønad {
        val behandling = behandlingService.hentBehandling(behandlingId)

        return behandling.forrigeBehandlingId?.let {

            val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
            val barnPåBehandling = barnService.finnBarnPåBehandling(behandlingId)
            val vedtaksdatoEllerDagensdato =
                finnDatoForKalkuleringAvLøpendeStønadPåBehandling(behandling)

            val barnIdForAlleAktuelleBehandlinger = hentHistorikk(fagsak.id, behandling.forrigeBehandlingId)
                .filter { it.endring?.type != EndringType.FJERNET }
                .filter { it.endring?.type != EndringType.ERSTATTET }
                .filter { it.andel.beløp > 0 && it.andel.periode.toDatoperiode().inneholder(vedtaksdatoEllerDagensdato) }
                .map { it.andel.barn }
                .flatten()
            val behandlingsbarn = barnService.hentBehandlingBarnForBarnIder(barnIdForAlleAktuelleBehandlinger)
            val barnMedLøpendeStønad =
                barnPåBehandling.filter { barnetViSerPå -> behandlingsbarn.any { it.personIdent == barnetViSerPå.personIdent } }
                    .map { it.id }
            return BarnMedLøpendeStønad(barn = barnMedLøpendeStønad, dato = vedtaksdatoEllerDagensdato)
        } ?: BarnMedLøpendeStønad(barn = emptyList(), dato = LocalDate.now())
    }

    private fun finnDatoForKalkuleringAvLøpendeStønadPåBehandling(behandling: Behandling) =
        vedtakService.hentVedtakHvisEksisterer(behandling.id)
            ?.let {
                tilkjentYtelseRepository.findByBehandlingId(behandling.id)?.vedtakstidspunkt?.toLocalDate()
                    ?: behandling.sporbar.opprettetTid.toLocalDate()
            }
            ?: LocalDate.now()
}
