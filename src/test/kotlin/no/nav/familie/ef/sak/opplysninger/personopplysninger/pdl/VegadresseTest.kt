package no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VegadresseTest {

    @Test
    fun `returner false når avstand til annen adresse er mindre enn minimumsavstand for automatisk behandling`() {
        val avstandTilAnnenAdresse =
            PdlTestdata.vegadresse.fjerneBoforhold(
                PdlTestdata.vegadresse.copy(
                    koordinater = Koordinater(
                        4f,
                        5f,
                        null,
                        null
                    )
                )
            )

        assertThat(avstandTilAnnenAdresse).isFalse
    }

    @Test
    fun `returner true når avstand til annen adresse er lenger enn minimumsavstand for automatisk behandling, sjekk Motzfeldts Gate og Sofiemyr`() {
        val motzfeldtsgate = PdlTestdata.vegadresse.copy(
            koordinater = Koordinater(
                598845f,
                6643333f,
                null,
                null
            )
        )
        val sofiemyr = PdlTestdata.vegadresse.copy(
            koordinater = Koordinater(
                601372f,
                6629367f,
                null,
                null
            )
        )
        val avstandTilAnnenAdresse =
            motzfeldtsgate.fjerneBoforhold(sofiemyr)

        assertThat(avstandTilAnnenAdresse).isTrue
    }

    @Test
    fun `returner true når avstand til annen adresse er lenger enn minimumsavstand for automatisk behandling, sjekk Sofiemyr og Kirkenes`() {
        val kirkenes = PdlTestdata.vegadresse.copy(
            koordinater = Koordinater(
                615386.4f,
                7734094.9f,
                null,
                null
            )
        )

        val sofiemyr = PdlTestdata.vegadresse.copy(
            koordinater = Koordinater(
                601372f,
                6629367f,
                null,
                null
            )
        )
        val avstandTilAnnenAdresse =
            kirkenes.fjerneBoforhold(sofiemyr)

        assertThat(avstandTilAnnenAdresse).isTrue()
    }
}
