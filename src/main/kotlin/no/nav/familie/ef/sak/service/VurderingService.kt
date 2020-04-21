package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.vurdering.MedlemskapRegelsett
import no.nav.familie.ef.sak.vurdering.Medlemskapshistorikk
import org.springframework.data.repository.findByIdOrNull
import java.util.*

class VurderingService(val sakRepository: SakRepository,
                       val personService: PersonService,
                       val familieIntegrasjonerClient: FamilieIntegrasjonerClient) {

    fun vurderMedlemskap(id: UUID) {
        val sak = sakRepository.findByIdOrNull(id) ?: error("Ugyldig Primærnøkkel : $id")

        val pdlSøker = personService.hentPdlPerson(sak.søker.fødselsnummer)

        val medlemskapsinfoSøker = familieIntegrasjonerClient.hentMedlemskapsinfo(sak.søker.fødselsnummer)
        val medlemskapshistorikk = Medlemskapshistorikk(pdlSøker.data?.person!!, medlemskapsinfoSøker)

        sak.barn.map {

            val medlemskapshistorikkAnnenForelder = it.annenForelder?.let { annenForelder ->
                val pdlAnnenForelder =
                        personService.hentPdlAnnenForelder(annenForelder.fødselsnummer!!)
                val medlemskapsinfoAnnenForelder =
                        familieIntegrasjonerClient.hentMedlemskapsinfo(annenForelder.fødselsnummer)
                Medlemskapshistorikk(pdlAnnenForelder.data?.person!!, medlemskapsinfoAnnenForelder)
            }

            val medlemskapRegelsett = MedlemskapRegelsett(medlemskapshistorikk, medlemskapshistorikkAnnenForelder)
            medlemskapRegelsett.totalvurderingMedlemskap.evaluer(sak)

        }


    }


}
