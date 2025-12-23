package no.nav.familie.ef.sak.vedtak

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.avslagDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseBarnetilsynDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseOvergangsstønadDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseSkolepengerDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.opphørDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.opphørSkolepengerDto
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
    }

    @Test
    fun `deserialiser og serialiser innvilget barnetilsyn vedtak dto`() {
        val vedtakJson = readFile("BarnetilsynInnvilgetVedtakDto.json")

        val vedtak = innvilgelseBarnetilsynDto(UUID.fromString("4ab497b2-a19c-4415-bf00-556ff8e9ce86"))
        assertErLik(vedtak, vedtakJson)
    }

    /*
    TODO: Er det riktig å håndtere BarnetilsynInnvilgetUtenUtbetaling som egen type når det ikke har egen klasse?
    @Test
    fun `deserialiser og serialiser innvilget barnetilsyn uten utbetaling vedtak dto`() {
        val vedtakJson = readFile("BarnetilsynInnvilgetUtenUtbetalingVedtakDto.json")

        val vedtak =
            innvilgelseBarnetilsynDto(UUID.fromString("4ab497b2-a19c-4415-bf00-556ff8e9ce86"), kontantstøtteBegrunnelse = "test")
                .copy(
                    resultatType = ResultatType.INNVILGE_UTEN_UTBETALING,
                    _type = "InnvilgelseBarnetilsynUtenUtbetaling",
                )
        assertErLik(vedtak, vedtakJson)
    }
     */

    @Test
    fun `deserialiser og serialiser innvilget skolepenger vedtak dto`() {
        val vedtakJson = readFile("SkolepengerInnvilgetVedtakDto.json")

        val vedtak = innvilgelseSkolepengerDto()
        assertErLik(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser opphørt skolepenger vedtak dto`() {
        val vedtakJson = readFile("SkolepengerOpphørDto.json")

        val vedtak = opphørSkolepengerDto()
        assertErLik(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser avslå vedtak dto`() {
        val vedtakJson = readFile("AvslåVedtakDto.json")

        val vedtak = avslagDto()
        assertErLik(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser opphør vedtak dto`() {
        val vedtakJson = readFile("OpphørVedtakDto.json")

        val vedtak = opphørDto()
        assertErLik(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser sanksjon vedtak dto`() {
        val vedtakJson = readFile("SanksjonVedtakDto.json")

        val vedtak = sanksjonertDto()
        assertErLik(vedtak, vedtakJson)
    }

    private fun assertErLik(
        vedtakDto: VedtakDto,
        vedtakJson: String,
    ) {
        val serialisertVedtak = objectMapper.writeValueAsString(vedtakDto)
        assertThat(objectMapper.readValue<VedtakDto>(serialisertVedtak)).isEqualTo(objectMapper.readValue<VedtakDto>(vedtakJson))
        assertThat(objectMapper.readValue<VedtakDto>(vedtakJson)).isEqualTo(vedtakDto)
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/vedtak/$filnavn")!!.readText()
}
