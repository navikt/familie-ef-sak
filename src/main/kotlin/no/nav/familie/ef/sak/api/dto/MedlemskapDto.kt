package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.vurdering.medlemskap.Medlemskapshistorikk
import java.time.LocalDate

data class MedlemskapDto(val søknadGrunnlag: MedlemskapSøknadGrunnlagDto,
                         val registerGrunnlag: MedlemskapRegisterGrunnlagDto,
                         val vurdering: VurderingDto? = null)

data class MedlemskapSøknadGrunnlagDto(val bosattNorgeSisteÅrene: Boolean,
                                       val oppholderDuDegINorge: Boolean,
                                       val utenlandsopphold: List<UtenlandsoppholdDto>)

//
data class MedlemskapRegisterGrunnlagDto(val nåværendeStatsborgerskap: List<String>,
                                         val statsborgerskap: List<StatsborgerskapDto>,
                                         val oppholdstatus: String?, //TODO må sjekkes med Mirja, ref hvilken data som finnes i PDL
                                         val medlemskapshistorikk: Medlemskapshistorikk)

class VurderingDto

data class UtenlandsoppholdDto(val fra: LocalDate,
                               val til: LocalDate,
                               val årsak: String)
