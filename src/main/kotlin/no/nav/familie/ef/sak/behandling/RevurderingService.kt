package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveForOpprettetBehandlingTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
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
    private val vurderingService: VurderingService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val taskService: TaskService,
    private val barnService: BarnService,
    private val fagsakService: FagsakService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val stegService: StegService,
    private val kopierVedtakService: KopierVedtakService,
    private val vedtakService: VedtakService,
    private val nyeBarnService: NyeBarnService,
    private val tilordnetRessursService: TilordnetRessursService,
) {
    fun hentRevurderingsinformasjon(behandlingId: UUID): RevurderingsinformasjonDto = årsakRevurderingService.hentRevurderingsinformasjon(behandlingId)

    fun lagreRevurderingsinformasjon(
        behandlingId: UUID,
        revurderingsinformasjonDto: RevurderingsinformasjonDto,
    ): RevurderingsinformasjonDto {
        stegService.håndterÅrsakRevurdering(behandlingId, revurderingsinformasjonDto)
        return hentRevurderingsinformasjon(behandlingId)
    }

    @Transactional
    fun slettRevurderingsinformasjon(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette revurderingsinformasjon når behandlingen er låst"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)) {
            "Behandlingen har en ny eier og du kan derfor ikke slette revurderingsinformasjon"
        }
        årsakRevurderingService.slettRevurderingsinformasjon(behandlingId)
    }

    @Transactional
    fun opprettRevurderingManuelt(revurderingInnhold: RevurderingDto): Behandling {
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(revurderingInnhold.fagsakId)
        validerOpprettRevurdering(fagsak, revurderingInnhold)

        val revurdering =
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.REVURDERING,
                fagsakId = revurderingInnhold.fagsakId,
                status = BehandlingStatus.UTREDES,
                stegType = StegType.BEREGNE_YTELSE,
                behandlingsårsak = revurderingInnhold.behandlingsårsak,
                kravMottatt = revurderingInnhold.kravMottatt,
            )
        val forrigeBehandlingId =
            behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)?.id
                ?: error("Revurdering må ha eksisterende iverksatt behandling")
        val saksbehandler = SikkerhetContext.hentSaksbehandler()

        søknadService.kopierSøknad(forrigeBehandlingId, revurdering.id)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)

        val terminbarn = revurderingInnhold.barnSomSkalFødes.map { it.tilBehandlingBarn(revurdering.id) }
        val nyeBarnFraRegister = vilkårsbehandleNyeBarn(revurdering, revurderingInnhold.vilkårsbehandleNyeBarn)
        barnService.opprettBarnForRevurdering(
            behandlingId = revurdering.id,
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = nyeBarnFraRegister + terminbarn,
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = fagsak.stønadstype,
        )
        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(revurdering.id)
        vurderingService.kopierVurderingerTilNyBehandling(
            eksisterendeBehandlingId = forrigeBehandlingId,
            nyBehandlingsId = revurdering.id,
            metadata = metadata,
            stønadType = fagsak.stønadstype,
        )
        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = revurdering.id,
                    saksbehandler = saksbehandler,
                    beskrivelse = "Revurdering i ny løsning",
                ),
            ),
        )
        taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))

        if (erSatsendring(revurderingInnhold)) {
            val vedtakDto =
                kopierVedtakService.lagVedtakDtoBasertPåTidligereVedtaksperioder(
                    fagsakId = fagsak.id,
                    forrigeBehandlingId = forrigeBehandlingId,
                    revurderingId = revurdering.id,
                )
            vedtakService.lagreVedtak(
                vedtakDto = vedtakDto,
                behandlingId = revurdering.id,
                stønadstype = fagsak.stønadstype,
            )
        }

        return revurdering
    }

    private fun vilkårsbehandleNyeBarn(
        revurdering: Behandling,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn,
    ): List<BehandlingBarn> {
        val nyeBarn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(revurdering.fagsakId)
        if (revurdering.årsak != BehandlingÅrsak.G_OMREGNING) {
            feilHvis(
                nyeBarn.harBarnISisteIverksatteBehandling &&
                    vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            ) {
                "Må vilkårsbehandle nye barn når det finnes barn på forrige behandling"
            }
            brukerfeilHvis(
                !nyeBarn.harBarnISisteIverksatteBehandling &&
                    nyeBarn.nyeBarn.isNotEmpty() &&
                    vilkårsbehandleNyeBarn == VilkårsbehandleNyeBarn.IKKE_VALGT,
            ) {
                "Må ta stilling til nye barn"
            }
        } else {
            feilHvis(vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE) {
                "Skal ikke vilkårsbehandle nye barn for g-omregning"
            }
        }
        return when (vilkårsbehandleNyeBarn) {
            VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE -> {
                nyeBarn.nyeBarn.map {
                    BehandlingBarn(
                        behandlingId = revurdering.id,
                        søknadBarnId = null,
                        personIdent = it.personIdent,
                        navn = it.navn,
                        fødselTermindato = null,
                    )
                }
            }

            VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE -> emptyList()
            VilkårsbehandleNyeBarn.IKKE_VALGT -> emptyList()
        }
    }

    private fun validerOpprettRevurdering(
        fagsak: Fagsak,
        revurderingInnhold: RevurderingDto,
    ) {
        feilHvis(
            fagsak.stønadstype != StønadType.OVERGANGSSTØNAD &&
                revurderingInnhold.behandlingsårsak == BehandlingÅrsak.G_OMREGNING,
        ) {
            "Kan ikke opprette revurdering med årsak g-omregning for ${fagsak.stønadstype}"
        }
        feilHvis(
            fagsak.stønadstype != StønadType.BARNETILSYN &&
                erSatsendring(revurderingInnhold),
        ) {
            "Kan ikke opprette revurdering med årsak satsendring for ${fagsak.stønadstype}"
        }
        if (revurderingInnhold.barnSomSkalFødes.isNotEmpty()) {
            feilHvis(fagsak.stønadstype == StønadType.BARNETILSYN) { "Kan ikke legge inn terminbarn for barnetilsyn" }
            feilHvis(revurderingInnhold.behandlingsårsak != BehandlingÅrsak.PAPIRSØKNAD && revurderingInnhold.behandlingsårsak != BehandlingÅrsak.NYE_OPPLYSNINGER) { "Terminbarn på revurdering kan kun legges inn for papirsøknader og nye opplysninger" }
        }
    }

    private fun erSatsendring(revurderingInnhold: RevurderingDto) = revurderingInnhold.behandlingsårsak == BehandlingÅrsak.SATSENDRING
}
