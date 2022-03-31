package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonert
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.YearMonth

class VedtakDtoMapperTest {

    @Test
    fun `deserialiser og serialiser innvilget overgangsstønad vedtak dto`() {
        val vedtakJson = readFile("OvergangsstønadInnvilgetVedtakDto.json")

        val vedtak = innvilgelseOvergangsstønad()
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser avslå vedtak dto`() {
        val vedtakJson = readFile("AvslåVedtakDto.json")

        val vedtak = Avslå(AvslagÅrsak.BARN_OVER_ÅTTE_ÅR, "en god begrunnelse")
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser opphør vedtak dto`() {
        val vedtakJson = readFile("OpphørVedtakDto.json")

        val vedtak = Opphør(YearMonth.of(2022, 1), "en god begrunnelse")
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    @Test
    fun `deserialiser og serialiser sanksjon vedtak dto`() {
        val vedtakJson = readFile("SanksjonVedtakDto.json")

        val vedtak = Sanksjonert(Sanksjonsårsak.SAGT_OPP_STILLING, vedtaksperiode(), "begrunnelse")
        assertErLik(vedtak, vedtakJson)
        assertErLikUtenType(vedtak, vedtakJson)
    }

    private fun innvilgelseOvergangsstønad() =
            InnvilgelseOvergangsstønad("periodebegrunnelse",
                                       "inntektsbegrunnelse",
                                       listOf(vedtaksperiode()),
                                       listOf(Inntekt(årMånedFra = YearMonth.of(2021, 1),
                                                      forventetInntekt = BigDecimal(100_000),
                                                      samordningsfradrag = BigDecimal(500))),
                                       SamordningsfradragType.GJENLEVENDEPENSJON)

    private fun vedtaksperiode() =
            VedtaksperiodeDto(YearMonth.of(2021, 1),
                              YearMonth.of(2021, 12),
                              AktivitetType.BARN_UNDER_ETT_ÅR,
                              VedtaksperiodeType.HOVEDPERIODE)


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