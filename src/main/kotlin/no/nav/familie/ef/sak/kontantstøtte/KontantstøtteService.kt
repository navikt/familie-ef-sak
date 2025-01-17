package no.nav.familie.ef.sak.kontantstøtte

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import org.springframework.stereotype.Service
import java.time.YearMonth

@Service
class KontantstøtteService(
    val kontantstøtteClient: KontantstøtteClient,
    val personService: PersonService,
) {
    fun hentUtbetalingsinfoKontantstøtte(personIdent: String): HentUtbetalingsinfoKontantstøtteDto {
        val personIdenter = personService.hentPersonIdenter(personIdent).identer().toList()
        return kontantstøtteClient.hentUtbetalingsinfo(personIdenter).tilDto()
    }
}

fun HentUtbetalingsinfoKontantstøtte.tilDto(): HentUtbetalingsinfoKontantstøtteDto =
    HentUtbetalingsinfoKontantstøtteDto(
        this.ksSakPerioder.isNotEmpty() || this.infotrygdPerioder.isNotEmpty(),
        this.ksSakPerioder.map { KsakPeriodeDto(it.fomMåned, it.tomMåned, KontantstøtteDatakilde.KONTANTSTØTTE) } +
            this.infotrygdPerioder.map { KsakPeriodeDto(it.fomMåned, it.tomMåned, KontantstøtteDatakilde.INFOTRYGD) },
    )

data class HentUtbetalingsinfoKontantstøtteDto(
    val finnesUtbetaling: Boolean,
    val perioder: List<KsakPeriodeDto>,
)

data class KsakPeriodeDto(
    val årMånedFra: YearMonth,
    val årMånedTil: YearMonth? = null,
    val kilde: KontantstøtteDatakilde,
)

enum class KontantstøtteDatakilde {
    KONTANTSTØTTE,
    INFOTRYGD,
}
