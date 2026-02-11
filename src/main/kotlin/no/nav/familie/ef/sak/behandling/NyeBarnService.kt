package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.dto.BehandlingBarnDto
import no.nav.familie.ef.sak.behandling.migrering.OpprettOppgaveForMigrertFødtBarnTask
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.ef.personhendelse.NyeBarnDto
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarnÅrsak
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.postgresql.util.PSQLException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.relational.core.conversion.DbActionExecutionException
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class NyeBarnService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    private val barnService: BarnService,
    private val taskService: TaskService,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun finnNyeEllerUtenforTerminFødteBarn(personIdent: PersonIdent): NyeBarnDto {
        val personIdenter = personService.hentPersonIdenter(personIdent.ident).identer()
        val nyttBarnList = mutableListOf<NyttBarn>()
        val fagsaker = fagsakService.finnFagsaker(personIdenter)
        if (fagsaker.isEmpty()) error("Kunne ikke finne fagsak for personident")

        for (fagsak in fagsaker) {
            val barnSidenGjeldendeBehandling = finnKobledeBarnSidenGjeldendeBehandling(fagsak.id, false)
            val nyeBarn = filtrerNyeBarn(barnSidenGjeldendeBehandling)
            opprettOppfølgningsoppgaveForBarn(fagsak, nyeBarn)

            nyttBarnList.addAll(
                nyeBarn.map {
                    NyttBarn(it.personIdent, fagsak.stønadstype, NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING)
                },
            )
            nyttBarnList.addAll(finnForTidligtFødteBarn(barnSidenGjeldendeBehandling, fagsak.stønadstype))
            nyttBarnList.addAll(finnForSentFødteBarn(barnSidenGjeldendeBehandling, fagsak.stønadstype))
        }
        return NyeBarnDto(nyttBarnList)
    }

    // TODO slett 4 måneder etter att siste migreringen er klar
    private fun opprettOppfølgningsoppgaveForBarn(
        fagsak: Fagsak,
        nyeBarn: List<BarnMinimumDto>,
    ) {
        if (fagsak.migrert) {
            try {
                taskService.save(OpprettOppgaveForMigrertFødtBarnTask.opprettOppgave(fagsak, nyeBarn))
            } catch (e: DuplicateKeyException) {
                logger.warn("DuplicateKeyException ved opprettelse av task, den er sannsynligvis allerede opprettet")
                return
            }
        }
    }

    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId: UUID): BehandlingBarnDto {
        val kobledeBarn = finnKobledeBarnSidenGjeldendeBehandling(fagsakId)
        val nyeBarn = filtrerNyeBarn(kobledeBarn)
        return BehandlingBarnDto(nyeBarn, kobledeBarn.kobledeBarn.isNotEmpty())
    }

    private fun finnKobledeBarnSidenGjeldendeBehandling(
        fagsakId: UUID,
        forventerAtBehandlingFinnes: Boolean = true,
    ): NyeBarnData {
        val behandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId)
        if (behandling == null) {
            feilHvis(forventerAtBehandlingFinnes) {
                "Fant ikke iverksatt eller avslått behandling for fagsak=$fagsakId"
            }
            return NyeBarnData(emptyList(), emptyList())
        }
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return finnKobledeBarn(behandling.id, aktivIdent)
    }

    private fun finnKobledeBarn(
        behandlingId: UUID,
        personIdent: String,
    ): NyeBarnData {
        val alleBarnPåBehandlingen = barnService.finnBarnPåBehandling(behandlingId)
        val pdlBarn = GrunnlagsdataMapper.mapBarn(personService.hentPersonMedBarn(personIdent).barn)
        val kobledeBarn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(alleBarnPåBehandlingen, pdlBarn)

        return NyeBarnData(pdlBarn, kobledeBarn)
    }

    private fun finnForTidligtFødteBarn(
        kobledeBarn: NyeBarnData,
        stønadstype: StønadType,
    ): List<NyttBarn> =
        kobledeBarn.kobledeBarn
            .filter { it.behandlingBarn.personIdent == null }
            .filter { barnFødtFørTermin(it) }
            .map {
                val barn = it.barn ?: error("Skal ha filtrert ut matchet barn uten barn")
                NyttBarn(barn.personIdent, stønadstype, NyttBarnÅrsak.FØDT_FØR_TERMIN)
            }

    private fun finnForSentFødteBarn(
        kobledeBarn: NyeBarnData,
        stønadstype: StønadType,
    ): List<NyttBarn> {
        val forSentFødteBarn =
            kobledeBarn.kobledeBarn
                .filter { it.behandlingBarn.personIdent == null }
                .filter { barnFødtEtterTermin(it) }
                .map {
                    val barn = it.barn ?: error("Skal ha filtrert ut matchet barn uten barn")
                    NyttBarn(barn.personIdent, stønadstype, NyttBarnÅrsak.FØDT_ETTER_TERMIN)
                }
        return forSentFødteBarn
    }

    private fun barnFødtFørTermin(barn: MatchetBehandlingBarn): Boolean {
        val pdlBarn = barn.barn
        val behandlingBarn = barn.behandlingBarn
        if (pdlBarn == null || behandlingBarn.fødselTermindato == null) {
            return false
        }
        val fødselsdato = pdlBarn.fødsel.first().fødselsdato ?: return false
        return YearMonth.from(fødselsdato) < YearMonth.from(behandlingBarn.fødselTermindato)
    }

    private fun barnFødtEtterTermin(barn: MatchetBehandlingBarn): Boolean {
        val pdlBarn = barn.barn
        val behandlingBarn = barn.behandlingBarn
        if (pdlBarn == null || behandlingBarn.fødselTermindato == null) {
            return false
        }
        val fødselsdato = pdlBarn.fødsel.first().fødselsdato ?: return false
        return YearMonth.from(fødselsdato).month > YearMonth.from(behandlingBarn.fødselTermindato).month
    }

    private data class NyeBarnData(
        val pdlBarn: List<BarnMedIdent>,
        val kobledeBarn: List<MatchetBehandlingBarn>,
    )

    private fun filtrerNyeBarn(data: NyeBarnData) =
        data.pdlBarn
            .filter { pdlBarn -> data.kobledeBarn.none { it.barn?.personIdent == pdlBarn.personIdent } }
            .map { barnMinimumDto(it) }

    private fun barnMinimumDto(it: BarnMedIdent) =
        BarnMinimumDto(
            personIdent = it.personIdent,
            navn = it.navn.visningsnavn(),
            fødselsdato = it.fødsel.first().fødselsdato,
        )
}
