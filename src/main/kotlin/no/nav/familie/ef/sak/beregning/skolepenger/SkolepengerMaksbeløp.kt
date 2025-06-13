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
    private val høgskoleUniversitet =
        mapOf<Year, Int>(
            Year.of(2025) to 79_432,
            Year.of(2024) to 77_192,
            Year.of(2023) to 74_366,
            Year.of(2022) to 69_500,
            Year.of(2021) to 68_136,
            Year.of(2020) to 66_604,
            Year.of(2019) to 65_326,
        )

    private val videregående =
        mapOf<Year, Int>(
            Year.of(2025) to 33_145,
            Year.of(2024) to 32_211,
            Year.of(2023) to 31_033,
            Year.of(2022) to 29_002,
            Year.of(2021) to 28_433,
            Year.of(2020) to 27_794,
            Year.of(2019) to 27_276,
        )

    /**
     * I 2022, når det ikke finnes beløp for neste år så skal man kunne hente beløp for 2023
     */
    private fun hentBeløpForSkoleårEllerSkoleåretFør(
        map: Map<Year, Int>,
        skoleår: Skoleår,
    ): Int? = map[skoleår.år] ?: map[skoleår.år.minusYears(1)]

    fun maksbeløp(
        studietype: SkolepengerStudietype,
        skoleår: Skoleår,
    ): Int {
        val maksbeløp =
            when (studietype) {
                HØGSKOLE_UNIVERSITET -> hentBeløpForSkoleårEllerSkoleåretFør(høgskoleUniversitet, skoleår)
                VIDEREGÅENDE -> hentBeløpForSkoleårEllerSkoleåretFør(videregående, skoleår)
            }
        brukerfeilHvis(maksbeløp == null) {
            "Finner ikke maksbeløp for studietype=$studietype skoleår=$skoleår"
        }
        return maksbeløp
    }

    fun hentSisteÅrRegistrertMaksbeløpHøyskole(): Year = this.høgskoleUniversitet.keys.max()

    fun hentSisteÅrRegistrertMaksbeløpVideregående(): Year = this.videregående.keys.max()
}
