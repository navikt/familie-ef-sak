package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class InfotrygdPeriodeUtilTest {


    @Test
    internal fun `enkel case 3 perioder`() {
        val perioder = parseFil("infotrygd/enkel_case_3_perioder.csv")
        assertThat(perioder).hasSize(3)
    }

    @Test
    internal fun `enkel case med hopp mellom perioder`() {
        val perioder = parseFil("infotrygd/enkel_case_med_hopp.csv")
        assertThat(perioder).hasSize(3)
        val førstePeriode = perioder.first()
        assertThat(førstePeriode.beløp).isEqualTo(18723)
    }

    @Test
    internal fun `samme start dato`() {
        val perioder = parseFil("infotrygd/samme_start_dato.csv")
        assertThat(perioder).hasSize(3)
        val førstePeriode = perioder.first()
        assertThat(førstePeriode.beløp).isEqualTo(18723)
    }

    private fun parseFil(fil: String) = InfotrygdPeriodeParser.parse(this::class.java.classLoader.getResource(fil)!!)

}
