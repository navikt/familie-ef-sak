package no.nav.familie.ef.sak.kontantstøtte

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import org.springframework.stereotype.Service

@Service
class KontantstøtteService(
    val kontantstøtteClient: KontantstøtteClient,
    val personService: PersonService,
) {

    fun finnesKontantstøtteUtbetalingerPåBrukersBarn(personIdent: String): HentUtbetalingsinfoKontantstøtteDto {
        val hentPersonMedBarn = personService.hentPersonMedBarn(personIdent)
        val barnIdenter = hentPersonMedBarn.barn.keys.toList()
        return kontantstøtteClient.hentUtbetalingsinfo(barnIdenter).tilDto()
    }
}

fun List<HentUtbetalingsinfoKontantstøtte>.tilDto(): HentUtbetalingsinfoKontantstøtteDto {
    return HentUtbetalingsinfoKontantstøtteDto(
        this.any { it.ksSakPerioder.isNotEmpty() || it.infotrygdPerioder.isNotEmpty() },
    )
}
data class HentUtbetalingsinfoKontantstøtteDto(
    val finnesUtbetaling: Boolean,
)
