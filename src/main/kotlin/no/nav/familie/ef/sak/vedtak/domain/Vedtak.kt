package no.nav.familie.ef.sak.vedtak.domain

import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID


data class Vedtak(@Id
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
                  val opphørFom: LocalDate? = null,
                  val barnetilsyn: BarnetilsynWrapper? = null,
                  @Column("kontantstotte")
                  val kontantstøtte: KontantstøtteWrapper? = null,
                  @Column("tilleggsstonad")
                  val tilleggsstønad: TilleggsstønadWrapper? = null,
                  val skolepenger: SkolepengerWrapper? = null,
                  val beslutterIdent: String? = null,
                  @Column("sanksjon_arsak")
                  val sanksjonsårsak: Sanksjonsårsak? = null,
                  val internBegrunnelse: String? = null)

data class Vedtaksperiode(
        val datoFra: LocalDate,
        val datoTil: LocalDate,
        val aktivitet: AktivitetType,
        val periodeType: VedtaksperiodeType)

@Improvement("Kan barnetilsynperiode og vedtaksperiode sees på som én ting?")
data class Barnetilsynperiode(val datoFra: LocalDate,
                              val datoTil: LocalDate,
                              val utgifter: Int,
                              val barn: List<UUID>)

data class UtgiftsperideSkolepenger(val studietype: SkolepengerStudietype,
                                    val datoFra: LocalDate,
                                    val datoTil: LocalDate,
                                    val studiebelastning: Int,
                                    val utgifter: Int)

enum class SkolepengerStudietype {
     HØGSKOLE_UNIVERSITET
}

data class PeriodeMedBeløp(val datoFra: LocalDate,
                           val datoTil: LocalDate,
                           val beløp: Int) {

    fun tilDto() = PeriodeMedBeløpDto(årMånedFra = YearMonth.from(datoFra),
                                      årMånedTil = YearMonth.from(datoTil),
                                      beløp = beløp)
}

data class PeriodeWrapper(val perioder: List<Vedtaksperiode>)
data class InntektWrapper(val inntekter: List<Inntektsperiode>)
data class TilleggsstønadWrapper(val harTilleggsstønad: Boolean,
                                 val perioder: List<PeriodeMedBeløp>,
                                 val begrunnelse: String?)

data class KontantstøtteWrapper(val perioder: List<PeriodeMedBeløp>)

data class BarnetilsynWrapper(val perioder: List<Barnetilsynperiode>,
                              val begrunnelse: String?)

data class SkolepengerWrapper(val perioder: List<UtgiftsperideSkolepenger>,
                              val begrunnelse: String?)

enum class VedtaksperiodeType {
    FORLENGELSE,
    HOVEDPERIODE,
    MIDLERTIDIG_OPPHØR,
    MIGRERING,
    PERIODE_FØR_FØDSEL,
    SANKSJON,
    UTVIDELSE,
    NY_PERIODE_FOR_NYTT_BARN
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
}

enum class AvslagÅrsak {
    VILKÅR_IKKE_OPPFYLT,
    BARN_OVER_ÅTTE_ÅR,
    STØNADSTID_OPPBRUKT,
    MANGLENDE_OPPLYSNINGER,
}

enum class SamordningsfradragType {
    GJENLEVENDEPENSJON,
    UFØRETRYGD,
}
