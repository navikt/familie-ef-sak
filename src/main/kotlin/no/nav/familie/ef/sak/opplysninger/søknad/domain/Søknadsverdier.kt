package no.nav.familie.ef.sak.opplysninger.søknad.domain

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
    val dokumentasjon: DokumentasjonFraSøknadDto,
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
    val utdanningsutgifter: DokumentasjonDto? = null
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
    dokumentasjon = tilDokumentasjon(this)
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
    dokumentasjon = tilDokumentasjon(this)

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
    dokumentasjon = tilDokumentasjon(this)
)

fun tilDokumentasjon(søknadsskjema: SøknadsskjemaBarnetilsyn): DokumentasjonFraSøknadDto =
    DokumentasjonFraSøknadDto(
        erIArbeid = søknadsskjema.aktivitet.erIArbeidDokumentasjon?.tilDto(),
        virksomhet = søknadsskjema.aktivitet.virksomhet?.dokumentasjon?.tilDto(),
        ikkeVilligTilÅTaImotTilbudOmArbeid = søknadsskjema.aktivitet.arbeidssøker?.ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon?.tilDto(),
        tidligereSamboerFortsattRegistrertPåAdresse = søknadsskjema.bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.tilDto(),
        uformeltGift = søknadsskjema.sivilstand.erUformeltGiftDokumentasjon?.tilDto(),
        uformeltSeparertEllerSkilt = søknadsskjema.sivilstand.erUformeltSeparertEllerSkiltDokumentasjon?.tilDto(),
        separasjonsbekreftelse = søknadsskjema.sivilstand.separasjonsbekreftelse?.tilDto(),
        samlivsbrudd = søknadsskjema.sivilstand.samlivsbruddsdokumentasjon?.tilDto(),
        avtaleOmDeltBosted = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.avtaleOmDeltBosted }?.tilDto(),
        samværsavtale = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.samværsavtale }?.tilDto(),
        skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke }?.tilDto(),
        erklæringOmSamlivsbrudd = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.erklæringOmSamlivsbrudd }?.tilDto(),
        terminbekreftelse = søknadsskjema.barn.firstNotNullOfOrNull { it.terminbekreftelse }?.tilDto(),
        barnepassordningFaktura = søknadsskjema.dokumentasjon.barnepassordningFaktura?.tilDto(),
        avtaleBarnepasser = søknadsskjema.dokumentasjon.avtaleBarnepasser?.tilDto(),
        arbeidstid = søknadsskjema.dokumentasjon.arbeidstid?.tilDto(),
        roterendeArbeidstid = søknadsskjema.dokumentasjon.roterendeArbeidstid?.tilDto(),
        spesielleBehov = søknadsskjema.dokumentasjon.spesielleBehov?.tilDto(),
    )

fun tilDokumentasjon(søknadsskjema: SøknadsskjemaOvergangsstønad): DokumentasjonFraSøknadDto =
    DokumentasjonFraSøknadDto(
        erIArbeid = søknadsskjema.aktivitet.erIArbeidDokumentasjon?.tilDto(),
        virksomhet = søknadsskjema.aktivitet.virksomhet?.dokumentasjon?.tilDto(),
        ikkeVilligTilÅTaImotTilbudOmArbeid = søknadsskjema.aktivitet.arbeidssøker?.ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon?.tilDto(),
        tidligereSamboerFortsattRegistrertPåAdresse = søknadsskjema.bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.tilDto(),
        uformeltGift = søknadsskjema.sivilstand.erUformeltGiftDokumentasjon?.tilDto(),
        uformeltSeparertEllerSkilt = søknadsskjema.sivilstand.erUformeltSeparertEllerSkiltDokumentasjon?.tilDto(),
        separasjonsbekreftelse = søknadsskjema.sivilstand.separasjonsbekreftelse?.tilDto(),
        samlivsbrudd = søknadsskjema.sivilstand.samlivsbruddsdokumentasjon?.tilDto(),
        avtaleOmDeltBosted = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.avtaleOmDeltBosted }?.tilDto(),
        samværsavtale = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.samværsavtale }?.tilDto(),
        skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke }?.tilDto(),
        erklæringOmSamlivsbrudd = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.erklæringOmSamlivsbrudd }?.tilDto(),
        terminbekreftelse = søknadsskjema.barn.firstNotNullOfOrNull { it.terminbekreftelse }?.tilDto(),
        sykdom = søknadsskjema.situasjon.sykdom?.tilDto(),
        barnsSykdom = søknadsskjema.situasjon.barnsSykdom?.tilDto(),
        manglendeBarnepass = søknadsskjema.situasjon.manglendeBarnepass?.tilDto(),
        barnMedSærligeBehov = søknadsskjema.situasjon.barnMedSærligeBehov?.tilDto(),
        arbeidskontrakt = søknadsskjema.situasjon.arbeidskontrakt?.tilDto(),
        lærlingkontrakt = søknadsskjema.situasjon.lærlingkontrakt?.tilDto(),
        utdanningstilbud = søknadsskjema.situasjon.utdanningstilbud?.tilDto(),
        reduksjonAvArbeidsforhold = søknadsskjema.situasjon.reduksjonAvArbeidsforholdDokumentasjon?.tilDto(),
        oppsigelse = søknadsskjema.situasjon.oppsigelseDokumentasjon?.tilDto(),
    )

fun tilDokumentasjon(søknadsskjema: SøknadsskjemaSkolepenger): DokumentasjonFraSøknadDto =
    DokumentasjonFraSøknadDto(
        tidligereSamboerFortsattRegistrertPåAdresse = søknadsskjema.bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.tilDto(),
        uformeltGift = søknadsskjema.sivilstand.erUformeltGiftDokumentasjon?.tilDto(),
        uformeltSeparertEllerSkilt = søknadsskjema.sivilstand.erUformeltSeparertEllerSkiltDokumentasjon?.tilDto(),
        separasjonsbekreftelse = søknadsskjema.sivilstand.separasjonsbekreftelse?.tilDto(),
        samlivsbrudd = søknadsskjema.sivilstand.samlivsbruddsdokumentasjon?.tilDto(),
        avtaleOmDeltBosted = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.avtaleOmDeltBosted }?.tilDto(),
        samværsavtale = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.samværsavtale }?.tilDto(),
        skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke }?.tilDto(),
        erklæringOmSamlivsbrudd = søknadsskjema.barn.firstNotNullOfOrNull { it.samvær?.erklæringOmSamlivsbrudd }?.tilDto(),
        terminbekreftelse = søknadsskjema.barn.firstNotNullOfOrNull { it.terminbekreftelse }?.tilDto(),
        utdanningsutgifter = søknadsskjema.utdanningsutgifter?.tilDto()
    )
