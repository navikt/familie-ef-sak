package no.nav.familie.ef.sak.vedtak.domain

import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

data class Vedtak(
    @Id
    val behandlingId: UUID,
    val resultatType: ResultatType,
    val periodeBegrunnelse: String? = null,
    val inntektBegrunnelse: String? = null,
    @Column("avsla_begrunnelse")
    val avslåBegrunnelse: String? = null,
    @Column("avsla_arsak")
    val avslåÅrsak: AvslagÅrsak? = null,
    val perioder: PeriodeWrapper? = null,
    val inntekter: InntektWrapper? = null,
    val samordningsfradragType: SamordningsfradragType? = null,
    val saksbehandlerIdent: String? = null,
    @Column("opphor_fom")
    val opphørFom: YearMonth? = null,
    val barnetilsyn: BarnetilsynWrapper? = null,
    @Column("kontantstotte")
    val kontantstøtte: KontantstøtteWrapper? = null,
    @Column("tilleggsstonad")
    val tilleggsstønad: TilleggsstønadWrapper? = null,
    val skolepenger: SkolepengerWrapper? = null,
    val beslutterIdent: String? = null,
    val internBegrunnelse: String? = null,
    val opprettetTid: LocalDateTime = SporbarUtils.now(),
    val opprettetAv: String = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
) {
    fun erVedtakUtenBeslutter(): Boolean = resultatType == ResultatType.AVSLÅ && (avslåÅrsak == AvslagÅrsak.MINDRE_INNTEKTSENDRINGER || avslåÅrsak == AvslagÅrsak.KORTVARIG_AVBRUDD_JOBB)

    fun skalVedtakBesluttes(): Boolean = !erVedtakUtenBeslutter()

    fun utledVedtakErUtenBeslutter(): VedtakErUtenBeslutter = VedtakErUtenBeslutter(erVedtakUtenBeslutter())
}

data class VedtakErUtenBeslutter(
    val value: Boolean,
)

sealed interface VedtaksperiodeMedSanksjonsårsak {
    val datoFra: LocalDate
    val datoTil: LocalDate
    val sanksjonsårsak: Sanksjonsårsak?

    val periode get() = Månedsperiode(datoFra, datoTil)
}

data class Vedtaksperiode(
    override val datoFra: LocalDate,
    override val datoTil: LocalDate,
    val aktivitet: AktivitetType,
    val periodeType: VedtaksperiodeType,
    override val sanksjonsårsak: Sanksjonsårsak? = null,
) : VedtaksperiodeMedSanksjonsårsak {
    init {
        feilHvis(
            (periodeType != VedtaksperiodeType.SANKSJON && sanksjonsårsak != null) ||
                (periodeType == VedtaksperiodeType.SANKSJON && sanksjonsårsak == null) ||
                (periodeType == VedtaksperiodeType.SANKSJON && aktivitet != AktivitetType.IKKE_AKTIVITETSPLIKT),
        ) {
            "Ugyldig kombinasjon av sanksjon periodeType=$periodeType aktivitet=$aktivitet sanksjonsårsak=$sanksjonsårsak"
        }
        validerSanksjon1Måned()
    }

    constructor(
        periode: Månedsperiode,
        aktivitet: AktivitetType,
        periodeType: VedtaksperiodeType,
        sanksjonsårsak: Sanksjonsårsak? = null,
    ) : this(
        periode.fomDato,
        periode.tomDato,
        aktivitet,
        periodeType,
        sanksjonsårsak,
    )
}

private fun VedtaksperiodeMedSanksjonsårsak.validerSanksjon1Måned() {
    feilHvis(sanksjonsårsak != null && periode.lengdeIHeleMåneder() != 1L) {
        "Sanksjon må være en måned, fra=$datoFra til=$datoTil"
    }
}

enum class PeriodetypeBarnetilsyn {
    ORDINÆR,
    OPPHØR,
    SANKSJON_1_MND,
    ;

    fun midlertidigOpphørEllerSanksjon() = this == OPPHØR || this == SANKSJON_1_MND
}

enum class AktivitetstypeBarnetilsyn {
    I_ARBEID,
    FORBIGÅENDE_SYKDOM,
}

@Improvement("Kan barnetilsynperiode og vedtaksperiode sees på som én ting?")
data class Barnetilsynperiode(
    override val datoFra: LocalDate,
    override val datoTil: LocalDate,
    val utgifter: Int,
    val barn: List<UUID>,
    override val sanksjonsårsak: Sanksjonsårsak? = null,
    val periodetype: PeriodetypeBarnetilsyn,
    val aktivitetstype: AktivitetstypeBarnetilsyn? = null,
) : VedtaksperiodeMedSanksjonsårsak {
    init {
        validerSanksjon1Måned()
        feilHvis(
            (periodetype != PeriodetypeBarnetilsyn.SANKSJON_1_MND && sanksjonsårsak != null) ||
                (periodetype == PeriodetypeBarnetilsyn.SANKSJON_1_MND && sanksjonsårsak == null),
        ) {
            "Ugyldig kombinasjon av sanksjon periodeType=$periodetype sanksjonsårsak=$sanksjonsårsak"
        }
    }

    constructor(
        periode: Månedsperiode,
        utgifter: Int,
        barn: List<UUID>,
        sanksjonsårsak: Sanksjonsårsak? = null,
        periodetype: PeriodetypeBarnetilsyn,
        aktivitet: AktivitetstypeBarnetilsyn? = null,
    ) : this(
        datoFra = periode.fomDato,
        datoTil = periode.tomDato,
        utgifter = utgifter,
        barn = barn,
        periodetype = periodetype,
        sanksjonsårsak = sanksjonsårsak,
        aktivitetstype = aktivitet,
    )
}

data class SkoleårsperiodeSkolepenger(
    val perioder: List<DelårsperiodeSkoleårSkolepenger>,
    val utgiftsperioder: List<SkolepengerUtgift>,
)

data class DelårsperiodeSkoleårSkolepenger(
    val studietype: SkolepengerStudietype,
    val datoFra: LocalDate,
    val datoTil: LocalDate,
    val studiebelastning: Int,
) {
    constructor(
        studietype: SkolepengerStudietype,
        periode: Månedsperiode,
        studiebelastning: Int,
    ) : this(
        studietype,
        periode.fomDato,
        periode.tomDato,
        studiebelastning,
    )

    val periode get() = Månedsperiode(datoFra, datoTil)
}

data class SkolepengerUtgift(
    val id: UUID,
    val utgiftsdato: LocalDate,
    val utgifter: Int? = null,
    val stønad: Int,
)

enum class SkolepengerStudietype {
    HØGSKOLE_UNIVERSITET,
    VIDEREGÅENDE,
}

data class PeriodeMedBeløp(
    val datoFra: LocalDate,
    val datoTil: LocalDate,
    val beløp: Int,
) {
    constructor(periode: Månedsperiode, beløp: Int) : this(periode.fomDato, periode.tomDato, beløp)

    fun tilDto() =
        PeriodeMedBeløpDto(
            årMånedFra = periode.fom,
            årMånedTil = periode.tom,
            periode = periode,
            beløp = beløp,
        )

    val periode get() = Månedsperiode(datoFra, datoTil)
}

data class PeriodeWrapper(
    val perioder: List<Vedtaksperiode>,
)

data class InntektWrapper(
    val inntekter: List<Inntektsperiode>,
)

data class TilleggsstønadWrapper(
    val harTilleggsstønad: Boolean,
    val perioder: List<PeriodeMedBeløp>,
    val begrunnelse: String?,
)

data class KontantstøtteWrapper(
    val perioder: List<PeriodeMedBeløp>,
)

data class BarnetilsynWrapper(
    val perioder: List<Barnetilsynperiode>,
    val begrunnelse: String?,
)

data class SkolepengerWrapper(
    val skoleårsperioder: List<SkoleårsperiodeSkolepenger>,
    val begrunnelse: String?,
)

enum class VedtaksperiodeType {
    FORLENGELSE,
    HOVEDPERIODE,
    MIDLERTIDIG_OPPHØR,
    MIGRERING,
    PERIODE_FØR_FØDSEL,
    SANKSJON,
    UTVIDELSE,
    NY_PERIODE_FOR_NYTT_BARN,
    ;

    fun midlertidigOpphørEllerSanksjon() = this == MIDLERTIDIG_OPPHØR || this == SANKSJON
}

enum class AktivitetType {
    MIGRERING,
    IKKE_AKTIVITETSPLIKT,
    BARN_UNDER_ETT_ÅR,
    FORSØRGER_I_ARBEID,
    FORSØRGER_I_UTDANNING,
    FORSØRGER_REELL_ARBEIDSSØKER,
    FORSØRGER_ETABLERER_VIRKSOMHET,
    BARNET_SÆRLIG_TILSYNSKREVENDE,
    FORSØRGER_MANGLER_TILSYNSORDNING,
    FORSØRGER_ER_SYK,
    BARNET_ER_SYKT,
    UTVIDELSE_FORSØRGER_I_UTDANNING,
    UTVIDELSE_BARNET_SÆRLIG_TILSYNSKREVENDE,
    FORLENGELSE_MIDLERTIDIG_SYKDOM,
    FORLENGELSE_STØNAD_PÅVENTE_ARBEID,
    FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER,
    FORLENGELSE_STØNAD_PÅVENTE_OPPSTART_KVALIFISERINGSPROGRAM,
    FORLENGELSE_STØNAD_PÅVENTE_TILSYNSORDNING,
    FORLENGELSE_STØNAD_PÅVENTE_UTDANNING,
    FORLENGELSE_STØNAD_UT_SKOLEÅRET,
    ;

    fun manglerTilsyn(): Boolean = this == FORSØRGER_MANGLER_TILSYNSORDNING
}

enum class SamordningsfradragType {
    GJENLEVENDEPENSJON,
    UFØRETRYGD,
    EØS_FAMILIEYTELSE,
}
