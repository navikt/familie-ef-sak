package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Service

@Service
class BarnService(private val behandlingService: BehandlingService,
                  private val fagsakService: FagsakService,
                  val personopplysningerService: PersonopplysningerService) {

    fun hentFnrForAlleBarn(personIdent: String): List<String> {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent), Stønadstype.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")
        val behandling =
                behandlingService.finnSisteIverksatteBehandling(fagsak.id) ?: error("Kunne ikke finne behandling for fagsak")
        val personopplysningerService = personopplysningerService.hentPersonopplysninger(behandling.id)
        return personopplysningerService.barn.map { it.personIdent }
    }
}