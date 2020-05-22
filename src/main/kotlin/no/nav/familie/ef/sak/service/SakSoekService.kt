package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.api.dto.NavnDto
import no.nav.familie.ef.sak.api.dto.SakSøkDto
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Component


@Component
class SakSoekService(
        private val sakRepository: SakRepository,
        private val pdlClient: PdlClient
) {

    fun finnSakForPerson(personIdent: String): Ressurs<SakSøkDto> {
        val saker = sakRepository.findBySøkerFødselsnummer(personIdent)
        return if (saker.isEmpty()) {
            Ressurs.failure(frontendFeilmelding = "Finner ikke noen sak på personen")
        } else {
            hentSøkerInformasjon(personIdent, saker)
        }
    }

    private fun hentSøkerInformasjon(personIdent: String, saker: List<Sak>): Ressurs<SakSøkDto> {
        val søker = pdlClient.hentSøkerKort(personIdent)

        return Ressurs.success(SakSøkDto(
                sakId = saker[0].id,
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
