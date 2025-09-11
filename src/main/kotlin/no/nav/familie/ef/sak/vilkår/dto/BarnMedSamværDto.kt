package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.DeltBostedDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.vilkår.regler.BarnForelderLangAvstandTilSøker
import java.time.LocalDate
import java.util.UUID

data class BarnMedSamværDto(
    val barnId: UUID,
    val søknadsgrunnlag: BarnMedSamværSøknadsgrunnlagDto,
    val registergrunnlag: BarnMedSamværRegistergrunnlagDto,
    val barnepass: BarnepassDto? = null,
) {
    fun mapTilBarnForelderLangAvstandTilSøker(): BarnForelderLangAvstandTilSøker =
        BarnForelderLangAvstandTilSøker(
            barnId = barnId,
            langAvstandTilSøker = registergrunnlag.forelder?.avstandTilSøker?.langAvstandTilSøker ?: LangAvstandTilSøker.UKJENT,
            borAnnenForelderISammeHus = søknadsgrunnlag.borAnnenForelderISammeHus ?: "ukjent",
        )
}

data class BarnMedSamværSøknadsgrunnlagDto(
    val id: UUID,
    val navn: String?,
    val fødselTermindato: LocalDate?,
    val harSammeAdresse: Boolean?,
    val skalBoBorHosSøker: String?,
    val forelder: AnnenForelderDto?,
    val ikkeOppgittAnnenForelderBegrunnelse: String?,
    val spørsmålAvtaleOmDeltBosted: Boolean?,
    val skalAnnenForelderHaSamvær: String?,
    val harDereSkriftligAvtaleOmSamvær: String?,
    val hvordanPraktiseresSamværet: String?,
    val borAnnenForelderISammeHus: String?,
    val borAnnenForelderISammeHusBeskrivelse: String?,
    val harDereTidligereBoddSammen: Boolean?,
    val nårFlyttetDereFraHverandre: LocalDate?,
    val hvorMyeErDuSammenMedAnnenForelder: String?,
    val beskrivSamværUtenBarn: String?,
) {
    fun erTerminbarn() = this.fødselTermindato != null
}

data class BarnMedSamværRegistergrunnlagDto(
    val id: UUID,
    val navn: String?,
    val fødselsnummer: String?,
    val harSammeAdresse: Boolean?,
    val deltBostedPerioder: List<DeltBostedDto>,
    val harDeltBostedVedGrunnlagsdataopprettelse: Boolean,
    val forelder: AnnenForelderDto?,
    val dødsdato: LocalDate? = null,
    val fødselsdato: LocalDate?,
    val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
    val adresse: String?,
) {
    fun erBosatt() = this.folkeregisterpersonstatus == Folkeregisterpersonstatus.BOSATT
}

data class AnnenForelderDto(
    val navn: String?,
    val fødselsnummer: String?,
    val fødselsdato: LocalDate?,
    val bosattINorge: Boolean?,
    val land: String?,
    val visningsadresse: String?,
    val dødsfall: LocalDate? = null,
    val tidligereVedtaksperioder: TidligereVedtaksperioderDto? = null,
    val avstandTilSøker: AvstandTilSøkerDto,
    val erKopiertFraAnnetBarn: Boolean? = false,
)

data class BarnepassDto(
    val id: UUID,
    val skalHaBarnepass: Boolean? = null,
    val barnepassordninger: List<BarnepassordningDto> = emptyList(),
    val årsakBarnepass: String? = null,
)

data class BarnepassordningDto(
    val type: String,
    val navn: String,
    val fra: LocalDate,
    val til: LocalDate,
    val beløp: Int,
)

data class AvstandTilSøkerDto(
    val avstand: Long?,
    val langAvstandTilSøker: LangAvstandTilSøker,
)

enum class LangAvstandTilSøker {
    JA,
    JA_UPRESIS,
    UKJENT,
}
