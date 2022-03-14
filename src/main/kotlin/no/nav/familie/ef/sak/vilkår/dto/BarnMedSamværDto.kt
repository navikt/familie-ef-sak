package no.nav.familie.ef.sak.vilkår.dto

import java.time.LocalDate
import java.util.UUID

data class BarnMedSamværDto(
        val barnId: UUID,
        val søknadsgrunnlag: BarnMedSamværSøknadsgrunnlagDto,
        val registergrunnlag: BarnMedSamværRegistergrunnlagDto,
        val barnepass: BarnepassDto? = null
)


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
        val beskrivSamværUtenBarn: String?
)

data class BarnMedSamværRegistergrunnlagDto(
        val id: UUID,
        val navn: String?,
        val fødselsnummer: String?,
        val harSammeAdresse: Boolean?,
        val forelder: AnnenForelderDto?,
        val dødsdato: LocalDate? = null,
        val fødselsdato: LocalDate?,
)

data class AnnenForelderDto(
        val navn: String?,
        val fødselsnummer: String?,
        val fødselsdato: LocalDate?,
        val bosattINorge: Boolean?,
        val land: String?,
        val dødsfall: LocalDate? = null,
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
        val beløp: Int
)