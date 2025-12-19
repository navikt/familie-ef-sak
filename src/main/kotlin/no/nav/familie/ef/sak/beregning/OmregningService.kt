package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.MarkerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class OmregningService(
    private val behandlingService: BehandlingService,
    private val vedtakHistorikkService: VedtakHistorikkService,
    private val taskService: TaskService,
    private val iverksettClient: IverksettClient,
    private val ytelseService: TilkjentYtelseService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val vurderingService: VurderingService,
    private val beregnYtelseSteg: BeregnYtelseSteg,
    private val iverksettingDtoMapper: IverksettingDtoMapper,
    private val søknadService: SøknadService,
    private val barnService: BarnService,
) {
    private val logger = Logg.getLogger(this::class)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun utførGOmregning(fagsakId: UUID) {
        val forrigeTilkjentYtelse = validerBehandlingOgHentSisteTilkjentYtelse(fagsakId)
        val innvilgelseOvergangsstønad = hentInnvilgelseForOvergangsstønad(fagsakId) ?: return

        utførGOmregning(fagsakId, forrigeTilkjentYtelse, innvilgelseOvergangsstønad)
    }

    private fun hentInnvilgelseForOvergangsstønad(fagsakId: UUID): InnvilgelseOvergangsstønad? {
        val innvilgelseOvergangsstønad =
            vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(
                fagsakId,
                YearMonth.from(Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed),
            )
        if (innvilgelseOvergangsstønad.perioder.any { it.periodeType == VedtaksperiodeType.SANKSJON }) {
            logger.warn("G-Omregning - Manuell: Fagsak med id $fagsakId har sanksjon og må manuelt behandles")
            return null
        }

        if (innvilgelseOvergangsstønad.inntekter.any { (it.samordningsfradrag ?: BigDecimal.ZERO) > BigDecimal.ZERO }) {
            logger.warn("G-Omregning - Manuell: Fagsak med id $fagsakId har samordningsfradrag og må behandles manuelt.")
            return null
        }
        return innvilgelseOvergangsstønad
    }

    private fun validerBehandlingOgHentSisteTilkjentYtelse(fagsakId: UUID): TilkjentYtelse {
        val sisteBehandling =
            behandlingService.finnSisteIverksatteBehandling(fagsakId)
                ?: error("FagsakId $fagsakId har mistet iverksatt behandling.")

        validerBehandlingstatusForFagsak(fagsakId)

        val forrigeTilkjentYtelse = ytelseService.hentForBehandling(sisteBehandling.id)

        feilHvis(forrigeTilkjentYtelse.grunnbeløpsmåned == Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed) {
            "Skal ikke utføre g-omregning når forrige tilkjent ytelse allerede har nyeste grunnbeløpsdato"
        }
        return forrigeTilkjentYtelse
    }

    private fun validerBehandlingstatusForFagsak(fagsakId: UUID) {
        feilHvis(behandlingService.finnesÅpenBehandling(fagsakId)) {
            "Kan ikke omregne, det finnes åpen behandling på fagsak: $fagsakId"
        }
    }

    private fun utførGOmregning(
        fagsakId: UUID,
        forrigeTilkjentYtelse: TilkjentYtelse,
        innvilgelseOvergangsstønad: InnvilgelseOvergangsstønad,
    ) {
        logger.info("Starter på g-omregning av fagsak=$fagsakId")

        val behandling =
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.REVURDERING,
                fagsakId = fagsakId,
                behandlingsårsak = BehandlingÅrsak.G_OMREGNING,
            )
        logger.info("G-omregner fagsak=$fagsakId behandling=${behandling.id} ")

        kopierDataFraForrigeBehandling(behandling)

        val indeksjusterInntekt =
            BeregningUtils.indeksjusterInntekt(
                forrigeTilkjentYtelse.grunnbeløpsmåned,
                innvilgelseOvergangsstønad.inntekter.tilInntektsperioder(),
            )

        val saksbehandling = behandlingService.hentSaksbehandling(behandling.id)

        beregnYtelseSteg.utførSteg(
            saksbehandling,
            InnvilgelseOvergangsstønad(
                periodeBegrunnelse = null,
                inntektBegrunnelse = null,
                perioder = innvilgelseOvergangsstønad.perioder,
                inntekter = indeksjusterInntekt.tilInntekt(),
            ),
        )

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)

        val iverksettDto = iverksettingDtoMapper.tilDtoMaskineltBehandlet(saksbehandling)
        iverksettClient.iverksettUtenBrev(iverksettDto)
        taskService.save(PollStatusFraIverksettTask.opprettTask(behandling.id))
    }

    private fun kopierDataFraForrigeBehandling(behandling: Behandling) {
        val forrigeBehandlingId =
            behandling.forrigeBehandlingId
                ?: error("Finner ikke forrigeBehandlingId til ${behandling.id}")
        søknadService.kopierSøknad(forrigeBehandlingId, behandling.id)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        barnService.opprettBarnForRevurdering(
            behandlingId = behandling.id,
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = emptyList(),
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = StønadType.OVERGANGSSTØNAD,
        )
        vurderingService.opprettVilkårForOmregning(behandling)
    }
}
