package no.nav.familie.ef.sak.vedtak.dto

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import no.nav.familie.ef.sak.beregning.Inntekt
import no.nav.familie.ef.sak.beregning.tilInntekt
import no.nav.familie.ef.sak.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeMedSanksjonsårsak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.YearMonth
import java.util.UUID

@Improvement("Bytt til Innvilget, Avslått og Henlagt")
enum class ResultatType {
    INNVILGE,
    INNVILGE_UTEN_UTBETALING,
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

fun ResultatType.tilVedtaksresultat(): Vedtaksresultat =
    when (this) {
        ResultatType.INNVILGE -> Vedtaksresultat.INNVILGET

        ResultatType.INNVILGE_UTEN_UTBETALING -> Vedtaksresultat.INNVILGET

        // TODO: Må kanskje være litt smart her???
        ResultatType.HENLEGGE -> error("Vedtaksresultat kan ikke være henlegge")

        ResultatType.AVSLÅ -> Vedtaksresultat.AVSLÅTT

        ResultatType.OPPHØRT -> Vedtaksresultat.OPPHØRT

        ResultatType.SANKSJONERE -> Vedtaksresultat.INNVILGET
    }

/*@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
              include = JsonTypeInfo.As.PROPERTY,
              property = "_type")*/
sealed class VedtakDto(
    open val resultatType: ResultatType,
    open val _type: String,
)

// Rename til dto? InnvilgelseOvergangsstønadDto, AvslagDto, OpphørDto, SanksjoneringDto
data class InnvilgelseOvergangsstønad(
    val periodeBegrunnelse: String?,
    val inntektBegrunnelse: String?,
    val perioder: List<VedtaksperiodeDto> = emptyList(),
    val inntekter: List<Inntekt> = emptyList(),
    val samordningsfradragType: SamordningsfradragType? = null,
) : VedtakDto(
        ResultatType.INNVILGE,
        "InnvilgelseOvergangsstønad",
    )

data class Avslå(
    val avslåÅrsak: AvslagÅrsak?,
    val avslåBegrunnelse: String?,
) : VedtakDto(ResultatType.AVSLÅ, "Avslag") {
    fun erGydlig(): Boolean = avslåÅrsak != AvslagÅrsak.KORTVARIG_AVBRUDD_JOBB && avslåÅrsak != AvslagÅrsak.MANGLENDE_OPPLYSNINGER
}

data class Opphør(
    val opphørFom: YearMonth,
    val begrunnelse: String?,
) : VedtakDto(ResultatType.OPPHØRT, "Opphør")

data class Sanksjonert(
    val sanksjonsårsak: Sanksjonsårsak,
    val periode: SanksjonertPeriodeDto,
    val internBegrunnelse: String,
) : VedtakDto(ResultatType.SANKSJONERE, "Sanksjonering")

data class SanksjonertPeriodeDto(
    @Deprecated("Bruk fomMåned", ReplaceWith("fom")) val årMånedFra: YearMonth,
    @Deprecated("Bruk tomMåned", ReplaceWith("tom")) val årMånedTil: YearMonth,
    @JsonIgnore
    val fom: YearMonth = årMånedFra,
    @JsonIgnore
    val tom: YearMonth = årMånedTil,
) {
    fun tilPeriode() = Månedsperiode(fom, tom)
}

fun VedtakDto.tilVedtak(
    behandlingId: UUID,
    stønadstype: StønadType,
): Vedtak =
    when (this) {
        is Avslå -> {
            Vedtak(
                behandlingId = behandlingId,
                avslåÅrsak = this.avslåÅrsak,
                avslåBegrunnelse = this.avslåBegrunnelse,
                resultatType = this.resultatType,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )
        }

        is InnvilgelseOvergangsstønad -> {
            Vedtak(
                behandlingId = behandlingId,
                periodeBegrunnelse = this.periodeBegrunnelse,
                inntektBegrunnelse = this.inntektBegrunnelse,
                resultatType = this.resultatType,
                perioder = PeriodeWrapper(perioder = this.perioder.tilDomene()),
                inntekter = InntektWrapper(inntekter = this.inntekter.tilInntektsperioder()),
                samordningsfradragType = this.samordningsfradragType,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )
        }

        is InnvilgelseBarnetilsyn -> {
            Vedtak(
                resultatType = this.resultatType,
                behandlingId = behandlingId,
                barnetilsyn =
                    BarnetilsynWrapper(
                        perioder = this.perioder.map { it.tilDomene() },
                        begrunnelse = this.begrunnelse,
                    ),
                kontantstøtte = KontantstøtteWrapper(perioder = this.perioderKontantstøtte.map { it.tilDomene() }, begrunnelse = this.kontantstøtteBegrunnelse),
                tilleggsstønad =
                    TilleggsstønadWrapper(
                        perioder = this.tilleggsstønad.perioder.map { it.tilDomene() },
                        begrunnelse = this.tilleggsstønad.begrunnelse,
                    ),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )
        }

        is InnvilgelseSkolepenger -> {
            Vedtak(
                resultatType = this.resultatType,
                behandlingId = behandlingId,
                skolepenger =
                    SkolepengerWrapper(
                        skoleårsperioder =
                            this.skoleårsperioder
                                .map { it.tilDomene() }
                                .sortedBy { it.perioder.first().periode },
                        begrunnelse = this.begrunnelse,
                    ),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )
        }

        is OpphørSkolepenger -> {
            Vedtak(
                resultatType = this.resultatType,
                behandlingId = behandlingId,
                skolepenger =
                    SkolepengerWrapper(
                        skoleårsperioder = this.skoleårsperioder.map { it.tilDomene() },
                        begrunnelse = this.begrunnelse,
                    ),
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )
        }

        is Opphør -> {
            Vedtak(
                behandlingId = behandlingId,
                avslåBegrunnelse = begrunnelse,
                resultatType = ResultatType.OPPHØRT,
                opphørFom = opphørFom,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
            )
        }

        is Sanksjonert -> {
            sanksjonertTilVedtak(behandlingId, stønadstype)
        }
    }

private fun Sanksjonert.sanksjonertTilVedtak(
    behandlingId: UUID,
    stønadstype: StønadType,
) = when (stønadstype) {
    StønadType.OVERGANGSSTØNAD -> {
        val vedtaksperiode =
            Vedtaksperiode(
                periode.tilPeriode(),
                AktivitetType.IKKE_AKTIVITETSPLIKT,
                VedtaksperiodeType.SANKSJON,
                this.sanksjonsårsak,
            )
        Vedtak(
            behandlingId = behandlingId,
            perioder = PeriodeWrapper(listOf(vedtaksperiode)),
            internBegrunnelse = this.internBegrunnelse,
            resultatType = ResultatType.SANKSJONERE,
            saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
        )
    }

    StønadType.BARNETILSYN -> {
        val vedtaksperiode =
            Barnetilsynperiode(
                periode = periode.tilPeriode(),
                utgifter = 0,
                barn = emptyList(),
                sanksjonsårsak = this.sanksjonsårsak,
                periodetype = PeriodetypeBarnetilsyn.SANKSJON_1_MND,
            )
        Vedtak(
            behandlingId = behandlingId,
            barnetilsyn = BarnetilsynWrapper(listOf(vedtaksperiode), begrunnelse = null),
            internBegrunnelse = this.internBegrunnelse,
            resultatType = ResultatType.SANKSJONERE,
            saksbehandlerIdent = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
        )
    }

    StønadType.SKOLEPENGER -> {
        error("Håndterer ikke sanksjon for skolepenger")
    }
}

fun Vedtak.tilVedtakDto(): VedtakDto =
    when (this.resultatType) {
        ResultatType.INNVILGE, ResultatType.INNVILGE_UTEN_UTBETALING -> {
            when {
                this.skolepenger != null -> mapInnvilgelseSkolepenger()
                this.barnetilsyn != null -> mapInnvilgelseBarnetilsyn(this.resultatType)
                this.perioder != null -> mapInnvilgelseOvergangsstønad()
                else -> error("Kan ikke mappe innvilget vedtak for vedtak=${this.behandlingId}")
            }
        }

        ResultatType.AVSLÅ -> {
            Avslå(
                avslåBegrunnelse = this.avslåBegrunnelse,
                avslåÅrsak = this.avslåÅrsak,
            )
        }

        ResultatType.OPPHØRT -> {
            if (this.skolepenger != null) {
                mapOpphørSkolepenger()
            } else {
                Opphør(
                    begrunnelse = this.avslåBegrunnelse,
                    opphørFom = YearMonth.from(this.opphørFom),
                )
            }
        }

        ResultatType.SANKSJONERE -> {
            val periode: VedtaksperiodeMedSanksjonsårsak =
                perioder?.perioder?.single()
                    ?: barnetilsyn?.perioder?.single()
                    ?: error("Mangler perioder for sanksjon")
            Sanksjonert(
                sanksjonsårsak = periode.sanksjonsårsak ?: error("Mangler perioder for sanksjon"),
                periode = periode.fraDomeneForSanksjon(),
                internBegrunnelse = this.internBegrunnelse ?: error("Sanksjon mangler intern begrunnelse."),
            )
        }

        else -> {
            throw Feil("Kan ikke sette vedtaksresultat som $this - ikke implementert")
        }
    }

private fun VedtaksperiodeMedSanksjonsårsak.fraDomeneForSanksjon(): SanksjonertPeriodeDto =
    SanksjonertPeriodeDto(
        årMånedFra = YearMonth.from(this.datoFra),
        årMånedTil = YearMonth.from(this.datoTil),
        fom = YearMonth.from(this.datoFra),
        tom = YearMonth.from(this.datoTil),
    )

fun Vedtak.mapInnvilgelseOvergangsstønad(): InnvilgelseOvergangsstønad {
    feilHvis(this.perioder == null || this.inntekter == null) {
        "Mangler felter fra vedtak for vedtak=${this.behandlingId}"
    }
    return InnvilgelseOvergangsstønad(
        periodeBegrunnelse = this.periodeBegrunnelse,
        inntektBegrunnelse = this.inntektBegrunnelse,
        perioder = this.perioder.perioder.fraDomene(),
        inntekter = this.inntekter.inntekter.tilInntekt(),
        samordningsfradragType = this.samordningsfradragType,
    )
}

private class VedtakDtoDeserializer : StdDeserializer<VedtakDto>(VedtakDto::class.java) {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext?,
    ): VedtakDto {
        val mapper = p.codec as ObjectMapper
        val node: JsonNode = mapper.readTree(p)

        // før vi har tatt i bruk @JsonTypeInfo så brukes denne for å mappe InnvilgelseBarnetilsyn
        if (node.get("_type") != null && node.get("_type").textValue() == "InnvilgelseBarnetilsyn") {
            return mapper.treeToValue(node, InnvilgelseBarnetilsyn::class.java)
        }

        if (node.get("_type") != null && node.get("_type").textValue() == "InnvilgelseSkolepenger") {
            return mapper.treeToValue(node, InnvilgelseSkolepenger::class.java)
        }

        if (node.get("_type") != null && node.get("_type").textValue() == "OpphørSkolepenger") {
            return mapper.treeToValue(node, OpphørSkolepenger::class.java)
        }

        if (node.get("_type") != null && node.get("_type").textValue() == "InnvilgelseBarnetilsynUtenUtbetaling") {
            return mapper
                .treeToValue(node, InnvilgelseBarnetilsyn::class.java)
                .copy(resultatType = ResultatType.INNVILGE_UTEN_UTBETALING)
        }

        return when (ResultatType.valueOf(node.get("resultatType").asText())) {
            ResultatType.INNVILGE -> mapper.treeToValue(node, InnvilgelseOvergangsstønad::class.java)
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
