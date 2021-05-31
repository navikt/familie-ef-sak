package no.nav.familie.ef.sak.api.beregning

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.repository.domain.InntektWrapper
import no.nav.familie.ef.sak.repository.domain.PeriodeWrapper
import no.nav.familie.ef.sak.repository.domain.Vedtak
import org.springframework.http.HttpStatus
import java.util.*

enum class ResultatType {
    INNVILGE,
    AVSLÅ,
    HENLEGGE
}

fun ResultatType.tilVedtak(): no.nav.familie.kontrakter.ef.felles.Vedtak = when(this) {
    ResultatType.INNVILGE -> Ve
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
        else -> throw Feil("Kan ikke sette vedtaksresultat som $this - ikke implementert")

}


private class VedtakDtoDeserializer : StdDeserializer<VedtakDto>(VedtakDto::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): VedtakDto {
        val mapper = p.codec as ObjectMapper
        val node: JsonNode = mapper.readTree(p)
        return when (ResultatType.valueOf(node.get("resultatType").asText())) {
            ResultatType.INNVILGE -> mapper.treeToValue(node, Innvilget::class.java)
            ResultatType.AVSLÅ -> mapper.treeToValue(node, Avslå::class.java)
            else -> throw Feil("Kunde ikke deserialisera vedtakdto")
        }
    }
}

class VedtakDtoModule : com.fasterxml.jackson.databind.module.SimpleModule() {

    init {
        addDeserializer(VedtakDto::class.java, VedtakDtoDeserializer())
    }
}

