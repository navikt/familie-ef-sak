package no.nav.familie.ef.sak.beregning

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.InntektWrapper
import no.nav.familie.ef.sak.repository.domain.PeriodeWrapper
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import org.springframework.http.HttpStatus
import java.time.YearMonth
import java.util.*

@Improvement("Bytt til Innvilget, Avslått og Henlagt")
enum class ResultatType {
    INNVILGE,
    AVSLÅ,
    HENLEGGE,
    OPPHØRT,
}

fun ResultatType.tilVedtaksresultat(): Vedtaksresultat = when(this) {
    ResultatType.INNVILGE -> Vedtaksresultat.INNVILGET // TODO: Når skal vi ha delvis innvilget og opphørt
    ResultatType.HENLEGGE -> error("Vedtaksresultat kan ikke være henlegge")
    ResultatType.AVSLÅ -> Vedtaksresultat.AVSLÅTT
    ResultatType.OPPHØRT -> Vedtaksresultat.OPPHØRT
}

sealed class VedtakDto
class Henlegge(val resultatType: ResultatType = ResultatType.HENLEGGE) : VedtakDto()
class Innvilget(val resultatType: ResultatType = ResultatType.INNVILGE,
                val periodeBegrunnelse: String?,
                val inntektBegrunnelse: String?,
                val perioder: List<VedtaksperiodeDto> = emptyList(),
                val inntekter: List<Inntekt> = emptyList()) : VedtakDto()

class Avslå(val resultatType: ResultatType = ResultatType.AVSLÅ,
            val avslåBegrunnelse: String?) : VedtakDto()
class Opphør(val resultatType: ResultatType = ResultatType.OPPHØRT,
             val opphørFom: YearMonth,
             val begrunnelse: String?) : VedtakDto()

fun VedtakDto.tilVedtak(behandlingId: UUID): Vedtak = when (this) {
    is Avslå -> Vedtak(behandlingId = behandlingId,
                       avslåBegrunnelse = this.avslåBegrunnelse,
                       resultatType = ResultatType.AVSLÅ)
    is Innvilget -> Vedtak(
            behandlingId = behandlingId,
            periodeBegrunnelse = this.periodeBegrunnelse,
            inntektBegrunnelse = this.inntektBegrunnelse,
            resultatType = ResultatType.INNVILGE,
            perioder = PeriodeWrapper(perioder = this.perioder.tilDomene()),
            inntekter = InntektWrapper(inntekter = this.inntekter.tilInntektsperioder()))
    is Opphør -> Vedtak(behandlingId = behandlingId,
                        avslåBegrunnelse = begrunnelse,
                        resultatType = ResultatType.OPPHØRT,
                        opphørFom = opphørFom.atDay(1)
    )
    is Henlegge -> throw Feil("Kan ikke sette vedtak $this då det har feil type", "Kan ikke sette vedtak $this då det har feil type", HttpStatus.BAD_REQUEST)
}

fun Vedtak.tilVedtakDto(): VedtakDto =
    when (this.resultatType) {
        ResultatType.INNVILGE -> Innvilget(
                resultatType = this.resultatType,
                periodeBegrunnelse = this.periodeBegrunnelse,
                inntektBegrunnelse = this.inntektBegrunnelse,
                perioder = (this.perioder ?: PeriodeWrapper(emptyList())).perioder.fraDomene(),
                inntekter = (this.inntekter ?: InntektWrapper(emptyList())).inntekter.tilInntekt())
        ResultatType.AVSLÅ -> Avslå(
                resultatType = this.resultatType,
                avslåBegrunnelse = this.avslåBegrunnelse
        )
        ResultatType.OPPHØRT -> Opphør(
                resultatType = this.resultatType,
                begrunnelse = this.avslåBegrunnelse,
                opphørFom = YearMonth.from(this.opphørFom)
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
            else -> throw Feil("Kunde ikke deserialisera vedtakdto")
        }
    }
}

class VedtakDtoModule : com.fasterxml.jackson.databind.module.SimpleModule() {

    init {
        addDeserializer(VedtakDto::class.java, VedtakDtoDeserializer())
    }
}

