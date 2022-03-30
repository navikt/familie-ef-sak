package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.dto.EksternId
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.BarnTilUtplukkForOppgave
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
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ForberedOppgaverTerminbarnService(private val behandlingRepository: BehandlingRepository,
                                        private val personService: PersonService,
                                        private val terminbarnRepository: TerminbarnRepository,
                                        private val iverksettClient: IverksettClient,
                                        private val fagsakRepository: FagsakRepository) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun forberedOppgaverForUfødteTerminbarn(sisteKjøring: LocalDate, kjøreDato: LocalDate = LocalDate.now()) {

        val gjeldendeBarn: Map<UUID, List<BarnTilUtplukkForOppgave>> =
                (terminbarnRepository
                        .finnBarnAvGjeldendeIverksatteBehandlingerKunTerminbarn(StønadType.OVERGANGSSTØNAD))
                        .groupBy { it.behandlingId }

        logger.info("Fant totalt ${gjeldendeBarn.size} terminbarn")

        gjeldendeBarn.keys.forEach {
            val terminbarnPåSøknad = gjeldendeBarn[it]
            val utgåtteTerminbarn =
                    terminbarnPåSøknad?.filter {
                        utgåttTermindato(it.termindatoBarn ?: error("Barn har ingen termindato"))
                    } ?: emptyList()
            if (utgåtteTerminbarn.isNotEmpty()) {
                val fødselsnummerSøker = gjeldendeBarn[it]?.first()?.fødselsnummerSøker
                                         ?: error("Kunne ikke finne barn for behandlingID=$it")
                val pdlBarnUnder18år = GrunnlagsdataMapper.mapBarn(personService.hentPersonMedBarn(fødselsnummerSøker).barn)
                        .filter { it.fødsel.gjeldende().erUnder18År() }

                val ugyldigeTerminbarn = utgåtteTerminbarn.filter { !it.match(pdlBarnUnder18år) }.associateBy { it.behandlingId }
                val oppgaver = lagreOgLagOppgaverForUgyldigeTerminbarn(ugyldigeTerminbarn)
                if (oppgaver.isNotEmpty()) {
                    sendOppgaverTilIverksett(oppgaver)
                }
            }
        }
    }

    private fun sendOppgaverTilIverksett(oppgaver: List<OppgaveForBarn>) {
        iverksettClient.sendOppgaverForTerminBarn(OppgaverForBarnDto(oppgaver))
    }

    private fun lagTerminbarnOppgave(eksternId: EksternId, barn: BarnTilUtplukkForOppgave): TerminbarnOppgave {
        val fagsakId = fagsakRepository.finnMedEksternId(eksternId.eksternFagsakId)?.id
                       ?: error("Kunne ikke finne fagsak med eksternFagsakID:${eksternId.eksternFagsakId}")
        val termindato = barn.termindatoBarn
                         ?: error("Termindato for barn er ikke satt. Dette skal ikke skje.")
        return TerminbarnOppgave(fagsakId, termindato)
    }

    private fun lagreOgLagOppgaverForUgyldigeTerminbarn(barnTilUtplukkForOppgave: Map<UUID, BarnTilUtplukkForOppgave>): List<OppgaveForBarn> {

        return behandlingRepository.finnEksterneIder(barnTilUtplukkForOppgave.map { it.key }.toSet()).filter {
            val terminbarnOppgave = lagTerminbarnOppgave(
                    it,
                    barnTilUtplukkForOppgave[it.behandlingId]
                    ?: error("Kunne ikke finne behandlingsId fra utplukk. Dette skal ikke skje."),
            )
            val oppgaveFinnes =
                    terminbarnRepository.existsByFagsakIdAndTermindato(terminbarnOppgave.fagsakId, terminbarnOppgave.termindato)
            if (!oppgaveFinnes) {
                terminbarnRepository.insert(terminbarnOppgave)
            }
            !oppgaveFinnes
        }.map {
            val utplukketBarn = barnTilUtplukkForOppgave[it.behandlingId]
                                ?: error("Kunne ikke finne behandlingsId fra utplukk. Dette skal ikke skje.")
            OppgaveForBarn(it.behandlingId,
                           it.eksternFagsakId,
                           utplukketBarn.fødselsnummerSøker,
                           StønadType.OVERGANGSSTØNAD,
                           OppgaveBeskrivelse.beskrivelseUfødtTerminbarn())
        }
    }

    private fun BarnTilUtplukkForOppgave.match(pdlBarn: List<BarnMedIdent>): Boolean {
        return pdlBarn.map { Fødselsnummer(it.personIdent).fødselsdato }
                .filter {
                    matchBarn(this.termindatoBarn
                              ?: error("Kunne ikke finne termindato"), it)
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