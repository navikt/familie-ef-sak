package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {
    /**
     * @return `null` dersom fullmakt er ukjent, f.eks. fordi saksbehandler mangler geografisk tilgang
     * i tilgangsmaskinen. Dette skal ikke tolkes som at personen ikke har noen fullmakter.
     */
    fun hentFullmakt(ident: String): List<Fullmakt>? {
        val fullmaktResponse = fullmaktClient.hentFullmakt(ident) ?: return null
        return fullmaktResponse.map {
            Fullmakt(
                it.gyldigFraOgMed,
                it.gyldigTilOgMed,
                it.fullmektig,
                MotpartsRolle.FULLMEKTIG,
                it.omraade.map { it.tema },
            )
        }
    }
}
