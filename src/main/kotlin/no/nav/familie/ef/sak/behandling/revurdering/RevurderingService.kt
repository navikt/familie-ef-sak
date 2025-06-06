package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.NyeBarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.steg.ÅrsakRevurderingSteg
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
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.IsoFields
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
    private val årsakRevurderingSteg: ÅrsakRevurderingSteg,
    private val stegService: StegService,
    private val kopierVedtakService: KopierVedtakService,
    private val vedtakService: VedtakService,
    private val nyeBarnService: NyeBarnService,
    private val tilordnetRessursService: TilordnetRessursService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentRevurderingsinformasjon(behandlingId: UUID): RevurderingsinformasjonDto = årsakRevurderingService.hentRevurderingsinformasjon(behandlingId)

    fun lagreRevurderingsinformasjon(
        behandlingId: UUID,
        revurderingsinformasjonDto: RevurderingsinformasjonDto,
    ): RevurderingsinformasjonDto {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        stegService.håndterSteg(saksbehandling, årsakRevurderingSteg, revurderingsinformasjonDto)

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
    fun opprettRevurderingManuelt(revurderingDto: RevurderingDto): Behandling {
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(revurderingDto.fagsakId)
        validerOpprettRevurdering(fagsak, revurderingDto)

        val revurdering =
            behandlingService.opprettBehandling(
                behandlingType = BehandlingType.REVURDERING,
                fagsakId = revurderingDto.fagsakId,
                status = BehandlingStatus.UTREDES,
                stegType = StegType.BEREGNE_YTELSE,
                behandlingsårsak = revurderingDto.behandlingsårsak,
                kravMottatt = revurderingDto.kravMottatt,
            )
        val forrigeBehandlingId =
            behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsak.id)?.id
                ?: error("Revurdering må ha eksisterende iverksatt behandling")

        søknadService.kopierSøknad(forrigeBehandlingId, revurdering.id)
        val grunnlagsdata = grunnlagsdataService.opprettGrunnlagsdata(revurdering.id)

        val terminbarn = revurderingDto.barnSomSkalFødes.map { it.tilBehandlingBarn(revurdering.id) }
        val nyeBarnFraRegister = vilkårsbehandleNyeBarn(revurdering, revurderingDto.vilkårsbehandleNyeBarn)
        barnService.opprettBarnForRevurdering(
            behandlingId = revurdering.id,
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = nyeBarnFraRegister + terminbarn,
            grunnlagsdataBarn = grunnlagsdata.grunnlagsdata.barn,
            stønadstype = fagsak.stønadstype,
        )
        val (_, metadata) = vurderingService.hentGrunnlagOgMetadata(revurdering.id)
        vurderingService.kopierVurderingerOgSamværsavtalerTilNyBehandling(
            behandlingSomSkalOppdateresId = revurdering.id,
            behandlingForGjenbrukId = forrigeBehandlingId,
            metadata = metadata,
            stønadType = fagsak.stønadstype,
        )
        val erAutomatiskRevurdering = revurderingDto.behandlingsårsak == BehandlingÅrsak.AUTOMATISK_INNTEKTSENDRING

        val saksbehandler =
            finnSaksbehandlerForRevurdering(erAutomatiskRevurdering)

        taskService.save(
            OpprettOppgaveForOpprettetBehandlingTask.opprettTask(
                OpprettOppgaveForOpprettetBehandlingTask.OpprettOppgaveTaskData(
                    behandlingId = revurdering.id,
                    saksbehandler = saksbehandler,
                    beskrivelse = if (erAutomatiskRevurdering) "Automatisk opprettet revurdering som følge av inntektskontroll" else "Revurdering i ny løsning",
                    mappeId = if (erAutomatiskRevurdering) GOSYS_MAPPE_ID_INNTEKTSKONTROLL else null,
                ),
            ),
        )
        if (!erAutomatiskRevurdering) {
            taskService.save(BehandlingsstatistikkTask.opprettPåbegyntTask(behandlingId = revurdering.id))
        }

        if (erSatsendring(revurderingDto)) {
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

    fun finnSaksbehandlerForRevurdering(erAutomatiskRevurdering: Boolean): String =
        when (erAutomatiskRevurdering) {
            true -> "S135150" // HARDKODER midlertidig saksbehandler for automatisk inntektsendring
            false -> SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        }

    @Transactional
    fun opprettAutomatiskInntektsendringTask(personIdenter: List<String>) {
        if (LeaderClient.isLeader() != true) {
            logger.info("Fant ingen leader ved oppretting av automatisk inntektsendring task")
        }
        val ukeÅr = LocalDate.now().let { "${it.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}-${it.year}" }

        personIdenter.take(10).forEach { personIdent ->

            val payload = objectMapper.writeValueAsString(PayloadBehandleAutomatiskInntektsendringTask(personIdent = personIdent, ukeÅr = ukeÅr))
            val finnesTask = taskService.finnTaskMedPayloadOgType(payload, BehandleAutomatiskInntektsendringTask.TYPE)

            if (finnesTask == null) {
                val task = BehandleAutomatiskInntektsendringTask.opprettTask(payload)
                taskService.save(task)
            }
        }
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
        revurderingDto: RevurderingDto,
    ) {
        feilHvis(
            fagsak.stønadstype != StønadType.OVERGANGSSTØNAD &&
                revurderingDto.behandlingsårsak == BehandlingÅrsak.G_OMREGNING,
        ) {
            "Kan ikke opprette revurdering med årsak g-omregning for ${fagsak.stønadstype}"
        }
        feilHvis(
            fagsak.stønadstype != StønadType.BARNETILSYN &&
                erSatsendring(revurderingDto),
        ) {
            "Kan ikke opprette revurdering med årsak satsendring for ${fagsak.stønadstype}"
        }
        if (revurderingDto.barnSomSkalFødes.isNotEmpty()) {
            feilHvis(fagsak.stønadstype == StønadType.BARNETILSYN) { "Kan ikke legge inn terminbarn for barnetilsyn" }
            feilHvis(revurderingDto.behandlingsårsak != BehandlingÅrsak.PAPIRSØKNAD && revurderingDto.behandlingsårsak != BehandlingÅrsak.NYE_OPPLYSNINGER) { "Terminbarn på revurdering kan kun legges inn for papirsøknader og nye opplysninger" }
        }
    }

    private fun erSatsendring(revurderingDto: RevurderingDto) = revurderingDto.behandlingsårsak == BehandlingÅrsak.SATSENDRING

    companion object {
        const val GOSYS_MAPPE_ID_INNTEKTSKONTROLL = 63L
    }
}
