package no.nav.familie.ef.sak.tilkjentytelse

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggle
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkKonfigurasjon
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class AndelsHistorikkService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val vedtakService: VedtakService,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val barnService: BarnService,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentHistorikk(
        fagsakId: UUID,
        tilOgMedBehandlingId: UUID?,
    ): List<AndelHistorikkDto> {
        val tilkjenteYtelser = tilkjentYtelseRepository.finnAlleIverksatteForFagsak(fagsakId)
        if (tilkjenteYtelser.isEmpty()) {
            return emptyList()
        }
        val stønadstype = fagsakService.hentFagsak(fagsakId).stønadstype
        val behandlingIder = tilkjenteYtelser.map { it.behandlingId }.toSet()
        val vedtakForBehandlinger = vedtakService.hentVedtakForBehandlinger(behandlingIder)
        val behandlinger = behandlingService.hentBehandlinger(behandlingIder)
        // hent vilkår for viss type hvor behandlingIder sendes inn
        val aktivitetArbeid = aktivitetArbeidForBehandlingIds(behandlingIder)
        return AndelHistorikkBeregner.lagHistorikk(
            stønadstype,
            tilkjenteYtelser,
            vedtakForBehandlinger,
            behandlinger,
            tilOgMedBehandlingId,
            aktivitetArbeid,
            HistorikkKonfigurasjon(
                brukIkkeVedtatteSatser = featureToggleService.isEnabled(FeatureToggle.SatsendringBrukIkkeVedtattMaxsats),
            ),
        )
    }

    fun aktivitetArbeidForBehandlingIds(behandlingIds: Collection<UUID>): Map<UUID, SvarId?> {
        val vilkårsvurderinger =
            vilkårsvurderingRepository.findByTypeAndBehandlingIdIn(VilkårType.AKTIVITET_ARBEID, behandlingIds)

        return vilkårsvurderinger.associate { vilkårsvurdering ->
            val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger

            vilkårsvurdering.behandlingId to
                delvilkårsvurderinger
                    .map { delvilkårsvurdering ->
                        delvilkårsvurdering.vurderinger.single { it.regelId == RegelId.ER_I_ARBEID_ELLER_FORBIGÅENDE_SYKDOM }.svar
                    }.single()
        }
    }

    fun utledLøpendeUtbetalingForBarnIBarnetilsyn(behandlingId: UUID): BarnMedLøpendeStønad {
        val behandling = behandlingService.hentBehandling(behandlingId)

        return behandling.forrigeBehandlingId?.let {
            val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
            val barnPåBehandling = barnService.finnBarnPåBehandling(behandlingId)
            val vedtaksdatoEllerDagensdato = behandling.vedtakstidspunkt?.toLocalDate() ?: LocalDate.now()

            val barnIdForAlleAktuelleBehandlinger =
                hentHistorikk(fagsak.id, behandling.forrigeBehandlingId)
                    .filter { it.endring?.type != EndringType.FJERNET }
                    .filter { it.endring?.type != EndringType.ERSTATTET }
                    .filter {
                        it.andel.beløp > 0 &&
                            it.andel.periode
                                .toDatoperiode()
                                .inneholder(vedtaksdatoEllerDagensdato)
                    }.map { it.andel.barn }
                    .flatten()
            val behandlingsbarn = barnService.hentBehandlingBarnForBarnIder(barnIdForAlleAktuelleBehandlinger)
            val barnMedLøpendeStønad =
                barnPåBehandling
                    .filter { barnetViSerPå -> behandlingsbarn.any { it.personIdent == barnetViSerPå.personIdent } }
                    .map { it.id }
            return BarnMedLøpendeStønad(barn = barnMedLøpendeStønad, dato = vedtaksdatoEllerDagensdato)
        } ?: BarnMedLøpendeStønad(barn = emptyList(), dato = LocalDate.now())
    }
}
