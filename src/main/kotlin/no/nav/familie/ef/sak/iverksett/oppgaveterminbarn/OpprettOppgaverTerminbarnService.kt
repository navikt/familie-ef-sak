package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service

@Service
class OpprettOppgaverTerminbarnService(
    private val oppgaveService: OppgaveService,
) {
    private val logger = Logg.getLogger(this::class)

    fun opprettOppgaveForTerminbarn(oppgaveForBarn: OppgaveForBarn) {
        val opprettetOppgaveId =
            oppgaveService.opprettOppgaveUtenÅLagreIRepository(
                behandlingId = oppgaveForBarn.behandlingId,
                oppgavetype = Oppgavetype.InnhentDokumentasjon,
                fristFerdigstillelse = oppgaveForBarn.aktivFra,
                beskrivelse = oppgaveForBarn.beskrivelse,
                tilordnetNavIdent = null,
            )
        logger.info("Opprettet oppgave med oppgaveId=$opprettetOppgaveId for behandling=${oppgaveForBarn.behandlingId}")
    }
}
