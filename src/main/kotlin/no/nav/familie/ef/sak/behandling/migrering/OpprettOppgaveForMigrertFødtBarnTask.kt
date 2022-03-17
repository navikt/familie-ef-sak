package no.nav.familie.ef.sak.behandling.migrering

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.OppgaveBeskrivelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.ef.iverksett.OppgaverForBarnDto
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.util.VirkedagerProvider.nesteVirkedag
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = OpprettOppgaveForMigrertFødtBarnTask.TYPE,
                     maxAntallFeil = 3,
                     settTilManuellOppfølgning = true,
                     triggerTidVedFeilISekunder = 15 * 60L,
                     beskrivelse = "Oppretter oppgave for fødte barn")
class OpprettOppgaveForMigrertFødtBarnTask(
        private val iverksettClient: IverksettClient,
        private val behandlingService: BehandlingService,
        private val tilkjentYtelseService: TilkjentYtelseService,
        private val grunnlagsdataService: GrunnlagsdataService
) : AsyncTaskStep {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val data = objectMapper.readValue<OpprettOppgaveForMigrertFødtBarnTaskData>(task.payload)
        val fagsakId = data.fagsakId
        val behandlingId = sisteIverksatteBehandling(fagsakId)
        val sisteUtbetalingsdato = finnSisteUtbetalingsdato(behandlingId)
        if (sisteUtbetalingsdato == null) {
            logger.info("fagsak=$fagsakId behandling=$behandlingId har ingen utbetalingsperioder")
            return
        }
        lagOgSendOppgaver(fagsakId, behandlingId, sisteUtbetalingsdato, data)
    }

    private fun lagOgSendOppgaver(fagsakId: UUID,
                                  behandlingId: UUID,
                                  sisteUtbetalingsdato: LocalDate,
                                  data: OpprettOppgaveForMigrertFødtBarnTaskData) {
        val oppgaver = lagOppgaver(behandlingId, data, sisteUtbetalingsdato)
        if (oppgaver.isNotEmpty()) {
            logger.info("Sender ${oppgaver.size} for fagsakId=$fagsakId")
            iverksettClient.sendOppgaverForBarn(OppgaverForBarnDto(oppgaver))
        }
    }

    private fun sisteIverksatteBehandling(fagsakId: UUID): UUID {
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                         ?: error("Finner ikke iverksatt behandling for fagsak=$fagsakId")
        return behandling.id
    }

    private fun finnSisteUtbetalingsdato(behandlingId: UUID): LocalDate? {
        return tilkjentYtelseService.hentForBehandling(behandlingId).andelerTilkjentYtelse.maxOfOrNull { it.stønadTom }
    }

    private fun lagOppgaver(behandlingId: UUID,
                            data: OpprettOppgaveForMigrertFødtBarnTaskData,
                            sisteUtbetalingsdato: LocalDate): List<OppgaveForBarn> {
        val kjenteFødselsdatoer = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.barn
                .flatMap { it.fødsel.mapNotNull(Fødsel::fødselsdato) }
        return data.barn.mapNotNull {
            val fødselsdato = it.fødselsdato
            if (fødselsdato == null) {
                logger.warn("Kan ikke opprette oppgave for barn uten fødselsdato personident=${it.personIdent}")
                return@mapNotNull null
            }
            if (kjenteFødselsdatoer.contains(fødselsdato)) {
                logger.info("Fødselsdato=$fødselsdato finnes allerede i grunnlagsdata, oppretter ikke oppgave")
                return@mapNotNull null
            }
            datoOgBeskrivelse(fødselsdato, sisteUtbetalingsdato)
                    .map { datoOgBeskrivelse ->
                        OppgaveForBarn(
                                behandlingId = behandlingId,
                                eksternFagsakId = data.eksternFagsakId,
                                personIdent = data.personIdent,
                                stønadType = data.stønadType.kontraktType,
                                beskrivelse = datoOgBeskrivelse.second,
                                aktivFra = datoOgBeskrivelse.first
                        )
                    }
        }.flatten()
    }

    /**
     * Oppretter datoer for 6 og 12 måneder
     * Skal ikke opprette noen oppføglningsoppgaver hvis datoet for når barnet fyller 1 år er før siste utbetalingsperioden
     */
    private fun datoOgBeskrivelse(fødselsdato: LocalDate, sisteUtbetalingsdato: LocalDate): List<Pair<LocalDate, String>> {
        val beskrivelseBarnBlirSeksMnd = OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd()
        val beskrivelseBarnFyllerEttÅr = OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr()
        val datoOm1År = nesteVirkedagForDatoMinus1Uke(fødselsdato.plusYears(1))
        if (sisteUtbetalingsdato < datoOm1År) {
            logger.info("Dato for sisteUtbetalingsdato=$sisteUtbetalingsdato er før barnet fyller 1 år = $datoOm1År")
            return emptyList()
        }
        return listOf(nesteVirkedagForDatoMinus1Uke(fødselsdato.plusMonths(6)) to beskrivelseBarnBlirSeksMnd,
                      datoOm1År to beskrivelseBarnFyllerEttÅr)
                .filter { it.first > LocalDate.now() }
    }

    /**
     * Setter dato minus 1 uke for å sette fristFerdigstilling til 1 uke før barnet fyller år
     * For å unngå att fristen settes på en helgdag, så brukes nesteVirkedag
     */
    private fun nesteVirkedagForDatoMinus1Uke(localDate: LocalDate): LocalDate {
        return nesteVirkedag(localDate.minusWeeks(1).minusDays(1))
    }

    companion object {

        const val TYPE = "opprettOppgaveForMigrertFødtBarn"

        fun opprettOppgave(fagsak: Fagsak, nyeBarn: List<BarnMinimumDto>): Task {
            return Task(TYPE, objectMapper.writeValueAsString(
                    OpprettOppgaveForMigrertFødtBarnTaskData(fagsakId = fagsak.id,
                                                             eksternFagsakId = fagsak.eksternId.id,
                                                             stønadType = fagsak.stønadstype,
                                                             personIdent = fagsak.hentAktivIdent(),
                                                             nyeBarn)))
        }
    }
}

data class OpprettOppgaveForMigrertFødtBarnTaskData(val fagsakId: UUID,
                                                    val eksternFagsakId: Long,
                                                    val stønadType: Stønadstype,
                                                    val personIdent: String,
                                                    val barn: List<BarnMinimumDto>)
