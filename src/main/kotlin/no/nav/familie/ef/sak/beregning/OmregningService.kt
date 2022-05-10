package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID


@Service
class OmregningService(private val behandlingService: BehandlingService,
                       private val fagsakService: FagsakService,
                       private val vedtakService: VedtakService,
                       private val vedtakHistorikkService: VedtakHistorikkService,
                       private val taskRepository: TaskRepository,
                       private val behandlingRepository: BehandlingRepository,
                       private val fagsakRepository: FagsakRepository,
                       private val iverksettClient: IverksettClient,
                       private val ytelseService: TilkjentYtelseService,
                       private val grunnlagsdataService: GrunnlagsdataService,
                       private val featureToggleService: FeatureToggleService,
                       private val vurderingService: VurderingService,
                       private val beregnYtelseSteg: BeregnYtelseSteg,
                       private val iverksettingDtoMapper: IverksettingDtoMapper) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun utførGOmregning(ferdigstiltBehandlingIdMedGammelG: UUID) {

        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.omberegning")) {
            "Feature toggle for omberegning er disabled"
        }

        val fagsak = fagsakRepository.finnFagsakTilBehandling(ferdigstiltBehandlingIdMedGammelG)

        val fagsakMedOppdatertPersonIdent = fagsakService.fagsakMedOppdatertPersonIdent(fagsak.id)
        val sisteBehandling =
                behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(fagsakMedOppdatertPersonIdent.stønadstype,
                                                                          fagsakMedOppdatertPersonIdent.personIdenter.map {
                                                                              it.ident
                                                                          }.toSet())

        feilHvis(sisteBehandling == null) {
            "Kan ikke omberegne: Fant ikke sisteBehandlingId for fagsak ${fagsak.id}"
        }
        feilHvisIkke(sisteBehandling.erAvsluttet()) {
            "Kan ikke omberegne fagsak med åpen behandling, id=${sisteBehandling.id}"
        }

        val behandling = behandlingService.opprettBehandling(behandlingType = BehandlingType.REVURDERING,
                                                             fagsakId = fagsak.id,
                                                             behandlingsårsak = BehandlingÅrsak.G_OMREGNING)
        logger.info("G-omregner fagsak=${fagsak.id} behandling=${behandling.id} ")


        val forrigeTilkjentYtelse = ytelseService.hentForBehandling(ferdigstiltBehandlingIdMedGammelG)

        val innvilgelseOvergangsstønad =
                vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(fagsak.id,
                                                                           YearMonth.from(nyesteGrunnbeløpGyldigFraOgMed))

        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.opprettVilkårForOmregning(behandling)


        val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(forrigeTilkjentYtelse.grunnbeløpsdato,
                                                   innvilgelseOvergangsstønad.inntekter.tilInntektsperioder())
        val saksbehandling = behandlingService.hentSaksbehandling(behandling.id)

        beregnYtelseSteg.utførSteg(saksbehandling, InnvilgelseOvergangsstønad(periodeBegrunnelse = null,
                                                                              inntektBegrunnelse = null,
                                                                              perioder = innvilgelseOvergangsstønad.perioder,
                                                                              inntekter = indeksjusterInntekt.tilInntekt()))

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)

        val iverksettDto = iverksettingDtoMapper.tilDtoMaskineltBehandlet(saksbehandling)
        iverksettClient.iverksettUtenBrev(iverksettDto)
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandling.id))

    }

}