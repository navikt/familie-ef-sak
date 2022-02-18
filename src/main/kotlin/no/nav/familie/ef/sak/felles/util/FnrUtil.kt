package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import org.springframework.http.HttpStatus

object FnrUtil {

    val FNR_REGEX = """[0-9]{11}""".toRegex()

    fun validerOptionalIdent(personIdent: String?) {
        if (!personIdent.isNullOrBlank()) {
            validerIdent(personIdent)
        }
    }

    fun validerIdent(personIdent: String) {
        if (personIdent.length != 11) {
            throw ApiFeil("Ugyldig personident. Det må være 11 sifre", HttpStatus.BAD_REQUEST)
        }
        if (!FNR_REGEX.matches(personIdent)) {
            throw ApiFeil("Ugyldig personident. Det kan kun inneholde tall", HttpStatus.BAD_REQUEST)
        }
    }
}