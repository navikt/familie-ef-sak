package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
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

        brukerfeilHvis(erNpid(personIdent)) {
            "Ident er en NPID har ingen data i PDL" // TODO: Annen feilmelding
        }
    }

    fun erNpid(personIdent: String): Boolean {
        if (personIdent.length != 11) return false

        val måned = personIdent.substring(2, 4).toInt()
        val syntetiskNpid = måned > 60 && måned <= 72
        val npid = måned > 20 && måned <= 32

        return (syntetiskNpid || npid)
    }
}
