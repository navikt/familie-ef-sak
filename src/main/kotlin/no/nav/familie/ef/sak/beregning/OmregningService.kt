package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.skolepenger.BeregningSkolepengerService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.simulering.BlankettSimuleringsService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.Simuleringsresultat
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID


@Service
class OmregningService(private val behandlingService: BehandlingService,
                       private val vedtakHistorikkService: VedtakHistorikkService,
                       private val taskService: TaskService,
                       private val iverksettClient: IverksettClient,
                       private val ytelseService: TilkjentYtelseService,
                       private val grunnlagsdataService: GrunnlagsdataService,
                       private val featureToggleService: FeatureToggleService,
                       private val vurderingService: VurderingService,
                       private val liveRunBeregnYtelseSteg: BeregnYtelseSteg,
                       private val dryRunBeregnYtelseSteg: DryRunBeregnYtelseSteg,
                       private val iverksettingDtoMapper: IverksettingDtoMapper) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun utførGOmregning(fagsakId: UUID,
                        liveRun: Boolean) {
        logger.info("Starter på g-omregning av fagsak=$fagsakId")

        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.omberegning")) {
            "Feature toggle for omberegning er disabled"
        }

        val sisteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                              ?: error("FagsakId $fagsakId har mistet iverksatt behandling.")

        feilHvis(behandlingService.finnesÅpenBehandling(fagsakId)) {
            "Kan ikke omberegne, det finnes åpen behandling på fagsak: $fagsakId"
        }

        val forrigeTilkjentYtelse = ytelseService.hentForBehandling(sisteBehandling.id)

        feilHvis(forrigeTilkjentYtelse.grunnbeløpsdato == nyesteGrunnbeløpGyldigFraOgMed) {
            "Skal ikke utføre g-omregning når forrige tilkjent ytelse allerede har nyeste grunnbeløpsdato"
        }

        val innvilgelseOvergangsstønad =
                vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(fagsakId,
                                                                           YearMonth.from(nyesteGrunnbeløpGyldigFraOgMed))
        feilHvis(innvilgelseOvergangsstønad.perioder.any { it.periodeType == VedtaksperiodeType.SANKSJON }) {
            "Omregning av vedtak med sanksjon må manuellt behandles"
        }

        if (innvilgelseOvergangsstønad.inntekter.any { (it.samordningsfradrag ?: BigDecimal.ZERO) > BigDecimal.ZERO }) {
            logger.info(MarkerFactory.getMarker("G-Omberegning - samordningsfradrag"),
                        "Fagsak med id $fagsakId har samordningsfradrag og må behandles manuelt.")
            throw OmregningMedSamordningsfradragException()
        }

        val behandling = behandlingService.opprettBehandling(behandlingType = BehandlingType.REVURDERING,
                                                             fagsakId = fagsakId,
                                                             behandlingsårsak = BehandlingÅrsak.G_OMREGNING)
        logger.info("G-omregner fagsak=$fagsakId behandling=${behandling.id} ")

        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.opprettVilkårForOmregning(behandling)


        val indeksjusterInntekt =
                BeregningUtils.indeksjusterInntekt(forrigeTilkjentYtelse.grunnbeløpsdato,
                                                   innvilgelseOvergangsstønad.inntekter.tilInntektsperioder())

        val saksbehandling = behandlingService.hentSaksbehandling(behandling.id)

        if (liveRun) {
            liveRunBeregnYtelseSteg.utførSteg(saksbehandling,
                                              InnvilgelseOvergangsstønad(periodeBegrunnelse = null,
                                                                         inntektBegrunnelse = null,
                                                                         perioder = innvilgelseOvergangsstønad.perioder,
                                                                         inntekter = indeksjusterInntekt.tilInntekt()))
        } else {
            dryRunBeregnYtelseSteg.utførSteg(saksbehandling,
                                             InnvilgelseOvergangsstønad(periodeBegrunnelse = null,
                                                                        inntektBegrunnelse = null,
                                                                        perioder = innvilgelseOvergangsstønad.perioder,
                                                                        inntekter = indeksjusterInntekt.tilInntekt()))
        }

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)

        val iverksettDto = iverksettingDtoMapper.tilDtoMaskineltBehandlet(saksbehandling)
        if (liveRun) {
            iverksettClient.iverksettUtenBrev(iverksettDto)
            taskService.save(PollStatusFraIverksettTask.opprettTask(behandling.id))
        } else {
            loggResultat(forrigeTilkjentYtelse, innvilgelseOvergangsstønad, fagsakId, behandling.id)
            throw DryRunException("Feature toggle familie.ef.sak.omberegning.live.run er ikke satt. Transaksjon rulles tilbake!")
        }

    }

    fun loggResultat(forrigeTilkjentYtelse: TilkjentYtelse,
                     innvilgelseOvergangsstønad: InnvilgelseOvergangsstønad,
                     fagsakId: UUID,
                     behandlingId: UUID) {
        val omberegnetTilkjentYtelse = ytelseService.hentForBehandling(behandlingId)


        val perioder = mapTilSammenlignbarePerioder(fagsakId,
                                                    forrigeTilkjentYtelse,
                                                    omberegnetTilkjentYtelse)

        logger.info(MarkerFactory.getMarker("G-Omberegning"), perioder.joinToString("\n"))
    }

    fun mapTilSammenlignbarePerioder(fagsakId: UUID,
                                     forrigeTilkjentYtelse: TilkjentYtelse,
                                     omberegnetTilkjentYtelse: TilkjentYtelse): List<RapportDto> {


        return forrigeTilkjentYtelse.andelerTilkjentYtelse.filter {
            it.stønadTom > nyesteGrunnbeløpGyldigFraOgMed
        }.map {
            if (it.stønadFom < nyesteGrunnbeløpGyldigFraOgMed) {
                it.copy(stønadFom = nyesteGrunnbeløpGyldigFraOgMed)
            } else {
                it
            }
        }.map {
            val omberegnetAndelTilkjentYtelse =
                    omberegnetTilkjentYtelse.andelerTilkjentYtelse.firstOrNull { andel ->
                        it.periode.omslutter(andel.periode) || it.periode.omsluttesAv(andel.periode)
                    } ?: error("Forventet omberegnet andelTilkjenYtelse med fradato ${it.stønadFom}")
            RapportDto(fagsakId,
                       it.stønadFom,
                       it.stønadTom,
                       it.beløp,
                       omberegnetAndelTilkjentYtelse.beløp,
                       it.inntekt,
                       omberegnetAndelTilkjentYtelse.inntekt)
        }

    }

    data class RapportDto(val fagsakId: UUID,
                          val fom: LocalDate,
                          val tom: LocalDate,
                          val gammelStønad: Int,
                          val omberegnetStønad: Int,
                          val gammelInntekt: Int,
                          val omberegnetInntekt: Int)
}

@Service
class DryRunBeregnYtelseSteg(tilkjentYtelseService: TilkjentYtelseService,
                             beregningService: BeregningService,
                             dryRunSimuleringService: DryRunSimuleringService,
                             beregningBarnetilsynService: BeregningBarnetilsynService,
                             beregningSkolepengerService: BeregningSkolepengerService,
                             vedtakService: VedtakService,
                             tilbakekrevingService: TilbakekrevingService,
                             barnService: BarnService,
                             fagsakService: FagsakService) {

    private val beregnYtelseSteg = BeregnYtelseSteg(tilkjentYtelseService,
                                                    beregningService,
                                                    beregningBarnetilsynService,
                                                    beregningSkolepengerService,
                                                    dryRunSimuleringService,
                                                    vedtakService,
                                                    tilbakekrevingService,
                                                    barnService,
                                                    fagsakService)

    fun utførSteg(saksbehandling: Saksbehandling, data: VedtakDto) {
        beregnYtelseSteg.utførSteg(saksbehandling, data)
    }

}


@Service
class DryRunSimuleringService(iverksettClient: IverksettClient,
                              vedtakService: VedtakService,
                              blankettSimuleringsService: BlankettSimuleringsService,
                              simuleringsresultatRepository: SimuleringsresultatRepository,
                              tilkjentYtelseService: TilkjentYtelseService,
                              tilgangService: TilgangService) : SimuleringService(iverksettClient,
                                                                                  vedtakService,
                                                                                  blankettSimuleringsService,
                                                                                  simuleringsresultatRepository,
                                                                                  tilkjentYtelseService,
                                                                                  tilgangService) {

    private val simuleringsoppsummering =
            Simuleringsoppsummering(listOf(), null, BigDecimal.ZERO, BigDecimal.ZERO, null, null, null, null, null)

    private val beriketSimuleringsresultat = BeriketSimuleringsresultat(DetaljertSimuleringResultat(listOf()),
                                                                        simuleringsoppsummering)

    override fun simuler(saksbehandling: Saksbehandling): Simuleringsoppsummering {
        throw IllegalAccessException("Forventer ikke kall til simuler fra BeregnYtelseSteg.")
    }

    override fun hentLagretSimuleringsoppsummering(behandlingId: UUID): Simuleringsoppsummering {
        throw IllegalAccessException("Forventer ikke kall til hentLagretSimuleringsoppsummering er fra BeregnYtelseSteg.")
    }

    override fun hentLagretSimmuleringsresultat(behandlingId: UUID): BeriketSimuleringsresultat {
        throw IllegalAccessException("Forventer ikke kall til hentLagretSimmuleringsresultat fra BeregnYtelseSteg.")
    }

    override fun slettSimuleringForBehandling(saksbehandling: Saksbehandling) {}

    override fun hentOgLagreSimuleringsresultat(saksbehandling: Saksbehandling): Simuleringsresultat {
        return Simuleringsresultat(behandlingId = UUID.randomUUID(),
                                   data = DetaljertSimuleringResultat(listOf()),
                                   beriketData = beriketSimuleringsresultat)
    }
}

class DryRunException(melding: String) : IllegalStateException(melding)
class OmregningMedSamordningsfradragException() : IllegalStateException()
