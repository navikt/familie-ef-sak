package no.nav.familie.ef.sak.beregning.skolepenger

import no.nav.familie.ef.sak.felles.util.Skoleår
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype.HØGSKOLE_UNIVERSITET
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype.VIDEREGÅENDE
import java.time.Year

/**
 * Skoleår 2021 = 2021/2022
 *
 * https://lovdata.no/nav/rundskriv/v6-15-00#ref/lov/1997-02-28-19/%C2%A715-11
 */
object SkolepengerMaksbeløp {

    private val høgskoleUniversitet = mapOf<Year, Int>(
        Year.of(2022) to 69_500,
        Year.of(2021) to 68_136,
        Year.of(2020) to 66_604,
    )

    private val videregående = mapOf<Year, Int>(
        Year.of(2022) to 29_002,
        Year.of(2021) to 28_433,
        Year.of(2020) to 27_794,
    )

    fun maksbeløp(studietype: SkolepengerStudietype, skoleår: Skoleår): Int {
        val maksbeløp = when (studietype) {
            HØGSKOLE_UNIVERSITET -> høgskoleUniversitet[skoleår.år]
            VIDEREGÅENDE -> videregående[skoleår.år]
        }
        brukerfeilHvis(maksbeløp == null) {
            "Finner ikke maksbeløp for studietype=$studietype skoleår=$skoleår"
        }
        return maksbeløp
    }
}
