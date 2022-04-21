package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.ef.iverksett.OppgaverForBarnDto
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ForberedOppgaverTerminbarnService(private val personService: PersonService,
                                        private val fagsakService: FagsakService,
                                        private val terminbarnRepository: TerminbarnRepository,
                                        private val iverksettClient: IverksettClient) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun forberedOppgaverForUfødteTerminbarn() {

        val gjeldendeBarn: Map<UUID, List<TerminbarnTilUtplukkForOppgave>> = terminbarnRepository
                .finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn(StønadType.OVERGANGSSTØNAD)
                .groupBy { it.behandlingId }

        logger.info("Fant totalt ${gjeldendeBarn.size} terminbarn")

        gjeldendeBarn.values.forEach { terminbarnPåSøknad ->
            val fødselsnummerSøker = fagsakService.hentAktivIdent(terminbarnPåSøknad.first().fagsakId)
            val pdlBarn = pdlBarn(fødselsnummerSøker)
            val ugyldigeTerminbarn = terminbarnPåSøknad.filter { !it.match(pdlBarn) }
            val oppgaver = lagreOgLagOppgaverForUgyldigeTerminbarn(ugyldigeTerminbarn, fødselsnummerSøker)
            if (oppgaver.isNotEmpty()) {
                sendOppgaverTilIverksett(oppgaver)
            }
        }
    }

    private fun pdlBarn(fødselsnummerSøker: String): List<BarnMedIdent> =
            GrunnlagsdataMapper.mapBarn(personService.hentPersonMedBarn(fødselsnummerSøker).barn)

    private fun sendOppgaverTilIverksett(oppgaver: List<OppgaveForBarn>) {
        iverksettClient.sendOppgaverForTerminBarn(OppgaverForBarnDto(oppgaver))
    }

    private fun lagreOgLagOppgaverForUgyldigeTerminbarn(barnTilUtplukkForOppgave: List<TerminbarnTilUtplukkForOppgave>,
                                                        fødselsnummerSøker: String): List<OppgaveForBarn> {

        return barnTilUtplukkForOppgave
                .filter { !terminbarnRepository.existsByFagsakIdAndTermindato(it.fagsakId, it.termindatoBarn) }
                .map {
                    terminbarnRepository.insert(it.tilTerminbarnOppgave())
                    OppgaveForBarn(it.behandlingId,
                                   it.eksternFagsakId,
                                   fødselsnummerSøker,
                                   StønadType.OVERGANGSSTØNAD,
                                   OppgaveBeskrivelse.beskrivelseUfødtTerminbarn())
                }
    }
}

private fun matchBarn(søknadBarnTermindato: LocalDate, pdlBarnFødselsdato: LocalDate): Boolean {
    return søknadBarnTermindato.minusMonths(3).isBefore(pdlBarnFødselsdato)
           && søknadBarnTermindato.plusWeeks(4).isAfter(pdlBarnFødselsdato)
}

private fun TerminbarnTilUtplukkForOppgave.match(pdlBarn: List<PdlBarn>): Boolean {
    return pdlBarn
            .mapNotNull { it.fødsel.gjeldende().fødselsdato }
            .any { matchBarn(this.termindatoBarn, it) }
}
            .any { matchBarn(this.termindatoBarn, it) }
}

private fun TerminbarnTilUtplukkForOppgave.tilTerminbarnOppgave(): TerminbarnOppgave {
    return TerminbarnOppgave(fagsakId = this.fagsakId, termindato = this.termindatoBarn)
}