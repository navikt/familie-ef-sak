package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ForberedOppgaverTerminbarnService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val terminbarnRepository: TerminbarnRepository,
    private val taskService: TaskService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun forberedOppgaverForUfødteTerminbarn() {
        val gjeldendeBarn: Map<UUID, List<TerminbarnTilUtplukkForOppgave>> = terminbarnRepository
            .finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD)
            .groupBy { it.behandlingId }

        logger.info("Fant totalt ${gjeldendeBarn.size} terminbarn")
        val oppgaver = gjeldendeBarn.values.map { terminbarnPåSøknad ->
            val fødselsnummerSøker = fagsakService.hentAktivIdent(terminbarnPåSøknad.first().fagsakId)
            val pdlBarn = pdlBarn(fødselsnummerSøker)
            val ugyldigeTerminbarn = terminbarnPåSøknad.filterNot { it.match(pdlBarn) }
            lagreOgMapTilOppgaverForUgyldigeTerminbarn(ugyldigeTerminbarn, fødselsnummerSøker)
        }.flatten()
        logger.info("Fant ${oppgaver.size} oppgaver for ugyldige terminbarn.")
        oppgaver.forEach { oppgave ->
            logger.info("Laget oppgave for behandlingID=${oppgave.behandlingId}.")
        }
        opprettTaskerForOppgaver(oppgaver)
    }

    private fun pdlBarn(fødselsnummerSøker: String): List<PdlPersonForelderBarn> =
        personService.hentPersonMedBarn(fødselsnummerSøker).barn.values.toList()

    private fun opprettTaskerForOppgaver(oppgaver: List<OppgaveForBarn>) {
        oppgaver.forEach { taskService.save(OpprettOppgaveTerminbarnTask.opprettTask(it)) }
    }

    private fun lagreOgMapTilOppgaverForUgyldigeTerminbarn(
        barnTilUtplukkForOppgave: List<TerminbarnTilUtplukkForOppgave>,
        fødselsnummerSøker: String
    ): List<OppgaveForBarn> {
        return barnTilUtplukkForOppgave
            .map {
                terminbarnRepository.insert(it.tilTerminbarnOppgave())
                OppgaveForBarn(
                    it.behandlingId,
                    it.eksternFagsakId,
                    fødselsnummerSøker,
                    StønadType.OVERGANGSSTØNAD,
                    OppgaveBeskrivelse.beskrivelseUfødtTerminbarn()
                )
            }
    }
}

private fun matchBarn(søknadBarnTermindato: LocalDate, pdlBarnFødselsdato: LocalDate): Boolean {
    return søknadBarnTermindato.minusMonths(3).isBefore(pdlBarnFødselsdato) &&
        søknadBarnTermindato.plusWeeks(4).isAfter(pdlBarnFødselsdato)
}

private fun TerminbarnTilUtplukkForOppgave.match(pdlPersonForelderBarn: List<PdlPersonForelderBarn>): Boolean {
    return pdlPersonForelderBarn
        .map { it.fødsel.gjeldende().fødselsdato }
        .any { matchBarn(this.termindatoBarn, it ?: error("Fødselsdato er null")) }
}

data class TerminbarnTilUtplukkForOppgave(
    val behandlingId: UUID,
    val fagsakId: UUID,
    val eksternFagsakId: Long,
    val termindatoBarn: LocalDate
)

private fun TerminbarnTilUtplukkForOppgave.tilTerminbarnOppgave(): TerminbarnOppgave {
    return TerminbarnOppgave(fagsakId = this.fagsakId, termindato = this.termindatoBarn)
}
