package no.nav.familie.ef.sak.forvaltning.aktivitetsplikt

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.PeriodiskAktivitetspliktBrevDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.PropertiesWrapper
import no.nav.familie.prosessering.domene.Task
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Year
import java.util.Properties

@Service
@TaskStepBeskrivelse(
    taskStepType = SendAktivitetspliktBrevTilIverksettTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Automatisk utsend brev for innhenting av aktivitetsplikt",
)
class SendAktivitetspliktBrevTilIverksettTask(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val oppgaveService: OppgaveService,
    private val frittståendeBrevService: FrittståendeBrevService,
    private val personopplysningerService: PersonopplysningerService,
    private val iverksettClient: IverksettClient,
    private val arbeidsfordelingService: ArbeidsfordelingService,
) : AsyncTaskStep {
    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val payload = jsonMapper.readValue<AutomatiskBrevAktivitetspliktPayload>(task.payload)
        val oppgave = oppgaveService.hentOppgave(payload.oppgaveId)
        val ident = OppgaveUtil.finnPersonidentForOppgave(oppgave) ?: throw Feil("Fant ikke ident for oppgave=${oppgave.id}")
        val fagsaker = fagsakService.finnFagsaker(setOf(ident))

        validerHarIkkeVergemål(ident, oppgave)
        validerHarFagsakOgBehandling(fagsaker, oppgave)

        val visningsnavn = personopplysningerService.hentGjeldeneNavn(listOf(ident)).getValue(ident)
        val brev = frittståendeBrevService.lagBrevForInnhentingAvAktivitetsplikt(visningsnavn, ident)
        val journalFørendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(ident)
        val fagsak = utledFagsak(fagsaker)

        logger.info("Starter utsending aktivitetsplikt for oppgaveId=${oppgave.id} og eksternFagsakId=${fagsak.eksternId}")

        iverksettClient.håndterUtsendingAvAktivitetspliktBrev(
            PeriodiskAktivitetspliktBrevDto(
                fil = brev,
                oppgaveId = payload.oppgaveId,
                personIdent = ident,
                eksternFagsakId = fagsak.eksternId,
                journalførendeEnhet = journalFørendeEnhet,
                gjeldendeÅr = payload.gjeldendeÅr,
                stønadType = fagsak.stønadstype,
            ),
        )
    }

    private fun validerHarIkkeVergemål(
        ident: String,
        opggave: Oppgave,
    ) {
        val personopplysninger = personopplysningerService.hentPersonopplysningerFraRegister(ident)
        val harVerge = personopplysninger.vergemål.isNotEmpty()
        feilHvis(harVerge) {
            "Kan ikke automatisk sende brev for oppgaveId=${opggave.id}. Brev om innhenting av aktivitetsplikt skal ikke sendes automatisk fordi bruker har vergemål. Saken må følges opp manuelt og tasken kan avvikshåndteres."
        }
    }

    private fun utledFagsak(fagsaker: List<Fagsak>): Fagsak = fagsaker.firstOrNull { it.stønadstype == StønadType.OVERGANGSSTØNAD } ?: fagsaker.first()

    private fun validerHarFagsakOgBehandling(
        fagsaker: List<Fagsak>,
        oppgave: Oppgave,
    ) {
        feilHvis(fagsaker.isEmpty()) {
            "Fant ingen fagsak for oppgave=${oppgave.id}"
        }

        feilHvis(fagsaker.none { behandlingService.finnesBehandlingForFagsak(it.id) }) {
            "Fant ingen behandling for oppgave=${oppgave.id}"
        }
    }

    companion object {
        fun opprettTask(
            oppgaveId: Long,
            gjeldendeÅr: Year,
        ): Task {
            val payload = opprettTaskPayload(oppgaveId, gjeldendeÅr)

            val properties =
                Properties().apply {
                    setProperty("oppgaveId", oppgaveId.toString())
                    setProperty(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
                }

            return Task(TYPE, payload).copy(metadataWrapper = PropertiesWrapper(properties))
        }

        fun opprettTaskPayload(
            oppgaveId: Long,
            gjeldendeÅr: Year,
        ): String =
            jsonMapper.writeValueAsString(
                AutomatiskBrevAktivitetspliktPayload(oppgaveId, gjeldendeÅr),
            )

        const val TYPE = "SendAktivitetspliktBrevTilIverksettTask"
    }
}

data class AutomatiskBrevAktivitetspliktPayload(
    val oppgaveId: Long,
    val gjeldendeÅr: Year,
)
