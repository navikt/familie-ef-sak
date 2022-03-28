package no.nav.familie.ef.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import no.nav.familie.kontrakter.felles.objectMapper
import java.time.YearMonth
import java.util.UUID

@Improvement("Bytt til Innvilget, Avslått og Henlagt")
enum class ResultatType {

    INNVILGE,
    AVSLÅ,
    HENLEGGE,
    OPPHØRT,
    SANKSJONERE,
}

enum class Sanksjonsårsak {
    NEKTET_DELTAGELSE_ARBEIDSMARKEDSTILTAK,
    NEKTET_TILBUDT_ARBEID,
    SAGT_OPP_STILLING,
    UNNLATT_GJENOPPTAGELSE_ARBEIDSFORHOLD,
    UNNLATT_MØTE_INNKALLING,
}

fun ResultatType.tilVedtaksresultat(): Vedtaksresultat = when (this) {
    ResultatType.INNVILGE -> Vedtaksresultat.INNVILGET
    ResultatType.HENLEGGE -> error("Vedtaksresultat kan ikke være henlegge")
    ResultatType.AVSLÅ -> Vedtaksresultat.AVSLÅTT
    ResultatType.OPPHØRT -> Vedtaksresultat.OPPHØRT
    ResultatType.SANKSJONERE -> Vedtaksresultat.INNVILGET
}

fun main() {

    jsontype(OvergangsstønadVedtakDto("Overgangsstønad"))
    jsontype(BarnetilsynVedtakDto("Barnetilsyn"))
}

fun jsontype(vedtak: NyVedtakDto) {
    val str = objectMapper.writeValueAsString(vedtak)
    println(str)
    val obj = objectMapper.readValue<NyVedtakDto>(str)
    println(obj::class.java)
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "_type")
sealed class NyVedtakDto() {

}

class OvergangsstønadVedtakDto(val _type: String) : NyVedtakDto()
class BarnetilsynVedtakDto(val _type: String) : NyVedtakDto()
class SkolepengerVedtakDto(val _type: String) : NyVedtakDto()

sealed class VedtakDto(val resultattypeIntern: ResultatType) {

    fun erInnvilgeMedOpphør(): Boolean {
        return this is Innvilget && this.perioder.any { it.periodeType == VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
    }
}

class Innvilget(val resultatType: ResultatType,
                val periodeBegrunnelse: String?,
                val inntektBegrunnelse: String?,
                val perioder: List<VedtaksperiodeDto> = emptyList(),
                val inntekter: List<Inntekt> = emptyList(),
                val samordningsfradragType: SamordningsfradragType? = null) : VedtakDto(ResultatType.INNVILGE)

class Avslå(val resultatType: ResultatType = ResultatType.AVSLÅ,
            val avslåÅrsak: AvslagÅrsak?,
            val avslåBegrunnelse: String?) : VedtakDto(ResultatType.AVSLÅ)

class Opphør(val resultatType: ResultatType = ResultatType.OPPHØRT,
             val opphørFom: YearMonth,
             val begrunnelse: String?) : VedtakDto(ResultatType.OPPHØRT)

class Sanksjonert(val resultatType: ResultatType = ResultatType.SANKSJONERE,
                  val sanksjonsårsak: Sanksjonsårsak,
                  val periode: VedtaksperiodeDto,
                  val internBegrunnelse: String) : VedtakDto(ResultatType.SANKSJONERE)

fun VedtakDto.tilVedtak(behandlingId: UUID): Vedtak = when (this) {
    is Avslå -> Vedtak(behandlingId = behandlingId,
                       avslåÅrsak = this.avslåÅrsak,
                       avslåBegrunnelse = this.avslåBegrunnelse,
                       resultatType = this.resultattypeIntern)
    is Innvilget -> Vedtak(behandlingId = behandlingId,
                           periodeBegrunnelse = this.periodeBegrunnelse,
                           inntektBegrunnelse = this.inntektBegrunnelse,
                           resultatType = this.resultatType,
                           perioder = PeriodeWrapper(perioder = this.perioder.tilDomene()),
                           inntekter = InntektWrapper(inntekter = this.inntekter.tilInntektsperioder()),
                           samordningsfradragType = this.samordningsfradragType)
    is Opphør -> Vedtak(behandlingId = behandlingId,
                        avslåBegrunnelse = begrunnelse,
                        resultatType = ResultatType.OPPHØRT,
                        opphørFom = opphørFom.atDay(1)
    )
    is Sanksjonert -> Vedtak(
            behandlingId = behandlingId,
            sanksjonsårsak = this.sanksjonsårsak,
            perioder = PeriodeWrapper(listOf(this.periode).tilDomene()),
            internBegrunnelse = this.internBegrunnelse,
            resultatType = ResultatType.SANKSJONERE,
    )
}

fun Vedtak.tilVedtakDto(): VedtakDto =
        when (this.resultatType) {
            ResultatType.INNVILGE -> Innvilget(
                    resultatType = this.resultatType,
                    periodeBegrunnelse = this.periodeBegrunnelse,
                    inntektBegrunnelse = this.inntektBegrunnelse,
                    perioder = (this.perioder ?: PeriodeWrapper(emptyList())).perioder.fraDomene(),
                    inntekter = (this.inntekter ?: InntektWrapper(emptyList())).inntekter.tilInntekt(),
                    samordningsfradragType = this.samordningsfradragType)
            ResultatType.AVSLÅ -> Avslå(
                    resultatType = this.resultatType,
                    avslåBegrunnelse = this.avslåBegrunnelse,
                    avslåÅrsak = this.avslåÅrsak,
            )
            ResultatType.OPPHØRT -> Opphør(
                    resultatType = this.resultatType,
                    begrunnelse = this.avslåBegrunnelse,
                    opphørFom = YearMonth.from(this.opphørFom)
            )
            ResultatType.SANKSJONERE -> Sanksjonert(
                    resultatType = this.resultatType,
                    sanksjonsårsak = this.sanksjonsårsak ?: error("Sanksjon mangler årsak."),
                    periode = (this.perioder?.perioder ?: emptyList()).fraDomeneForSanksjon(),
                    internBegrunnelse = this.internBegrunnelse ?: error("Sanksjon mangler intern begrunnelse."),
            )
            else -> throw Feil("Kan ikke sette vedtaksresultat som $this - ikke implementert")
        }


private class VedtakDtoDeserializer : StdDeserializer<VedtakDto>(VedtakDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): VedtakDto {
        val mapper = p.codec as ObjectMapper
        val node: JsonNode = mapper.readTree(p)
        return when (ResultatType.valueOf(node.get("resultatType").asText())) {
            ResultatType.INNVILGE -> mapper.treeToValue(node, Innvilget::class.java)
            ResultatType.AVSLÅ -> mapper.treeToValue(node, Avslå::class.java)
            ResultatType.OPPHØRT -> mapper.treeToValue(node, Opphør::class.java)
            ResultatType.SANKSJONERE -> mapper.treeToValue(node, Sanksjonert::class.java)
            else -> throw Feil("Kunde ikke deserialisera vedtakdto")
        }
    }
}

class VedtakDtoModule : com.fasterxml.jackson.databind.module.SimpleModule() {

    init {
        addDeserializer(VedtakDto::class.java, VedtakDtoDeserializer())
    }
}

