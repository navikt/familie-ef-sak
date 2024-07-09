package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import org.springframework.stereotype.Service

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {
    fun hentFullmakt(ident: String): List<Fullmakt> {
        val fullmaktResponse = fullmaktClient.hentFullmakt(ident)

        return fullmaktResponse.map {
            Fullmakt(
                it.gyldigFraOgMed,
                it.gyldigTilOgMed,
                it.motpartsPersonident,
                MotpartsRolle.FULLMEKTIG,
                it.områder,
            )
        }
    }
}
