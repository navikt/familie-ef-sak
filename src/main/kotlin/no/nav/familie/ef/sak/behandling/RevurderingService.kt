package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.tilBehandlingBarn
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynUtil
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vedtak.historikk.fraDato
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.util.UUID

@Service
class RevurderingService(
    private val søknadService: SøknadService,
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val vurderingService: VurderingService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val taskRepository: TaskRepository,
    private val barnService: BarnService,
    private val fagsakService: FagsakService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val stegService: StegService,
    private val vedtakService: VedtakService,
    private val vedtakHistorikkService: VedtakHistorikkService,
    private val barnRepository: BarnRepository
) {

    fun hentRevurderingsinformasjon(behandlingId: UUID): RevurderingsinformasjonDto {
        return årsakRevurderingService.hentRevurderingsinformasjon(behandlingId)
    }

    fun lagreRevurderingsinformasjon(
        behandlingId: UUID,
        revurderingsinformasjonDto: RevurderingsinformasjonDto
    ): RevurderingsinformasjonDto {
        stegService.håndterÅrsakRevurdering(behandlingId, revurderingsinformasjonDto)
        return hentRevurderingsinformasjon(behandlingId)
    }

    @Transactional
    fun slettRevurderingsinformasjon(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette revurderingsinformasjon når behandlingen er låst"
        }
        årsakRevurderingService.slettRevurderingsinformasjon(behandlingId)
    }

    @Transactional
    fun opprettRevurderingManuelt(revurderingInnhold: RevurderingDto): Behandling {
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(revurderingInnhold.fagsakId)
        validerOpprettRevurdering(fagsak, revurderingInnhold)

        val revurdering = behandlingService.opprettBehandling(
            behandlingType = BehandlingType.REVURDERING,
            fagsakId = revurderingInnhold.fagsakId,
            status = BehandlingStatus.UTREDES,
            stegType = StegType.BEREGNE_YTELSE,
            behandlingsårsak = revurderingInnhold.behandlingsårsak,
            kravMottatt = revurderingInnhold.kravMottatt
        )
        val forrigeBehandlingId = forrigeBehandling(revurdering)
        val saksbehandler = SikkerhetContext.hentSaksbehandler(true)

        søknadService.kopierSøknad(forrigeBehandlingId, revurdering.id)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)

        barnService.opprettBarnForRevurdering(
            behandlingId = revurdering.id,
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = revurderingInnhold.barn.tilBehandlingBarn(revurdering.id),
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = fagsak.stønadstype
        )
        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(revurdering.id)
        vurderingService.kopierVurderingerTilNyBehandling(
            forrigeBehandlingId,
            revurdering.id,
            metadata,
            fagsak.stønadstype
        )
        val oppgaveId = oppgaveService.opprettOppgave(
            behandlingId = revurdering.id,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = saksbehandler,
            beskrivelse = "Revurdering i ny løsning"
        )

        taskRepository.save(
            BehandlingsstatistikkTask.opprettMottattTask(
                behandlingId = revurdering.id,
                oppgaveId = oppgaveId
            )
        )
        taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))

        kopierVedtakHvisSatsendring(revurderingInnhold.behandlingsårsak, fagsak, revurdering, forrigeBehandlingId)
        return revurdering
    }

    fun kopierVedtakHvisSatsendring(
        behandlingsÅrsak: BehandlingÅrsak,
        fagsak: Fagsak,
        revurdering: Behandling,
        forrigeBehandlingId: UUID
    ) {
        if (behandlingsÅrsak == BehandlingÅrsak.SATSENDRING) {
            val behandlingBarn = barnRepository.findByBehandlingId(revurdering.id)
            val vedtakDto = mapTilBarnetilsynVedtak(fagsak.id, behandlingBarn, forrigeBehandlingId)
            vedtakService.lagreVedtak(vedtakDto, revurdering.id, StønadType.BARNETILSYN)
        }
    }

    fun mapTilBarnetilsynVedtak(fagsakId: UUID, behandlingBarn: List<BehandlingBarn>, forrigeBehandlingId: UUID): VedtakDto {
        val fraDato = BeregningBarnetilsynUtil.ikkeVedtatteSatserForBarnetilsyn.maxOf { it.periode.fom }
        val historikk = vedtakHistorikkService.hentAktivHistorikk(fagsakId).fraDato(YearMonth.from(fraDato))

        return InnvilgelseBarnetilsyn(
            perioder = mapUtgiftsperioder(historikk, behandlingBarn),
            resultatType = ResultatType.INNVILGE,
            perioderKontantstøtte = mapPerioderKontantstøtte(historikk),
            tilleggsstønad = mapTilleggsstønadDto(historikk, forrigeBehandlingId),
            begrunnelse = "Satsendring barnetilsyn"
        )
    }

    private fun mapTilleggsstønadDto(historikk: List<AndelHistorikkDto>, forrigeBehandlingId: UUID): TilleggsstønadDto {
        return TilleggsstønadDto(
            historikk.any { it.andel.tilleggsstønad > 0 },
            historikk.filter { it.andel.tilleggsstønad > 0 }.map {
                PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.tilleggsstønad)
            },
            vedtakService.hentVedtak(forrigeBehandlingId).tilleggsstønad?.begrunnelse
        )
    }

    private fun mapPerioderKontantstøtte(historikk: List<AndelHistorikkDto>): List<PeriodeMedBeløpDto> {
        return historikk.filter { kontantstøtte -> kontantstøtte.andel.kontantstøtte > 0 }
            .map {
                PeriodeMedBeløpDto(
                    periode = it.andel.periode,
                    beløp = it.andel.kontantstøtte
                )
            }
    }

    private fun mapUtgiftsperioder(historikk: List<AndelHistorikkDto>, behandlingBarn: List<BehandlingBarn>): List<UtgiftsperiodeDto> {
        return historikk.map {
            feilHvis(vedtakService.hentVedtak(it.behandlingId).barnetilsyn?.perioder?.any { v -> v.erMidlertidigOpphør == true } ?: false) {
                "Ikke implementert: Kan ikke satsendre andeler med midlertidig opphør."
            }
            UtgiftsperiodeDto(
                årMånedFra = it.andel.periode.fom,
                årMånedTil = it.andel.periode.tom,
                periode = it.andel.periode,
                barn = finnBehandlingBarnIdsGittTidligereAndelBarn(it.andel.barn, behandlingBarn),
                utgifter = it.andel.utgifter.toInt(),
                erMidlertidigOpphør = false
            )
        }
    }

    private fun finnBehandlingBarnIdsGittTidligereAndelBarn(andelBarn: List<UUID>, behandlingBarn: List<BehandlingBarn>): List<UUID> {
        val tidligereValgteAndelBarn = barnRepository.findAllById(andelBarn).map { it.personIdent }
        return behandlingBarn.filter { it.personIdent in tidligereValgteAndelBarn }.map { it.id }
    }

    private fun validerOpprettRevurdering(fagsak: Fagsak, revurderingInnhold: RevurderingDto) {
        feilHvis(
            fagsak.stønadstype != StønadType.OVERGANGSSTØNAD &&
                revurderingInnhold.behandlingsårsak == BehandlingÅrsak.G_OMREGNING
        ) {
            "Kan ikke opprette revurdering med årsak g-omregning for ${fagsak.stønadstype}"
        }
        feilHvis(
            fagsak.stønadstype != StønadType.BARNETILSYN &&
                revurderingInnhold.behandlingsårsak == BehandlingÅrsak.SATSENDRING
        ) {
            "Kan ikke opprette revurdering med årsak satsendring for ${fagsak.stønadstype}"
        }
    }

    /**
     * Returnerer id til forrige behandling.
     * Skal håndtere en førstegangsbehandling som er avslått, då vi trenger en behandlingId for å kopiere data fra søknaden
     */
    private fun forrigeBehandling(revurdering: Behandling): UUID {
        val sisteBehandling = behandlingService.hentBehandlinger(revurdering.fagsakId)
            .filter { it.id != revurdering.id }
            .filter { it.resultat != BehandlingResultat.HENLAGT }
            .maxByOrNull { it.sporbar.opprettetTid }
        return revurdering.forrigeBehandlingId
            ?: sisteBehandling?.id
            ?: error("Revurdering må ha eksisterende iverksatt behandling")
    }
}
