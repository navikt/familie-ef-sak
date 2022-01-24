package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.kontrakter.felles.PersonIdent
import org.springframework.stereotype.Service

@Service
class BarnService(private val behandlingService: BehandlingService,
                  private val fagsakService: FagsakService,
                  private val personService: PersonService,
                  private val grunnlagsdataService: GrunnlagsdataService) {

    fun finnNyeBarnSidenGjeldendeBehandling(personIdent: PersonIdent): List<String> {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent.ident), Stønadstype.OVERGANGSSTØNAD)
                     ?: error("Kunne ikke finne fagsak for personident")
        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsak.id)
                         ?: error("Kunne ikke finne behandling for fagsak")

        val allePdlBarnIBehandlingen = grunnlagsdataService.hentGrunnlagsdata(behandling.id).grunnlagsdata.barn.map {
            it.personIdent
        }
        val alleBarnFraPdl = personService.hentPersonMedBarn(personIdent.ident)

        val barnFraPdlMenIkkeIBehandlingen = alleBarnFraPdl.barn.filter { barnFraPdl ->
            barnFraPdl.value.fødsel.gjeldende().erUnder18År() && allePdlBarnIBehandlingen.none { it == barnFraPdl.key }
        }

        // TODO: Mirja/Hilde sjekker om vi ønsker å fiske frem potensielle terminfødte barn i tillegg,
        //  eller om det er ønskelig at det opprettes oppgave for disse

        return barnFraPdlMenIkkeIBehandlingen.keys.toList()
    }

}