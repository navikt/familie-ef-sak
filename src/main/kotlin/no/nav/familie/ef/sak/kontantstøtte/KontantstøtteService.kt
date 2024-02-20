package no.nav.familie.ef.sak.kontantstøtte

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import org.springframework.stereotype.Service

@Service
class KontantstøtteService(
    val kontantstøtteClient: KontantstøtteClient,
    val personService: PersonService,
) {
    fun finnesKontantstøtteUtbetalingerPåBruker(personIdent: String): HentUtbetalingsinfoKontantstøtteDto {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer().toList()
        return kontantstøtteClient.hentUtbetalingsinfo(personIdenter).tilDto()
    }
}

fun HentUtbetalingsinfoKontantstøtte.tilDto(): HentUtbetalingsinfoKontantstøtteDto {
    return HentUtbetalingsinfoKontantstøtteDto(
        this.ksSakPerioder.isNotEmpty() || this.infotrygdPerioder.isNotEmpty(),
    )
}

data class HentUtbetalingsinfoKontantstøtteDto(
    val finnesUtbetaling: Boolean,
)
