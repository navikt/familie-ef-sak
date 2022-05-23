package no.nav.familie.ef.sak.vedtak

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.avslagDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseBarnetilsynDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseOvergangsstønadDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseSkolepengerDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.opphørDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.sanksjonertDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class VedtakDtoMapperTest {

    @Test
    fun `deserialiser og serialiser innvilget overgangsstønad vedtak dto`() {
        val vedtakJson = readFile("OvergangsstønadInnvilgetVedtakDto.json")

        val vedtak = innvilgelseOvergangsstønadDto()
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser innvilget barnetilsyn vedtak dto`() {
        val vedtakJson = readFile("BarnetilsynInnvilgetVedtakDto.json")

        val vedtak = innvilgelseBarnetilsynDto(UUID.fromString("4ab497b2-a19c-4415-bf00-556ff8e9ce86"))
        assertErLik(vedtak, vedtakJson)
        //assertErLikUtenType(vedtak, vedtakJson) Må få type fra frontend når barnetilsyn blir tatt i bruk
    }

    @Test
    fun `deserialiser og serialiser innvilget barnetilsyn uten utbetaling vedtak dto`() {
        val vedtakJson = readFile("BarnetilsynInnvilgetUtenUtbetalingVedtakDto.json")

        val vedtak = innvilgelseBarnetilsynDto(UUID.fromString("4ab497b2-a19c-4415-bf00-556ff8e9ce86"))
                .copy(resultatType = ResultatType.INNVILGE_UTEN_UTBETALING,
                      _type = "InnvilgelseBarnetilsynUtenUtbetaling")
        assertErLik(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser innvilget skolepenger vedtak dto`() {
        val vedtakJson = readFile("SkolepengerInnvilgetVedtakDto.json")

        val vedtak = innvilgelseSkolepengerDto()
        assertErLik(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser avslå vedtak dto`() {
        val vedtakJson = readFile("AvslåVedtakDto.json")

        val vedtak = avslagDto()
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser opphør vedtak dto`() {
        val vedtakJson = readFile("OpphørVedtakDto.json")

        val vedtak = opphørDto()
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser sanksjon vedtak dto`() {
        val vedtakJson = readFile("SanksjonVedtakDto.json")

        val vedtak = sanksjonertDto()
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    private fun assertErLik(vedtakDto: VedtakDto, vedtakJson: String) {
        val serialisertVedtak = objectMapper.writeValueAsString(vedtakDto)
        assertThat(objectMapper.readTree(serialisertVedtak)).isEqualTo(objectMapper.readTree(vedtakJson))
        assertThat(objectMapper.readValue<VedtakDto>(vedtakJson)).isEqualTo(vedtakDto)
    }

    private fun assertErLikUtenType(vedtakDto: VedtakDto, vedtakJson: String) {
        val tree = objectMapper.readTree(vedtakJson)
        (tree as ObjectNode).remove("_type")
        val vedtakJsonUtenType = objectMapper.writeValueAsString(tree)
        assertThat(objectMapper.readValue<VedtakDto>(vedtakJsonUtenType)).isEqualTo(vedtakDto)
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/vedtak/$filnavn")!!.readText()
    }
}