package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import org.springframework.stereotype.Service

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {
    /**
     * @return [FullmaktResultat] med `fullmakter = null` dersom fullmakt er ukjent, f.eks. fordi innlogget bruker
     * mangler tilgang i tilgangsmaskinen. Dette skal ikke tolkes som at personen ikke har noen fullmakter.
     * `ikkeTilgangÅrsak` beskriver hvorfor fullmakt ikke kunne hentes, og kan brukes til å gi en mer presis
     * feilmelding videre i kjeden.
     */
    fun hentFullmakt(ident: String): FullmaktResultat {
        val resultat = fullmaktClient.hentFullmakt(ident)
        val fullmakter =
            resultat.fullmakter?.map {
                Fullmakt(
                    it.gyldigFraOgMed,
                    it.gyldigTilOgMed,
                    it.fullmektig,
                    MotpartsRolle.FULLMEKTIG,
                    it.omraade.map { it.tema },
                )
            }
        return FullmaktResultat(fullmakter, resultat.ikkeTilgangÅrsak)
    }
}

data class FullmaktResultat(
    val fullmakter: List<Fullmakt>?,
    val ikkeTilgangÅrsak: String? = null,
)
