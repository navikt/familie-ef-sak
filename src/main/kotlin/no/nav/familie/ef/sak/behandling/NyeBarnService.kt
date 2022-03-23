package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.migrering.OpprettOppgaveForMigrertFødtBarnTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarnÅrsak
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.TaskRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class NyeBarnService(private val behandlingService: BehandlingService,
                     private val fagsakService: FagsakService,
                     private val personService: PersonService,
                     private val barnService: BarnService,
                     private val taskRepository: TaskRepository) {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @Deprecated("bruk finnNyeEllerTidligereFødteBarn")
    fun finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(personIdent: PersonIdent): List<String> {
        val personIdenter = personService.hentPersonIdenter(personIdent.ident).identer()
        val fagsak = fagsakService.finnFagsak(personIdenter, StønadType.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")

        val nyeBarnSidenGjeldendeBehandling = finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsak.id)

        opprettOppfølgningsoppgaveForBarn(fagsak, nyeBarnSidenGjeldendeBehandling)

        return nyeBarnSidenGjeldendeBehandling.map { it.personIdent }
    }

    fun finnNyeEllerTidligereFødteBarn(personIdent: PersonIdent): NyeBarnDto {
        val personIdenter = personService.hentPersonIdenter(personIdent.ident).identer()
        val fagsak = fagsakService.finnFagsak(personIdenter, StønadType.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")
        val barnSidenGjeldendeBehandling = finnKobledeBarnSidenGjeldendeBehandling(fagsak.id)

        val nyeBarn = filtrerNyeBarn(barnSidenGjeldendeBehandling)

        opprettOppfølgningsoppgaveForBarn(fagsak, nyeBarn)

        val nyeBarnDto = nyeBarn.map { NyttBarn(it.personIdent, NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING) }
        val forTidligtFødteBarn = finnForTidligtFødteBarn(barnSidenGjeldendeBehandling)
        return NyeBarnDto(nyeBarnDto + forTidligtFødteBarn)
    }

    // TODO slett 4 måneder etter att siste migreringen er klar
    private fun opprettOppfølgningsoppgaveForBarn(fagsak: Fagsak, nyeBarn: List<BarnMinimumDto>) {
        if (fagsak.migrert) {
            try {
                taskRepository.save(OpprettOppgaveForMigrertFødtBarnTask.opprettOppgave(fagsak, nyeBarn))
            } catch (e: DbActionExecutionException) {
                if(e.cause is DuplicateKeyException) {
                    logger.warn("DuplicateKeyException ved opprettelse av task, den er sannsynligvis allerede opprettet")
                    return
                }
                throw e
            }
        }
    }

    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId: UUID): List<BarnMinimumDto> {
        val kobledeBarn = finnKobledeBarnSidenGjeldendeBehandling(fagsakId)
        return filtrerNyeBarn(kobledeBarn)
    }

    private fun finnKobledeBarnSidenGjeldendeBehandling(fagsakId: UUID): NyeBarnData {
        val behandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId)
                         ?: error("Kunne ikke finne behandling for fagsak - $fagsakId")
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return finnKobledeBarn(behandling.id, aktivIdent)
    }

    private fun finnKobledeBarn(forrigeBehandlingId: UUID, personIdent: String): NyeBarnData {
        val alleBarnPåBehandlingen = barnService.finnBarnPåBehandling(forrigeBehandlingId)
        val pdlBarnUnder18år = GrunnlagsdataMapper.mapBarn(personService.hentPersonMedBarn(personIdent).barn)
                .filter { it.fødsel.gjeldende().erUnder18År() }
        val kobledeBarn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(alleBarnPåBehandlingen, pdlBarnUnder18år)

        return NyeBarnData(pdlBarnUnder18år, kobledeBarn)
    }

    private fun finnForTidligtFødteBarn(kobledeBarn: NyeBarnData): List<NyttBarn> {
        return kobledeBarn.kobledeBarn
                .filter { it.behandlingBarn.personIdent == null }
                .filter { barnFødtFørTermin(it) }
                .map {
                    val barn = it.barn ?: error("Skal ha filtrert ut matchet barn uten barn")
                    NyttBarn(barn.personIdent, NyttBarnÅrsak.FØDT_FØR_TERMIN)
                }
    }

    private fun barnFødtFørTermin(barn: MatchetBehandlingBarn): Boolean {
        val pdlBarn = barn.barn
        val behandlingBarn = barn.behandlingBarn
        if (pdlBarn == null || behandlingBarn.fødselTermindato == null) {
            return false
        }
        val fødselsdato = pdlBarn.fødsel.gjeldende().fødselsdato ?: return false
        return YearMonth.from(fødselsdato) < YearMonth.from(behandlingBarn.fødselTermindato)
    }

    private data class NyeBarnData(val pdlBarnUnder18år: List<BarnMedIdent>,
                                   val kobledeBarn: List<MatchetBehandlingBarn>)

    private fun filtrerNyeBarn(data: NyeBarnData) =
            data.pdlBarnUnder18år
                    .filter { pdlBarn -> data.kobledeBarn.none { it.barn?.personIdent == pdlBarn.personIdent } }
                    .map { barnMinimumDto(it) }

    private fun barnMinimumDto(it: BarnMedIdent) =
            BarnMinimumDto(personIdent = it.personIdent,
                           navn = it.navn.visningsnavn(),
                           fødselsdato = it.fødsel.gjeldende().fødselsdato)


}