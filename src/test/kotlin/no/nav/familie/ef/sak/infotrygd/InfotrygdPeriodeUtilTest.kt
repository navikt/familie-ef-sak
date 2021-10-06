package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeParser
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
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
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `startdato i perioden til første periode`() {
        val inputOutput = parseFil("infotrygd/erstatter_del_av_tidligere_periode.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `startdato i perioden til første periode med opphør`() {
        val inputOutput = parseFil("infotrygd/erstatter_del_av_tidligere_periode_med_opphør.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `enkel case med hopp mellom perioder`() {
        val inputOutput = parseFil("infotrygd/enkel_case_med_hopp.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `samme start dato`() {
        val inputOutput = parseFil("infotrygd/samme_start_dato.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `enkel_forkortning_periode_158`() {
        val inputOutput = parseFil("infotrygd/enkel_forkortning_periode_158.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `perioder med flere opphør, returnerer kun en periode av vedtak 2 `() {
        val inputOutput = parseFil("infotrygd/perioder_med_flere_opphør_2172101.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `periode avkorter en tidligere lenger periode, vedtak 6 avkorter vedtak 5`() {
        val inputOutput = parseFil("infotrygd/avkorter_periode_4571388.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `4 perioder omsluttes av annen periode og returneres ikke, vedtak 11 gjør vedtak 7 til 10 irrelevant`() {
        val inputOutput = parseFil("infotrygd/4_perioder_omsluttes_av_annen_periode_4120848.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `flere_duplikater_4131963`() {
        val inputOutput = parseFil("infotrygd/flere_duplikater_4131963.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    @Test
    internal fun `ny_stønad_med_periode_før_alle_andre_4921363`() {
        val inputOutput = parseFil("infotrygd/ny_stønad_med_periode_før_alle_andre_4921363.csv")
        assertThat(inputOutput.output).isEqualTo(lagPerioder(inputOutput.input))
    }

    fun lagPerioder(perioder: List<InfotrygdPeriode>): List<InternPeriode> {
        val perioder = InfotrygdPeriodeUtil.filtrerOgSorterPerioderFraInfotrygd(perioder)
            .map { it.tilInternPeriode() }

        return InternPeriodeUtil.slåSammenPerioder(perioder)
    }

    private fun parseFil(fil: String) = InfotrygdPeriodeParser.parse(this::class.java.classLoader.getResource(fil)!!)

}
