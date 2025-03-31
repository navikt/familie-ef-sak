package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Adresseopplysninger
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aktivitet
import no.nav.familie.ef.sak.opplysninger.søknad.domain.BarnetilsynDokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Bosituasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.DokumentasjonFraSøknadDto
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Situasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaSkolepenger

object DokumentasjonMapper {
    fun tilDokumentasjonDto(søknadsskjema: SøknadsskjemaBarnetilsyn): DokumentasjonFraSøknadDto =
        tilDokumentasjonDto(
            aktivitet = søknadsskjema.aktivitet,
            bosituasjon = søknadsskjema.bosituasjon,
            sivilstand = søknadsskjema.sivilstand,
            barn = søknadsskjema.barn,
            situasjon = null,
            barnetilsynDokumentasjon = søknadsskjema.dokumentasjon,
            utdanningsutgifter = null,
            utdanningDokumentasjon = null,
            adresseopplysninger = søknadsskjema.adresseopplysninger,
        )

    fun tilDokumentasjonDto(søknadsskjema: SøknadsskjemaOvergangsstønad): DokumentasjonFraSøknadDto =
        tilDokumentasjonDto(
            aktivitet = søknadsskjema.aktivitet,
            bosituasjon = søknadsskjema.bosituasjon,
            sivilstand = søknadsskjema.sivilstand,
            barn = søknadsskjema.barn,
            situasjon = søknadsskjema.situasjon,
            barnetilsynDokumentasjon = null,
            utdanningsutgifter = null,
            utdanningDokumentasjon = null,
            adresseopplysninger = søknadsskjema.adresseopplysninger,
        )

    fun tilDokumentasjonDto(søknadsskjema: SøknadsskjemaSkolepenger): DokumentasjonFraSøknadDto =
        tilDokumentasjonDto(
            aktivitet = null,
            bosituasjon = søknadsskjema.bosituasjon,
            sivilstand = søknadsskjema.sivilstand,
            barn = søknadsskjema.barn,
            situasjon = null,
            barnetilsynDokumentasjon = null,
            utdanningsutgifter = søknadsskjema.utdanningsutgifter,
            utdanningDokumentasjon = søknadsskjema.utdanningDokumentasjon,
            adresseopplysninger = søknadsskjema.adresseopplysninger,
        )

    fun tilDokumentasjonDto(
        aktivitet: Aktivitet?,
        bosituasjon: Bosituasjon,
        sivilstand: Sivilstand,
        barn: Set<SøknadBarn>,
        situasjon: Situasjon?,
        barnetilsynDokumentasjon: BarnetilsynDokumentasjon?,
        utdanningsutgifter: Dokumentasjon?,
        utdanningDokumentasjon: Dokumentasjon?,
        adresseopplysninger: Adresseopplysninger?,
    ): DokumentasjonFraSøknadDto =
        DokumentasjonFraSøknadDto(
            erIArbeid = aktivitet?.erIArbeidDokumentasjon?.tilDto(),
            virksomhet = aktivitet?.virksomhet?.dokumentasjon?.tilDto(),
            ikkeVilligTilÅTaImotTilbudOmArbeid = aktivitet?.arbeidssøker?.ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon?.tilDto(),
            tidligereSamboerFortsattRegistrertPåAdresse = bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.tilDto(),
            uformeltGift = sivilstand.erUformeltGiftDokumentasjon?.tilDto(),
            uformeltSeparertEllerSkilt = sivilstand.erUformeltSeparertEllerSkiltDokumentasjon?.tilDto(),
            separasjonsbekreftelse = sivilstand.separasjonsbekreftelse?.tilDto(),
            samlivsbrudd = sivilstand.samlivsbruddsdokumentasjon?.tilDto(),
            /**
             * Dokumentasjonsbehov som er knyttet til barn blir satt likt for alle barna fra søknaden.
             * Henter derfor bare ut for ett barn dersom det eksisterer
             */
            avtaleOmDeltBosted = barn.firstNotNullOfOrNull { it.samvær?.avtaleOmDeltBosted }?.tilDto(),
            samværsavtale = barn.firstNotNullOfOrNull { it.samvær?.samværsavtale }?.tilDto(),
            skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke =
                barn
                    .firstNotNullOfOrNull { it.samvær?.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke }
                    ?.tilDto(),
            erklæringOmSamlivsbrudd = barn.firstNotNullOfOrNull { it.samvær?.erklæringOmSamlivsbrudd }?.tilDto(),
            terminbekreftelse = barn.firstNotNullOfOrNull { it.terminbekreftelse }?.tilDto(),
            barnepassordningFaktura = barnetilsynDokumentasjon?.barnepassordningFaktura?.tilDto(),
            avtaleBarnepasser = barnetilsynDokumentasjon?.avtaleBarnepasser?.tilDto(),
            arbeidstid = barnetilsynDokumentasjon?.arbeidstid?.tilDto(),
            roterendeArbeidstid = barnetilsynDokumentasjon?.roterendeArbeidstid?.tilDto(),
            spesielleBehov = barnetilsynDokumentasjon?.spesielleBehov?.tilDto(),
            sykdom = situasjon?.sykdom?.tilDto(),
            barnsSykdom = situasjon?.barnsSykdom?.tilDto(),
            manglendeBarnepass = situasjon?.manglendeBarnepass?.tilDto(),
            barnMedSærligeBehov = situasjon?.barnMedSærligeBehov?.tilDto(),
            arbeidskontrakt = situasjon?.arbeidskontrakt?.tilDto(),
            lærlingkontrakt = situasjon?.lærlingkontrakt?.tilDto(),
            utdanningstilbud = utdanningDokumentasjon?.tilDto() ?: situasjon?.utdanningstilbud?.tilDto(),
            reduksjonAvArbeidsforhold = situasjon?.reduksjonAvArbeidsforholdDokumentasjon?.tilDto(),
            oppsigelse = situasjon?.oppsigelseDokumentasjon?.tilDto(),
            utdanningsutgifter = utdanningsutgifter?.tilDto(),
            meldtAdresseendring = adresseopplysninger?.dokumentasjonAdresseendring?.tilDto(),
        )
}
