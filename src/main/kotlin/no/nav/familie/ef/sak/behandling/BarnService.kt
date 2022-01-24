package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service

@Service
class BarnService(private val behandlingService: BehandlingService,
                  private val fagsakService: FagsakService,
//                  private val søknadService: SøknadService,
                  private val grunnlagsdataService: GrunnlagsdataService) {

    fun hentFnrForRegisterbarnISisteIverksatteBehandling(personIdent: String): List<String> {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent), Stønadstype.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)
                         ?: error("Kunne ikke finne behandling for fagsak")
        val grunnlagsdataMedMetadata = grunnlagsdataService.hentGrunnlagsdata(behandling.id)
        val allePdlBarnIBehandlingen = grunnlagsdataMedMetadata.grunnlagsdata.barn.map {
            it.personIdent
        }

        // TODO: Mirja/Hilde sjekker om vi ønsker å fiske frem potensielle terminfødte barn i tillegg,
        //  eller om det er ønskelig at det opprettes oppgave for disse

        return allePdlBarnIBehandlingen
    }

//    val søknad = søknadService.hentOvergangsstønad(behandling.id)
    // Søknad sine barn med fnr. De uten ligger kun i søknad

    //        val ufødteBarn = søknad?.barn?.filter { it.erBarnetFødt && it.fødselTermindato != null }
//
//        val alleBarnIPdl = emptyList<String>()//
//        val nyeBarnSomFinnesIBehandlingen = alleBarnIPdl.filter {
//            it === ufødteBarn.noe()
//        }

}