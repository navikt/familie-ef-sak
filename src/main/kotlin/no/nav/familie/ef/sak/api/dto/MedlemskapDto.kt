package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class MedlemskapDto(val søknadsgrunnlag: MedlemskapSøknadsgrunnlagDto,
                         val registergrunnlag: MedlemskapRegistergrunnlagDto)

data class MedlemskapSøknadsgrunnlagDto(val bosattNorgeSisteÅrene: Boolean,
                                        val oppholderDuDegINorge: Boolean,
                                        val utenlandsopphold: List<UtenlandsoppholdDto>)

data class MedlemskapRegistergrunnlagDto(val nåværendeStatsborgerskap: List<String>,
                                         val statsborgerskap: List<StatsborgerskapDto>,
                                         val oppholdstatus: List<OppholdstillatelseDto>,
                                         val bostedsadresse: List<AdresseDto>,
                                         val innflytting: List<InnflyttingDto>,
                                         val utflytting: List<UtflyttingDto>,
                                         val folkeregisterpersonstatus: Folkeregisterpersonstatus?)

data class UtenlandsoppholdDto(val fraDato: LocalDate,
                               val tilDato: LocalDate,
                               val årsak: String)
