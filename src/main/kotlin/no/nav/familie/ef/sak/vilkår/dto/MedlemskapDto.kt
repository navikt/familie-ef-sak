package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.OppholdstillatelseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeInfo
import java.time.LocalDate

data class MedlemskapDto(
    val søknadsgrunnlag: MedlemskapSøknadsgrunnlagDto?,
    val registergrunnlag: MedlemskapRegistergrunnlagDto
)

data class MedlemskapSøknadsgrunnlagDto(
    val bosattNorgeSisteÅrene: Boolean,
    val oppholderDuDegINorge: Boolean,
    val utenlandsopphold: List<UtenlandsoppholdDto>
)

data class MedlemskapRegistergrunnlagDto(
    val nåværendeStatsborgerskap: List<String>,
    val statsborgerskap: List<StatsborgerskapDto>,
    val oppholdstatus: List<OppholdstillatelseDto>,
    val bostedsadresse: List<AdresseDto>,
    val innflytting: List<InnflyttingDto>,
    val utflytting: List<UtflyttingDto>,
    val folkeregisterpersonstatus: Folkeregisterpersonstatus?,
    val medlUnntak: MedlUnntakDto
)

data class MedlUnntakDto(val gyldigeVedtaksPerioder: List<MedlUnntaksperiodeDto>)

data class MedlUnntaksperiodeDto(val fraogmedDato: LocalDate, val tilogmedDato: LocalDate, val erMedlemIFolketrygden: Boolean)

fun Medlemskapsinfo.tilDto(): MedlUnntakDto =
    MedlUnntakDto(this.gyldigePerioder.map { it.tilDto() })

fun PeriodeInfo.tilDto(): MedlUnntaksperiodeDto =
    MedlUnntaksperiodeDto(this.fom, this.tom, this.gjelderMedlemskapIFolketrygden)

data class UtenlandsoppholdDto(
    @Deprecated("Bruk periode!", ReplaceWith("periode.fomDato")) val fraDato: LocalDate? = null,
    @Deprecated("Bruk periode!", ReplaceWith("periode.tomDato")) val tilDato: LocalDate? = null,
    val periode: Datoperiode = Datoperiode(
        fraDato ?: error("Periode eller fraDato må ha verdi"),
        tilDato ?: error("Periode eller tilDato må ha verdi")
    ),
    val årsak: String
)
