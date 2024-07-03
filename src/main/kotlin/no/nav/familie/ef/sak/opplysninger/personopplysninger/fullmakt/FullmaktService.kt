package no.nav.familie.ef.sak.opplysninger.personopplysninger.fullmakt

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fullmakt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.MotpartsRolle
import org.springframework.stereotype.Service

@Service
class FullmaktService(
    val fullmaktClient: FullmaktClient,
) {
    fun hentFullmakt(ident: String): List<Fullmakt> {
        val fullmakt = fullmaktClient.hentFullmakt(ident)

        return listOf(
            Fullmakt(
                fullmakt.gyldigFraOgMed,
                fullmakt.gyldigTilOgMed,
                fullmakt.motpartsPersonident,
                MotpartsRolle.FULLMEKTIG,
                fullmakt.områder,
            ),
        )
    }
}
