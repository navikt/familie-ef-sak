package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.iverksett.infrastruktur.json.BarnDto
import no.nav.familie.ef.mottak.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.dto.BeslutteVedtakDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.blankett.JournalførBlankettTask
import no.nav.familie.ef.sak.integration.dto.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.mapper.BarnMatcher
import no.nav.familie.ef.sak.mapper.MatchetBarn
import no.nav.familie.ef.sak.nare.specifications.Regel
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.repository.VedtaksbrevRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.PersisterGrunnlagsdataService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.TotrinnskontrollService
import no.nav.familie.ef.sak.service.VedtaksbrevService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.task.IverksettMotOppdragTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.task.OpprettOppgaveTask.OpprettOppgaveTaskData
import no.nav.familie.ef.sak.task.PollStatusFraIverksettTask
import no.nav.familie.kontrakter.ef.felles.BehandlingResultat
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.felles.Vedtak
import no.nav.familie.kontrakter.ef.iverksett.AktivitetskravDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.FagsakdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.SøkerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.domene.TaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.BehandlingType as BehandlingTypeKontakter

@Service
class BeslutteVedtakSteg(private val taskRepository: TaskRepository,
                         private val fagsakService: FagsakService,
                         private val oppgaveService: OppgaveService,
                         private val featureToggleService: FeatureToggleService,
                         private val iverksettClient: IverksettClient,
                         private val arbeidsfordelingService: ArbeidsfordelingService,
                         private val vurderingService: VurderingService,
                         private val søknadService: SøknadService,
                         private val vedtakService: VedtakService,
                         private val persisterGrunnlagsdataService: PersisterGrunnlagsdataService,
                         private val totrinnskontrollService: TotrinnskontrollService,
                         private val vedtaksbrevRepository: VedtaksbrevRepository,
                         private val vedtaksbrevService: VedtaksbrevService) : BehandlingSteg<BeslutteVedtakDto> {

    override fun validerSteg(behandling: Behandling) {
        if (behandling.steg != stegType()) {
            throw Feil("Behandling er i feil steg=${behandling.steg}")
        }
    }

    override fun utførOgReturnerNesteSteg(behandling: Behandling, data: BeslutteVedtakDto): StegType {
        val saksbehandler = totrinnskontrollService.lagreTotrinnskontrollOgReturnerBehandler(behandling, data)

        ferdigstillOppgave(behandling)

        return if (data.godkjent) {
            if (behandling.type != BehandlingType.BLANKETT) {
                val fil = vedtaksbrevService.lagreEndeligBrev(behandling.id).pdf
                require(fil != null) { "For å iverksette må det finnes en pdf" }
                if (featureToggleService.isEnabled("familie.ef.sak.brukEFIverksett")) {

                    //TODO Hardkodet tillsvidare verdier for behandlingResultat og behandlingÅrsak
                    val behandlingsdetaljer = BehandlingsdetaljerDto(behandlingId = behandling.id,
                                                                     behandlingType = BehandlingTypeKontakter.valueOf(behandling.type.name),
                                                                     behandlingResultat = BehandlingResultat.FERDIGSTILT,
                                                                     behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                                                                     eksternId = behandling.eksternId.id)

                    val fagsak = fagsakService.hentFaksakForBehandling(behandling.id)
                    val fagsakdetaljerDto = FagsakdetaljerDto(fagsakId = fagsak.id,
                                                              eksternId = fagsak.eksternId.id,
                                                              stønadstype = StønadType.OVERGANGSSTØNAD)


                    val søknad = søknadService.hentOvergangsstønad(behandling.id)
                    val grunnlagsdata = persisterGrunnlagsdataService.hentGrunnlagsdata(behandling.id, søknad)
                    val alleBarn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, grunnlagsdata.barn)
                    val navEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(søknad.fødselsnummer)
                    val harSagtOpp = vurderingService.hentEllerOpprettVurderinger(behandling.id).vurderinger
                                             .find { it.vilkårType === VilkårType.SAGT_OPP_ELLER_REDUSERT }
                                             ?.let { vilkårsvurderingDto ->
                                                 vilkårsvurderingDto.delvilkårsvurderinger
                                                         .any { delvilkår ->
                                                             val vurdering =
                                                                     delvilkår.vurderinger.find { vurdering -> vurdering.regelId == RegelId.SAGT_OPP_ELLER_REDUSERT }
                                                             vurdering?.svar?.let {
                                                                 when (it) {
                                                                     SvarId.JA -> true
                                                                     SvarId.NEI -> false
                                                                     else -> error("Jajaj")

                                                                 }
                                                             } ?: error("detta var inte så bra")
                                                         }
                                             } ?: error("detta var inte så bra")

                    val søkerDto = SøkerDto(kode6eller7 = grunnlagsdata.søker.adressebeskyttelse?.let {
                        listOf(AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                               AdressebeskyttelseGradering.FORTROLIG).contains(it.gradering)
                    }
                                                          ?: false,
                                            personIdent = fagsak.hentAktivIdent(),
                                            barn = alleBarn.map {
                                                BarnDto(personIdent = it.fødselsnummer,
                                                        termindato = it.søknadsbarn.fødselTermindato)
                                            },
                                            aktivitetskrav = AktivitetskravDto(
                                                    aktivitetspliktInntrefferDato = LocalDate.now(),
                                                    harSagtOppArbeidsforhold = harSagtOpp),
                                            tilhørendeEnhet = navEnhet)

                    val vedtak = vedtakService.hentVedtak(behandling.id)
                    val vedtakDto = VedtaksdetaljerDto(vedtak = Vedtak.valueOf(vedtak.resultatType.name),
                                                       vedtaksdato =,
                                                       opphørÅrsak = null,
                                                       saksbehandlerId = "",
                                                       beslutterId = "",
                                                       tilkjentYtelse =,
                                                       inntekter = listOf())

                    val iverksettDto =
                            IverksettDto(behandling = behandlingsdetaljer, fagsak = fagsakdetaljerDto, søker = søkerDto)
                    iverksettClient.iverksett(iverksettDto, fil)
                    opprettPollForStatusOppgave(behandling.id)
                    StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT
                } else {
                    opprettTaskForIverksettMotOppdrag(behandling)
                    stegType().hentNesteSteg(behandling.type)
                }
            } else {
                opprettTaskForJournalførBlankett(behandling)
                stegType().hentNesteSteg(behandling.type)
            }
        } else {
            vedtaksbrevRepository.deleteById(behandling.id)
            opprettBehandleUnderkjentVedtakOppgave(behandling, saksbehandler)
            StegType.SEND_TIL_BESLUTTER
        }
    }

    private fun ferdigstillOppgave(behandling: Behandling) {
        val oppgavetype = Oppgavetype.GodkjenneVedtak
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)?.let {
            taskRepository.save(FerdigstillOppgaveTask.opprettTask(behandlingId = behandling.id, oppgavetype))
        }
    }

    private fun opprettBehandleUnderkjentVedtakOppgave(behandling: Behandling, navIdent: String) {
        taskRepository.save(OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTaskData(behandlingId = behandling.id,
                                       oppgavetype = Oppgavetype.BehandleUnderkjentVedtak,
                                       tilordnetNavIdent = navIdent)))
    }

    private fun opprettTaskForIverksettMotOppdrag(behandling: Behandling) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        taskRepository.save(IverksettMotOppdragTask.opprettTask(behandling, aktivIdent))
    }

    private fun opprettTaskForJournalførBlankett(behandling: Behandling) {
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        taskRepository.save(JournalførBlankettTask.opprettTask(behandling, aktivIdent))
    }

    private fun opprettPollForStatusOppgave(behandlingId: UUID) {
        taskRepository.save(PollStatusFraIverksettTask.opprettTask(behandlingId))
    }

    override fun stegType(): StegType {
        return StegType.BESLUTTE_VEDTAK
    }

    override fun utførSteg(behandling: Behandling, data: BeslutteVedtakDto) {
        error("Bruker utførOgReturnerNesteSteg")
    }

    override fun settInnHistorikk(): Boolean = false
}