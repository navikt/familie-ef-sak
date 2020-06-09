package no.nav.familie.ef.sak.no.nav.familie.ef.sak.integration.dto.pdl

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class PdlDtoTest {

    @Test
    fun `pdlSøkerKortData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/søker_kort.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlSøkerKortData)!!

        Assertions.assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlSøkerData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/søker.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlSøkerData)!!

        Assertions.assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlBarnData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/barn.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlBarnData)!!

        Assertions.assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }

    @Test
    fun `pdlAnnenForelderData inneholder samme felter som blir spurt om i query`() {
        val spørringsfelter = PdlTestUtil.parseSpørring("/pdl/annenForelder.graphql")

        val dtoFelter = PdlTestUtil.finnFeltStruktur(PdlTestdata.pdlAnnenForelderData)!!

        Assertions.assertThat(dtoFelter).isEqualTo(spørringsfelter["data"])
    }
}
