package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.tilBehandlingBarn
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
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
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val vedtakService: VedtakService,
    private val vedtakHistorikkService: VedtakHistorikkService,
    private val barnRepository: BarnRepository
) {

    @Transactional
    fun opprettRevurderingManuelt(revurderingInnhold: RevurderingDto): Behandling {
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(revurderingInnhold.fagsakId)
        validerOpprettRevurdering(fagsak, revurderingInnhold)

        val revurdering = behandlingService.opprettBehandling(
            BehandlingType.REVURDERING,
            revurderingInnhold.fagsakId,
            BehandlingStatus.UTREDES,
            StegType.BEREGNE_YTELSE,
            revurderingInnhold.behandlingsårsak,
            revurderingInnhold.kravMottatt
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
        vurderingService.kopierVurderingerTilNyBehandling(forrigeBehandlingId, revurdering.id, metadata, fagsak.stønadstype)
        val oppgaveId = oppgaveService.opprettOppgave(
            behandlingId = revurdering.id,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = saksbehandler,
            beskrivelse = "Revurdering i ny løsning"
        )

        taskRepository.save(BehandlingsstatistikkTask.opprettMottattTask(behandlingId = revurdering.id, oppgaveId = oppgaveId))
        taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))

        kopierVedtakHvisSatsendring(revurderingInnhold, fagsak, revurdering)
        return revurdering
    }

    private fun kopierVedtakHvisSatsendring(
        revurderingInnhold: RevurderingDto,
        fagsak: Fagsak,
        revurdering: Behandling
    ) {
        if (revurderingInnhold.behandlingsårsak == BehandlingÅrsak.SATSENDRING) {
            val behandlingBarn = barnRepository.findByBehandlingId(revurdering.id)
            val vedtakDto = mapTilBarnetilsynVedtak(fagsak.id, behandlingBarn)
            vedtakService.lagreVedtak(vedtakDto, revurdering.id, StønadType.BARNETILSYN)
        }
    }

    private fun mapTilBarnetilsynVedtak(fagsakId: UUID, behandlingBarn: List<BehandlingBarn>): VedtakDto {
        val historikk = vedtakHistorikkService.hentAktivHistorikk(fagsakId)

        return InnvilgelseBarnetilsyn(
            perioder = mapUtgiftsperioder(historikk, behandlingBarn),
            resultatType = ResultatType.INNVILGE,
            perioderKontantstøtte = mapPerioderKontantstøtte(historikk),
            tilleggsstønad = mapTilleggsstønadDto(historikk),
            begrunnelse = "Satsendring barnetilsyn"
        )
    }

    private fun mapTilleggsstønadDto(historikk: List<AndelHistorikkDto>): TilleggsstønadDto {
        return TilleggsstønadDto(
            historikk.any { it.andel.tilleggsstønad > 0 },
            historikk.map {
                PeriodeMedBeløpDto(periode = it.andel.periode, beløp = it.andel.tilleggsstønad)
            },
            null
        )
    }

    private fun mapPerioderKontantstøtte(historikk: List<AndelHistorikkDto>): List<PeriodeMedBeløpDto> {
        return historikk.filter { kontanstaøtte -> kontanstaøtte.andel.kontantstøtte > 0 }
            .map {
            PeriodeMedBeløpDto(
                periode = it.andel.periode,
                beløp = it.andel.kontantstøtte
            )
        }
    }

    private fun mapUtgiftsperioder(historikk: List<AndelHistorikkDto>, behandlingBarn: List<BehandlingBarn>): List<UtgiftsperiodeDto> {
        return historikk.map {
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
        val tidligereValgteAndelBarn = barnRepository.findAllById(andelBarn)
        return behandlingBarn.filter { it.personIdent in tidligereValgteAndelBarn.map { b -> b.personIdent } }.map { it.id }
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
