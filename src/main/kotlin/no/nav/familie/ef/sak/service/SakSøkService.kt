package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.Kjønn
import no.nav.familie.ef.sak.api.dto.NavnDto
import no.nav.familie.ef.sak.api.dto.SakSøkDto
import no.nav.familie.ef.sak.api.dto.SakSøkListeDto
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøkerKort
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.validering.Sakstilgang
import no.nav.familie.kontrakter.felles.Ressurs
import org.springframework.stereotype.Component
import java.util.*


@Component
class SakSøkService(private val sakRepository: SakRepository,
                    private val pdlClient: PdlClient,
                    private val sakstilgang: Sakstilgang) {

    fun finnSaker(): Ressurs<SakSøkListeDto> {
        val saker = sakRepository.findAll().filter(sakstilgang::harTilgang)
        val personInfo = pdlClient.hentSøkerKortBolk(saker.map { it.søker.fødselsnummer })
        val søkListeDto = SakSøkListeDto(saker.map {
            val fnr = it.søker.fødselsnummer
            lagSakSøkDto(fnr, it.id, personInfo[fnr] ?: error("Finner ikke personinfo til søker $fnr"))
        })
        return Ressurs.success(søkListeDto)
    }

    fun finnSakForPerson(personIdent: String): Ressurs<SakSøkDto> {
        val saker = sakRepository.findBySøkerFødselsnummer(personIdent)
        return if (saker.isEmpty()) {
            Ressurs.failure(frontendFeilmelding = "Finner ikke noen sak på personen")
        } else {
            val søker = pdlClient.hentSøkerKortBolk(listOf(personIdent))[personIdent] ?: error("Finner ikke personinfo til søker")
            val sak = saker.first()
            val sakId = sak.id
            if (!sakstilgang.harTilgang(sak)) {
                Ressurs.failure(frontendFeilmelding = "Har ikke tilgang til sak")
            } else {
                Ressurs.success(lagSakSøkDto(personIdent, sakId, søker))
            }
        }
    }

    private fun lagSakSøkDto(personIdent: String,
                             sakId: UUID,
                             søker: PdlSøkerKort): SakSøkDto =
            SakSøkDto(
                    sakId = sakId,
                    personIdent = personIdent,
                    kjønn = søker.kjønn.single().kjønn.let { Kjønn.valueOf(it.name) },
                    navn = NavnDto.fraNavn(søker.navn.gjeldende())
            )

}
