package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeParser
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class InfotrygdPeriodeUtilTest {


    @Test
    internal fun `enkel case 3 perioder`() {
        val perioder = parseFil("infotrygd/enkel_case_3_perioder.csv")
        assertThat(perioder).hasSize(3)
    }

    private fun parseFil(fil: String) = InfotrygdPeriodeParser.parse(this::class.java.classLoader.getResource(fil))
}