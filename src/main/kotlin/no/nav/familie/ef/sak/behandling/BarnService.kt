package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(private val behandlingService: BehandlingService,
                  private val fagsakService: FagsakService,
                  private val søknadService: SøknadService,
                  private val personService: PersonService,
                  private val grunnlagsdataService: GrunnlagsdataService) {

    fun finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(personIdent: PersonIdent): List<String> {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent.ident), Stønadstype.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)
                         ?: error("Kunne ikke finne behandling for fagsak")

        return finnNyeBarnSidenGjeldendeBehandling(behandling.id, personIdent.ident).map { it.personIdent }
    }

    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId: UUID): List<BarnMinimumDto> {
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                         ?: error("Kunne ikke finne behandling for fagsak - $fagsakId")
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return finnNyeBarnSidenGjeldendeBehandling(behandling.id, aktivIdent)

    }

    private fun finnNyeBarnSidenGjeldendeBehandling(behandlingId: UUID, personIdent: String): List<BarnMinimumDto> {
        val allePdlBarnIBehandlingen = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.barn
                .map { it.personIdent }

        val barnFraSøknaden = søknadService.hentOvergangsstønad(behandlingId)?.barn ?: emptySet()
        val barnFraPdlMenIkkeIBehandlingen = barnFraPdlMenIkkeIBehandlingen(personIdent, allePdlBarnIBehandlingen)
        val personIdenterForTerminbarn = personIdenterForTerminbarn(barnFraSøknaden, barnFraPdlMenIkkeIBehandlingen)

        return barnFraPdlMenIkkeIBehandlingen
                .filter { !personIdenterForTerminbarn.contains(it.personIdent) }
                .map {
                    BarnMinimumDto(personIdent = it.personIdent,
                                   navn = it.navn.visningsnavn(),
                                   fødselsdato = it.fødsel.gjeldende().fødselsdato)
                }
    }

    private fun personIdenterForTerminbarn(barnFraSøknaden: Set<SøknadBarn>,
                                           barnFraPdlMenIkkeIBehandlingen: List<BarnMedIdent>): List<String> {
        return barnFraSøknaden.filter { !it.erBarnetFødt }
                .mapNotNull { it.fødselTermindato }
                .mapNotNull { BarnMatcher.forsøkMatchPåFødselsdato(it, barnFraPdlMenIkkeIBehandlingen)?.personIdent }
    }

    private fun barnFraPdlMenIkkeIBehandlingen(personIdent: String, allePdlBarnIBehandlingen: List<String>): List<BarnMedIdent> {
        return personService.hentPersonMedBarn(personIdent).barn
                .filter { barnFraPdl ->
                    barnFraPdl.value.fødsel.gjeldende().erUnder18År() && allePdlBarnIBehandlingen.none { it == barnFraPdl.key }
                }
                .map { GrunnlagsdataMapper.mapBarn(it.value, it.key) }
    }


}