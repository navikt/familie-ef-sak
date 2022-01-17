package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.PollStatusFraIverksettTask
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettService
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.YearMonth

@Service
class MigreringService(
        private val taskRepository: TaskRepository,
        private val fagsakService: FagsakService,
        private val behandlingService: BehandlingService,
        private val iverksettService: IverksettService,
        private val iverksettClient: IverksettClient,
        private val grunnlagsdataService: GrunnlagsdataService,
        private val vurderingService: VurderingService,
        private val beregnYtelseSteg: BeregnYtelseSteg,
        private val iverksettingDtoMapper: IverksettingDtoMapper,
        private val featureToggleService: FeatureToggleService
) {

    /**
     * Henter data fra infotrygd og oppretter migrering

    @Transactional
    fun opprettMigrering(personIdent: String) {
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val fra = YearMonth.now()
        val til = YearMonth.now().plusMonths(1)
        val forventetInntekt = BigDecimal.ZERO
        val samordningsfradrag = BigDecimal.ZERO
        opprettMigrering(fagsak, fra, til, forventetInntekt, samordningsfradrag)
    }
    */

    @Transactional
    fun opprettMigrering(fagsak: Fagsak,
                         fra: YearMonth,
                         til: YearMonth,
                         forventetInntekt: BigDecimal,
                         samordningsfradrag: BigDecimal): Behandling {
        feilHvisIkke(featureToggleService.isEnabled("familie.ef.sak.migrering")) {
            "Feature toggle for migrering er disabled"
        }
        fagsakService.settFagsakTilMigrert(fagsak.id)
        val behandling = behandlingService.opprettMigrering(fagsak.id)
        iverksettService.startBehandling(behandling, fagsak)

        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vurderingService.opprettMaskielltOpprettedeVurderinger(behandling)

        val vedtaksperiode = VedtaksperiodeDto(årMånedFra = fra,
                                               årMånedTil = til,
                                               aktivitet = AktivitetType.MIGRERING,
                                               periodeType = VedtaksperiodeType.MIGRERING)

        val inntekt = Inntekt(fra, forventetInntekt = forventetInntekt, samordningsfradrag = samordningsfradrag)
        beregnYtelseSteg.utførSteg(behandling, Innvilget(resultatType = ResultatType.INNVILGE,
                                                         periodeBegrunnelse = null,
                                                         inntektBegrunnelse = null,
                                                         perioder = listOf(vedtaksperiode),
                                                         inntekter = listOf(inntekt)))

        // TODO burde vi sjekke att simulere ikke gir diff? Ikke sikkert den gir noe riktig beløp for neste måned?

        behandlingService.oppdaterResultatPåBehandling(behandling.id, BehandlingResultat.INNVILGET)
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.IVERKSETTER_VEDTAK)
        val iverksettDto = iverksettingDtoMapper.tilMigreringDto(behandling)
        iverksettClient.iverksettMigrering(iverksettDto)
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandling.id))

        return behandlingService.hentBehandling(behandling.id)
    }
}