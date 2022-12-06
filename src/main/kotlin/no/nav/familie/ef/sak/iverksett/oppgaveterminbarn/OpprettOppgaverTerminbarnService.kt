package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OpprettOppgaverTerminbarnService(
    private val oppgaveService: OppgaveService,
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun opprettOppgaveForTerminbarn(oppgaveForBarn: OppgaveForBarn) {
        val opprettetOppgaveId = oppgaveService.opprettOppgaveUtenÅLagreIRepository(
            behandlingId = oppgaveForBarn.behandlingId,
            oppgavetype = Oppgavetype.InnhentDokumentasjon,
            fristFerdigstillelse = oppgaveForBarn.aktivFra,
            beskrivelse = oppgaveForBarn.beskrivelse,
            tilordnetNavIdent = null,
            mappeId = enhetForInnhentDokumentasjon(oppgaveForBarn.personIdent).enhetId.toLong()
        )
        logger.info("Opprettet oppgave med oppgaveId=$opprettetOppgaveId for behandling=${oppgaveForBarn.behandlingId}")
    }

    private fun enhetForInnhentDokumentasjon(personIdent: String): Arbeidsfordelingsenhet {
        return personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(personIdent).first()
    }
}
