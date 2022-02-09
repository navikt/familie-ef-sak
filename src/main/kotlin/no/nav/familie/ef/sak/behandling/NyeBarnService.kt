package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.GrunnlagsdataMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NyeBarnService(private val behandlingService: BehandlingService,
                     private val fagsakService: FagsakService,
                     private val personService: PersonService,
                     private val barnService: BarnService,
                     private val pdlClient: PdlClient) {

    fun finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(personIdent: PersonIdent): List<String> {
        val personIdenter = pdlClient.hentPersonidenter(personIdent.ident).identer()
        val fagsak = fagsakService.finnFagsak(personIdenter, Stønadstype.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")

        return finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsak.id).map { it.personIdent }
    }

    fun finnNyeBarnSidenGjeldendeBehandlingForFagsak(fagsakId: UUID): List<BarnMinimumDto> {
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                         ?: error("Kunne ikke finne behandling for fagsak - $fagsakId")
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return finnNyeBarnSidenGjeldendeBehandling(behandling.id, aktivIdent)

    }

    private fun finnNyeBarnSidenGjeldendeBehandling(forrigeBehandlingId: UUID, personIdent: String): List<BarnMinimumDto> {
        val alleBarnPåBehandlingen = barnService.finnBarnPåBehandling(forrigeBehandlingId)
        val allePdlBarn = GrunnlagsdataMapper.mapBarn(personService.hentPersonMedBarn(personIdent).barn).filter { it.fødsel.gjeldende().erUnder18År() }
        val kobledeBarn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(alleBarnPåBehandlingen, allePdlBarn)

        return allePdlBarn
                .filter { pdlBarn -> kobledeBarn.none { it.barn?.personIdent == pdlBarn.personIdent } }
                .map {
                    BarnMinimumDto(personIdent = it.personIdent,
                                   navn = it.navn.visningsnavn(),
                                   fødselsdato = it.fødsel.gjeldende().fødselsdato)
                }
    }

}