package no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl

import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VegadresseTest {

    @Test
    fun `returner UKJENT med distanse når avstand til annen adresse er mindre enn minimumsavstand for automatisk behandling`() {
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

        assertThat(avstandTilAnnenAdresse.langAvstandTilSøker).isEqualTo(LangAvstandTilSøker.UKJENT)
        assertThat(avstandTilAnnenAdresse.avstandIKm).isLessThan(1)
    }

    @Test
    fun `returner avstand til annen adresse er lenger enn minimumsavstand for automatisk behandling, sjekk Motzfeldts Gate og Sofiemyr`() {
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

        assertThat(avstandTilAnnenAdresse.avstandIKm).isGreaterThan(1)
        assertThat(avstandTilAnnenAdresse.langAvstandTilSøker).isEqualTo(LangAvstandTilSøker.JA)
    }

    @Test
    fun `returner avstand til annen adresse er lenger enn minimumsavstand for automatisk behandling som krysser to UTM-soner, sjekk Sofiemyr og Kirkenes`() {
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

        assertThat(avstandTilAnnenAdresse.avstandIKm).isGreaterThan(1)
        assertThat(avstandTilAnnenAdresse.langAvstandTilSøker).isEqualTo(LangAvstandTilSøker.JA_UPRESIS)
    }
}
