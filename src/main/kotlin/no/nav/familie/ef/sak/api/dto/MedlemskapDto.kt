package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class MedlemskapDto(val søknadGrunnlag: MedlemskapSøknadGrunnlagDto,
                         val registerGrunnlag: MedlemskapRegisterGrunnlagDto,
                         val vurdering: VurderingDto? = null)

data class MedlemskapSøknadGrunnlagDto(val bosattNorgeSisteÅrene: Boolean,
                                       val oppholderDuDegINorge: Boolean,
                                       val utenlandsopphold: List<UtenlandsoppholdDto>)

data class MedlemskapRegisterGrunnlagDto(val nåværendeStatsborgerskap: List<String>,
                                         val statsborgerskap: List<StatsborgerskapDto>,
                                         val oppholdstatus: List<OppholdstillatelseDto>)

data class UtenlandsoppholdDto(val fra: LocalDate,
                               val til: LocalDate,
                               val årsak: String)
