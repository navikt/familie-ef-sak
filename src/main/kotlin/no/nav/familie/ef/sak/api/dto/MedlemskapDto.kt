package no.nav.familie.ef.sak.api.dto

import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeInfo
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
                                         val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
                                         val medlUnntak: MedlUnntakDto)

data class MedlUnntakDto (val gyldigeVedtaksPerioder : List<MedlUnntaksperiodeDto> )

data class MedlUnntaksperiodeDto(val fraogmedDato: LocalDate, val tilogmedDato: LocalDate, val erMedlemIFolketrygden: Boolean)

fun Medlemskapsinfo.tilDto(): MedlUnntakDto =
        MedlUnntakDto(this.gyldigePerioder.map { it.tilDto() })


fun PeriodeInfo.tilDto(): MedlUnntaksperiodeDto =
        MedlUnntaksperiodeDto(this.fom, this.tom, this.gjelderMedlemskapIFolketrygden)

data class UtenlandsoppholdDto(val fraDato: LocalDate,
                               val tilDato: LocalDate,
                               val årsak: String)
