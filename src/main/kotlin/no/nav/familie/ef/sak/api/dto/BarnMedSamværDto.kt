package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate
import java.util.UUID

data class BarnMedSamværDto(
        val barnId: UUID,
        val søknadsgrunnlag: BarnMedSamværSøknadsgrunnlagDto,
        val registergrunnlag: BarnMedSamværRegistergrunnlagDto?
)


data class BarnMedSamværSøknadsgrunnlagDto(
        val id: UUID,
        val navn: String?,
        val fødselsnummer: String?,
        val fødselTermindato: LocalDate?,
        val erBarnetFødt: Boolean,
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
)

data class AnnenForelderDto(
        val navn: String?,
        val fødselsnummer: String?,
        val fødselsdato: LocalDate?,
        val bosattINorge: Boolean?,
        val land: String?,
)