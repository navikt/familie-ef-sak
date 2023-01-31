package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingUtil.sisteFerdigstilteBehandling
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
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vedtak.KopierVedtakService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
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
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val fagsakService: FagsakService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val stegService: StegService,
    private val kopierVedtakService: KopierVedtakService,
    private val vedtakService: VedtakService
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
        val forrigeBehandlingId = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)?.id
            ?: error("Revurdering må ha eksisterende iverksatt behandling")
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
        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = revurdering.id,
                    saksbehandler = saksbehandler,
                    beskrivelse = "Revurdering i ny løsning"
                )
            )
        )
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))

        if (erSatsendring(revurderingInnhold)) {
            val vedtakDto = kopierVedtakService.lagVedtakDtoBasertPåTidligereVedtaksperioder(
                fagsakId = fagsak.id,
                forrigeBehandlingId = forrigeBehandlingId,
                revurderingId = revurdering.id
            )
            vedtakService.lagreVedtak(
                vedtakDto = vedtakDto,
                behandlingId = revurdering.id,
                stønadstype = fagsak.stønadstype
            )
        }

        return revurdering
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
                erSatsendring(revurderingInnhold)
        ) {
            "Kan ikke opprette revurdering med årsak satsendring for ${fagsak.stønadstype}"
        }
    }

    private fun erSatsendring(revurderingInnhold: RevurderingDto) =
        revurderingInnhold.behandlingsårsak == BehandlingÅrsak.SATSENDRING
}
