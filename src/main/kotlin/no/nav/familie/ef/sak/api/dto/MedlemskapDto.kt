package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import java.time.LocalDate

data class MedlemskapDto(val søknadsgrunnlag: MedlemskapSøknadsgrunnlagDto,
                         val pdlMedlemskapgrunnlag: PdlMedlemskapgrunnlagDto,
                         val medlMedlemskapgrunnlag: Medlemskapsinfo)

data class MedlemskapSøknadsgrunnlagDto(val bosattNorgeSisteÅrene: Boolean,
                                        val oppholderDuDegINorge: Boolean,
                                        val utenlandsopphold: List<UtenlandsoppholdDto>)

data class PdlMedlemskapgrunnlagDto(val nåværendeStatsborgerskap: List<String>,
                                    val statsborgerskap: List<StatsborgerskapDto>,
                                    val oppholdstatus: List<OppholdstillatelseDto>,
                                    val bostedsadresse: List<AdresseDto>,
                                    val innflytting: List<InnflyttingDto>,
                                    val utflytting: List<UtflyttingDto>,
                                    val folkeregisterpersonstatus: Folkeregisterpersonstatus?)


data class UtenlandsoppholdDto(val fraDato: LocalDate,
                               val tilDato: LocalDate,
                               val årsak: String)
