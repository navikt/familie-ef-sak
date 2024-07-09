package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentFullmakt(ident: String): List<Fullmakt> {
        val fullmaktResponse = fullmaktClient.hentFullmakt(ident)
        secureLogger.info("FullmaktResponse: $fullmaktResponse")
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
