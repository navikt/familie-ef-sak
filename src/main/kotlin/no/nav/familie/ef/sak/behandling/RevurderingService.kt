package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.tilBehandlingBarn
import no.nav.familie.ef.sak.behandling.dto.tilDomene
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.data.repository.findByIdOrNull
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
    private val årsakRevurderingsRepository: ÅrsakRevurderingsRepository
) {

    // eget steg / flytt til stegservice
    @Transactional
    fun lagreÅrsakRevurdering(behandlingId: UUID, årsakRevurderingDto: ÅrsakRevurderingDto) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og kan ikke oppdatere årsak til revurdering"
        }
        årsakRevurderingsRepository.deleteById(behandlingId)
        årsakRevurderingsRepository.insert(årsakRevurderingDto.tilDomene(behandlingId))
    }

    fun hentÅrsakRevurdering(behandlingId: UUID): ÅrsakRevurdering? {
        return årsakRevurderingsRepository.findByIdOrNull(behandlingId)
    }

    @Transactional
    fun opprettRevurderingManuelt(revurderingInnhold: RevurderingDto): Behandling {
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(revurderingInnhold.fagsakId)
        validerOpprettRevurdering(fagsak, revurderingInnhold)

        val revurdering = behandlingService.opprettBehandling(
            behandlingType = BehandlingType.REVURDERING,
            fagsakId = revurderingInnhold.fagsakId,
            status = BehandlingStatus.UTREDES,
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
        vurderingService.kopierVurderingerTilNyBehandling(forrigeBehandlingId, revurdering.id, metadata, fagsak.stønadstype)
        val oppgaveId = oppgaveService.opprettOppgave(
            behandlingId = revurdering.id,
            oppgavetype = Oppgavetype.BehandleSak,
            tilordnetNavIdent = saksbehandler,
            beskrivelse = "Revurdering i ny løsning"
        )

        taskRepository.save(BehandlingsstatistikkTask.opprettMottattTask(behandlingId = revurdering.id, oppgaveId = oppgaveId))
        taskRepository.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))
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
