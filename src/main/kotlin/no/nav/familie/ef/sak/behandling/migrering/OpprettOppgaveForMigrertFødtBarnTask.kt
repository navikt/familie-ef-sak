package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.AktivitetspliktigAlder
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettOppfølgingsoppgaveForBarnFyltÅrTask
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OpprettOppgavePayload
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = OpprettOppgaveForMigrertFødtBarnTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Oppretter oppgave for fødte barn",
)
class OpprettOppgaveForMigrertFødtBarnTask(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val taskService: TaskService,
) : AsyncTaskStep {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val data = jsonMapper.readValue<OpprettOppgaveForMigrertFødtBarnTaskData>(task.payload)
        val fagsakId = data.fagsakId
        val behandlingId = sisteIverksatteBehandling(fagsakId)
        val sisteUtbetalingsdato = finnSisteUtbetalingsdato(behandlingId)
        if (sisteUtbetalingsdato == null) {
            logger.info("fagsak=$fagsakId behandling=$behandlingId har ingen utbetalingsperioder")
            return
        }
        lagOgSendOppgaver(behandlingId, sisteUtbetalingsdato, data)
    }

    private fun lagOgSendOppgaver(
        behandlingId: UUID,
        sisteUtbetalingsdato: LocalDate,
        data: OpprettOppgaveForMigrertFødtBarnTaskData,
    ) {
        val oppgaverForBarn = lagOppgaver(behandlingId, data, sisteUtbetalingsdato)

        val opprettOppfølgingsoppgaveForBarnFyltÅrTasks =
            oppgaverForBarn.map {
                OpprettOppfølgingsoppgaveForBarnFyltÅrTask.opprettTask(
                    OpprettOppgavePayload(
                        behandlingId = it.behandlingId,
                        barnPersonIdent = it.personIdent,
                        søkerPersonIdent = data.personIdent,
                        alder = it.alder,
                        aktivFra = it.aktivFra,
                    ),
                )
            }

        if (opprettOppfølgingsoppgaveForBarnFyltÅrTasks.isNotEmpty()) {
            taskService.saveAll(opprettOppfølgingsoppgaveForBarnFyltÅrTasks)
        }
    }

    private fun sisteIverksatteBehandling(fagsakId: UUID): UUID {
        val behandling =
            behandlingService.finnSisteIverksatteBehandling(fagsakId)
                ?: error("Finner ikke iverksatt behandling for fagsak=$fagsakId")
        return behandling.id
    }

    private fun finnSisteUtbetalingsdato(behandlingId: UUID): LocalDate? = tilkjentYtelseService.hentForBehandling(behandlingId).andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }

    private fun lagOppgaver(
        behandlingId: UUID,
        data: OpprettOppgaveForMigrertFødtBarnTaskData,
        sisteUtbetalingsdato: LocalDate,
    ): List<OppgaveForBarn> {
        val kjenteFødselsdatoer =
            grunnlagsdataService
                .hentGrunnlagsdata(behandlingId)
                .grunnlagsdata.barn
                .flatMap { it.fødsel.mapNotNull(Fødsel::fødselsdato) }
        return data.barn
            .mapNotNull {
                val fødselsdato = it.fødselsdato
                if (fødselsdato == null) {
                    logger.warn("Kan ikke opprette oppgave for barn uten fødselsdato personident=${it.personIdent}")
                    return@mapNotNull null
                }
                if (kjenteFødselsdatoer.contains(fødselsdato)) {
                    logger.info("Fødselsdato=$fødselsdato finnes allerede i grunnlagsdata, oppretter ikke oppgave")
                    return@mapNotNull null
                }
                datoOgAlder(fødselsdato, sisteUtbetalingsdato)
                    .map { datoOgBeskrivelse ->
                        OppgaveForBarn(
                            behandlingId = behandlingId,
                            personIdent = it.personIdent,
                            stønadType = data.stønadType,
                            aktivFra = datoOgBeskrivelse.first,
                            alder = datoOgBeskrivelse.second,
                        )
                    }
            }.flatten()
    }

    /**
     * Oppretter datoer for 6 og 12 måneder
     * Skal ikke opprette noen oppføglningsoppgaver hvis datoet for når barnet fyller 1 år er før siste utbetalingsperioden
     */
    private fun datoOgAlder(
        fødselsdato: LocalDate,
        sisteUtbetalingsdato: LocalDate,
    ): List<Pair<LocalDate, AktivitetspliktigAlder>> {
        val datoOm1År = nesteVirkedagForDatoMinus1Uke(fødselsdato.plusYears(1))
        if (sisteUtbetalingsdato < datoOm1År) {
            logger.info("Dato for sisteUtbetalingsdato=$sisteUtbetalingsdato er før barnet fyller 1 år = $datoOm1År")
            return emptyList()
        }
        return listOf(
            nesteVirkedagForDatoMinus1Uke(fødselsdato.plusMonths(6)) to AktivitetspliktigAlder.SEKS_MND,
            datoOm1År to AktivitetspliktigAlder.ETT_ÅR,
        ).filter { it.first > LocalDate.now() }
    }

    /**
     * Setter dato minus 1 uke for å sette fristFerdigstilling til 1 uke før barnet fyller år
     * For å unngå att fristen settes på en helgdag, så brukes nesteVirkedag
     */
    private fun nesteVirkedagForDatoMinus1Uke(localDate: LocalDate): LocalDate = nesteVirkedag(localDate.minusWeeks(1).minusDays(1))

    companion object {
        const val TYPE = "opprettOppgaveForMigrertFødtBarn"

        fun opprettOppgave(
            fagsak: Fagsak,
            nyeBarn: List<BarnMinimumDto>,
        ): Task =
            Task(
                TYPE,
                jsonMapper.writeValueAsString(
                    OpprettOppgaveForMigrertFødtBarnTaskData(
                        fagsakId = fagsak.id,
                        eksternFagsakId = fagsak.eksternId,
                        stønadType = fagsak.stønadstype,
                        personIdent = fagsak.hentAktivIdent(),
                        barn = nyeBarn,
                    ),
                ),
            )
    }
}

data class OpprettOppgaveForMigrertFødtBarnTaskData(
    val fagsakId: UUID,
    val eksternFagsakId: Long,
    val stønadType: StønadType,
    val personIdent: String,
    val barn: List<BarnMinimumDto>,
)

data class OppgaveForBarn(
    val behandlingId: UUID,
    val personIdent: String,
    val stønadType: StønadType,
    val aktivFra: LocalDate? = null,
    val alder: AktivitetspliktigAlder,
)
