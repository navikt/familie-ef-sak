package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.api.dto.NavnDto
import no.nav.familie.ef.sak.api.dto.SakSøkDto
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøkerKort
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Component
import java.util.*


@Component
class SakSøkService(
        private val sakRepository: SakRepository,
        private val pdlClient: PdlClient
) {

    fun finnSakForPerson(personIdent: String): Ressurs<SakSøkDto> {
        val saker = sakRepository.findBySøkerFødselsnummer(personIdent)
        return if (saker.isEmpty()) {
            Ressurs.failure(frontendFeilmelding = "Finner ikke noen sak på personen")
        } else {
            val søker = pdlClient.hentSøkerKort(personIdent)
            val sakId = saker.first().id
            lagSakSøkDto(personIdent, sakId, søker)
        }
    }

    private fun lagSakSøkDto(
            personIdent: String,
            sakId: UUID,
            søker: PdlSøkerKort
            ): Ressurs<SakSøkDto> {
        return Ressurs.success(SakSøkDto(
                sakId = sakId,
                personIdent = personIdent,
                kjønn = søker.kjønn.first().kjønn.let { Kjønn.valueOf(it.name) },
                navn = søker.navn.first().let {
                    NavnDto(it.fornavn,
                            it.mellomnavn,
                            it.etternavn)
                }
        ))
    }

}
