package no.nav.familie.ef.sak.opplysninger.søknad.domain

import no.nav.familie.ef.sak.opplysninger.søknad.mapper.DokumentasjonMapper
import no.nav.familie.ef.sak.vilkår.dto.DokumentasjonDto
import java.time.LocalDateTime
import java.time.YearMonth

data class Søknadsverdier(
    val barn: Set<SøknadBarn>,
    val fødselsnummer: String,
    val medlemskap: Medlemskap,
    val sivilstandsplaner: Sivilstandsplaner?,
    val bosituasjon: Bosituasjon,
    val sivilstand: Sivilstand,
    val aktivitet: Aktivitet? = null, // Gjelder: OS og BT
    val situasjon: Situasjon? = null, // Gjelder: OS
    val datoMottatt: LocalDateTime,
    val søkerFra: YearMonth? = null,
    val opplysningerOmAdresse: OpplysningerOmAdresse?,
    val dokumentasjon: DokumentasjonFraSøknadDto
)

data class DokumentasjonFraSøknadDto(
    val erIArbeid: DokumentasjonDto? = null,
    val virksomhet: DokumentasjonDto? = null,
    val ikkeVilligTilÅTaImotTilbudOmArbeid: DokumentasjonDto? = null,
    val tidligereSamboerFortsattRegistrertPåAdresse: DokumentasjonDto? = null,
    val uformeltGift: DokumentasjonDto? = null,
    val uformeltSeparertEllerSkilt: DokumentasjonDto? = null,
    val separasjonsbekreftelse: DokumentasjonDto? = null,
    val samlivsbrudd: DokumentasjonDto? = null,
    val avtaleOmDeltBosted: DokumentasjonDto? = null,
    val samværsavtale: DokumentasjonDto? = null,
    val skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke: DokumentasjonDto? = null,
    val erklæringOmSamlivsbrudd: DokumentasjonDto? = null,
    val terminbekreftelse: DokumentasjonDto? = null,
    val barnepassordningFaktura: DokumentasjonDto? = null,
    val avtaleBarnepasser: DokumentasjonDto? = null,
    val arbeidstid: DokumentasjonDto? = null,
    val roterendeArbeidstid: DokumentasjonDto? = null,
    val spesielleBehov: DokumentasjonDto? = null,
    val sykdom: DokumentasjonDto? = null,
    val barnsSykdom: DokumentasjonDto? = null,
    val manglendeBarnepass: DokumentasjonDto? = null,
    val barnMedSærligeBehov: DokumentasjonDto? = null,
    val arbeidskontrakt: DokumentasjonDto? = null,
    val lærlingkontrakt: DokumentasjonDto? = null,
    val utdanningstilbud: DokumentasjonDto? = null,
    val reduksjonAvArbeidsforhold: DokumentasjonDto? = null,
    val oppsigelse: DokumentasjonDto? = null,
    val utdanningsutgifter: DokumentasjonDto? = null,
    val meldtAdresseendring: DokumentasjonDto? = null
)

fun SøknadsskjemaSkolepenger.tilSøknadsverdier() = Søknadsverdier(
    aktivitet = Aktivitet(underUtdanning = this.utdanning, tidligereUtdanninger = this.tidligereUtdanninger),
    barn = this.barn,
    fødselsnummer = this.fødselsnummer,
    medlemskap = this.medlemskap,
    sivilstand = this.sivilstand,
    sivilstandsplaner = this.sivilstandsplaner,
    bosituasjon = this.bosituasjon,
    situasjon = null,
    datoMottatt = this.datoMottatt,
    opplysningerOmAdresse = this.opplysningerOmAdresse,
    dokumentasjon = DokumentasjonMapper.tilDokumentasjonDto(this)
)

fun SøknadsskjemaBarnetilsyn.tilSøknadsverdier() = Søknadsverdier(
    aktivitet = this.aktivitet,
    barn = this.barn,
    fødselsnummer = this.fødselsnummer,
    medlemskap = this.medlemskap,
    sivilstand = this.sivilstand,
    sivilstandsplaner = this.sivilstandsplaner,
    bosituasjon = this.bosituasjon,
    situasjon = null,
    datoMottatt = this.datoMottatt,
    søkerFra = this.søkerFra,
    opplysningerOmAdresse = this.opplysningerOmAdresse,
    dokumentasjon = DokumentasjonMapper.tilDokumentasjonDto(this)

)

fun SøknadsskjemaOvergangsstønad.tilSøknadsverdier() = Søknadsverdier(
    aktivitet = this.aktivitet,
    barn = this.barn,
    fødselsnummer = this.fødselsnummer,
    medlemskap = this.medlemskap,
    sivilstand = this.sivilstand,
    sivilstandsplaner = this.sivilstandsplaner,
    bosituasjon = this.bosituasjon,
    situasjon = this.situasjon,
    datoMottatt = this.datoMottatt,
    søkerFra = this.søkerFra,
    opplysningerOmAdresse = this.opplysningerOmAdresse,
    dokumentasjon = DokumentasjonMapper.tilDokumentasjonDto(this)
)
