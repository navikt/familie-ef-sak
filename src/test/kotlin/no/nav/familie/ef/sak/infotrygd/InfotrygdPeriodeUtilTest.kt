package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InfotrygdPeriodeUtilTest {

    /**
     * Output er lik input då det ikke er noen overlappende perioder
     */
    @Test
    internal fun `leser riktig antall input og output`() {
        val inputOutput = parseFil("infotrygd/enkel_case_3_perioder.csv")
        assertThat(inputOutput.input).hasSize(3)
        assertThat(inputOutput.output).hasSize(3)
    }

    @Test
    internal fun `enkel case 3 perioder`() {
        val inputOutput = parseFil("infotrygd/enkel_case_3_perioder.csv")
        assertThat(inputOutput.output).isEqualTo(InfotrygdPeriodeUtil.lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `startdato i perioden til første periode`() {
        val inputOutput = parseFil("infotrygd/erstatter_del_av_tidligere_periode.csv")
        assertThat(inputOutput.output).isEqualTo(InfotrygdPeriodeUtil.lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `startdato i perioden til første periode med opphør`() {
        val inputOutput = parseFil("infotrygd/erstatter_del_av_tidligere_periode_med_opphør.csv")
        assertThat(inputOutput.output).isEqualTo(InfotrygdPeriodeUtil.lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `enkel case med hopp mellom perioder`() {
        val inputOutput = parseFil("infotrygd/enkel_case_med_hopp.csv")
        assertThat(inputOutput.output).isEqualTo(InfotrygdPeriodeUtil.lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `samme start dato`() {
        val inputOutput = parseFil("infotrygd/samme_start_dato.csv")
        assertThat(inputOutput.output).isEqualTo(InfotrygdPeriodeUtil.lagPerioder(inputOutput.input))
    }

    private fun parseFil(fil: String) = InfotrygdPeriodeParser.parse(this::class.java.classLoader.getResource(fil)!!)

}
