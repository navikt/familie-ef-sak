package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.BarnTilUtplukkForOppgave
import no.nav.familie.ef.sak.iverksett.oppgaveforbarn.GjeldendeBarnRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.ef.søknad.Fødselsnummer
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class ForberedOppgaverTerminbarnService(private val gjeldendeBarnRepository: GjeldendeBarnRepository,
                                        private val behandlingRepository: BehandlingRepository,
                                        private val personService: PersonService) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun forberedOppgaverForUfødteTerminbarn(sisteKjøring: LocalDate, kjøreDato: LocalDate = LocalDate.now()) {

        val referanseDato = referanseDato(sisteKjøring)
        val gjeldendeBarn: Map<UUID, List<BarnTilUtplukkForOppgave>> =
                (gjeldendeBarnRepository.finnBarnAvGjeldendeIverksatteBehandlinger(StønadType.OVERGANGSSTØNAD, referanseDato) +
                 gjeldendeBarnRepository.finnBarnTilMigrerteBehandlinger(StønadType.OVERGANGSSTØNAD, referanseDato))
                        .filter { erTerminbarn(it) }
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
                lagOppgaverForUgyldigeTerminbarn(ugyldigeTerminbarn)
            }
        }
    }

    private fun lagOppgaverForUgyldigeTerminbarn(barnTilUtplukkForOppgave: Map<UUID, BarnTilUtplukkForOppgave>): List<OppgaveForBarn> {
        return behandlingRepository.finnEksterneIder(barnTilUtplukkForOppgave.map { it.key }.toSet()).map {
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
                .map { matchBarn(it) }
                .isNotEmpty()
    }

    private fun matchBarn(søknadBarnTermindato: LocalDate, pdlBarnFødselsdato: LocalDate): Boolean {
        return søknadBarnTermindato.minusMonths(3).isBefore(pdlBarnFødselsdato)
               && søknadBarnTermindato.plusWeeks(4).isAfter(pdlBarnFødselsdato)
    }

    private fun utgåttTermindato(termindato: LocalDate): Boolean {
        return termindato.isBefore(LocalDate.now())
    }

    private fun erTerminbarn(barn: BarnTilUtplukkForOppgave): Boolean {
        return barn.fødselsnummerBarn == null
    }

    private fun referanseDato(sisteKjøring: LocalDate): LocalDate {
        val periodeGap = ChronoUnit.DAYS.between(sisteKjøring, LocalDate.now()) - 7
        if (periodeGap > 0) {
            return LocalDate.now().minusDays(periodeGap)
        }
        return LocalDate.now()
    }
}