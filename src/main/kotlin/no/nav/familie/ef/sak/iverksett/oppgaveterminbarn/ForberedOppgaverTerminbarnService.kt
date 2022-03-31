package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.ef.iverksett.OppgaverForBarnDto
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class ForberedOppgaverTerminbarnService(private val behandlingRepository: BehandlingRepository,
                                        private val personService: PersonService,
                                        private val terminbarnRepository: TerminbarnRepository,
                                        private val iverksettClient: IverksettClient,
                                        private val fagsakService: FagsakService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun forberedOppgaverForUfødteTerminbarn(sisteKjøring: LocalDate, kjøreDato: LocalDate = LocalDate.now()) {

        val gjeldendeBarn: Map<UUID, List<TerminbarnTilUtplukkForOppgave>> =
                (terminbarnRepository
                        .finnBarnAvGjeldendeIverksatteBehandlingerUtgåtteTerminbarn())
                        .groupBy { it.behandlingId }

        logger.info("Fant totalt ${gjeldendeBarn.size} terminbarn")

        gjeldendeBarn.keys.forEach {
            val terminbarnPåSøknad = gjeldendeBarn[it]
            val utgåtteTerminbarn =
                    terminbarnPåSøknad?.filter {
                        utgåttTermindato(it.termindatoBarn)
                    } ?: emptyList()
            if (utgåtteTerminbarn.isNotEmpty()) {
                val fødselsnummerSøker = fagsakService.hentAktivIdent(gjeldendeBarn[it]?.first()?.fagsakId
                                                                      ?: error("Kunne ikke finne TerminbarnTilUtplukkForOppgave"))
                val pdlBarnUnder18år = GrunnlagsdataMapper.mapBarn(personService.hentPersonMedBarn(fødselsnummerSøker).barn)
                        .filter { it.fødsel.gjeldende().erUnder18År() }

                val ugyldigeTerminbarn = utgåtteTerminbarn.filter { !it.match(pdlBarnUnder18år) }
                val oppgaver = lagreOgLagOppgaverForUgyldigeTerminbarn(ugyldigeTerminbarn, fødselsnummerSøker)
                if (oppgaver.isNotEmpty()) {
                    sendOppgaverTilIverksett(oppgaver)
                }
            }
        }
    }

    private fun sendOppgaverTilIverksett(oppgaver: List<OppgaveForBarn>) {
        iverksettClient.sendOppgaverForTerminBarn(OppgaverForBarnDto(oppgaver))
    }

    private fun lagTerminbarnOppgave(barn: TerminbarnTilUtplukkForOppgave): TerminbarnOppgave {
        return TerminbarnOppgave(barn.fagsakId, barn.termindatoBarn)
    }

    private fun lagreOgLagOppgaverForUgyldigeTerminbarn(barnTilUtplukkForOppgave: List<TerminbarnTilUtplukkForOppgave>,
                                                        fødselsnummerSøker: String): List<OppgaveForBarn> {

        return barnTilUtplukkForOppgave.filter {
            val terminbarnOppgave = lagTerminbarnOppgave(it)
            val oppgaveFinnes =
                    terminbarnRepository.existsByFagsakIdAndTermindato(terminbarnOppgave.fagsakId, terminbarnOppgave.termindato)
            if (!oppgaveFinnes) {
                terminbarnRepository.insert(terminbarnOppgave)
            }
            !oppgaveFinnes
        }.map {
            OppgaveForBarn(it.behandlingId,
                           it.eksternFagsakId,
                           fødselsnummerSøker,
                           StønadType.OVERGANGSSTØNAD,
                           OppgaveBeskrivelse.beskrivelseUfødtTerminbarn())
        }
    }

    private fun TerminbarnTilUtplukkForOppgave.match(pdlBarn: List<BarnMedIdent>): Boolean {
        return pdlBarn.map { Fødselsnummer(it.personIdent).fødselsdato }
                .filter {
                    matchBarn(this.termindatoBarn, it)
                }.isNotEmpty()
    }

    private fun matchBarn(søknadBarnTermindato: LocalDate, pdlBarnFødselsdato: LocalDate): Boolean {
        return søknadBarnTermindato.minusMonths(3).isBefore(pdlBarnFødselsdato)
               && søknadBarnTermindato.plusWeeks(4).isAfter(pdlBarnFødselsdato)
    }

    private fun utgåttTermindato(termindato: LocalDate): Boolean {
        return termindato.isBefore(LocalDate.now())
    }

}