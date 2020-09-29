package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import java.time.LocalDate

data class MedlemskapDto(val søknadGrunnlag: MedlemskapSøknadGrunnlagDto,
                         val registerGrunnlag: MedlemskapRegisterGrunnlagDto,
                         val vurdering: VurderingDto? = null)

data class MedlemskapSøknadGrunnlagDto(val bosattNorgeSisteÅrene: Boolean,
                                       val oppholderDuDegINorge: Boolean,
                                       val utenlandsopphold: List<UtenlandsoppholdDto>)

data class MedlemskapRegisterGrunnlagDto(val nåværendeStatsborgerskap: List<String>,
                                         val statsborgerskap: List<StatsborgerskapDto>,
                                         val oppholdstatus: List<OppholdstillatelseDto>, //TODO må sjekkes med Mirja, ref hvilken data som finnes i PDL
                                         val medlemskapsinfo: Medlemskapsinfo) //TODO: Lag en DTO-klasse for denne i stedet for å sende opp integrasjonsklassen

class VurderingDto

data class UtenlandsoppholdDto(val fra: LocalDate,
                               val til: LocalDate,
                               val årsak: String)
