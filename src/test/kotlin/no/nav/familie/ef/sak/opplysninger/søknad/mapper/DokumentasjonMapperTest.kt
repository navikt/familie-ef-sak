package no.nav.familie.ef.sak.no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aktivitet
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssøker
import no.nav.familie.ef.sak.opplysninger.søknad.domain.BarnetilsynDokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Bosituasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokument
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Medlemskap
import no.nav.familie.ef.sak.opplysninger.søknad.domain.OpplysningerOmAdresse
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Samvær
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Situasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstandsplaner
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadType
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.opplysninger.søknad.domain.UnderUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Virksomhet
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.DokumentasjonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class DokumentasjonMapperTest {

    private val dokumentasjonSendtInnTidligere = Dokumentasjon(true, emptyList())
    private val dokument = Dokument("1234", "Dokumentnavn")
    private val dokumentasjonMedVedlegg = Dokumentasjon(false, listOf(dokument))

    private val sivilstand = Sivilstand(
        erUformeltGiftDokumentasjon = dokumentasjonSendtInnTidligere,
        erUformeltSeparertEllerSkiltDokumentasjon = dokumentasjonMedVedlegg,
        separasjonsbekreftelse = dokumentasjonSendtInnTidligere,
        samlivsbruddsdokumentasjon = dokumentasjonSendtInnTidligere
    )

    private val medlemskap = Medlemskap(
        oppholderDuDegINorge = false,
        bosattNorgeSisteÅrene = false
    )

    private val bosituasjon = Bosituasjon(
        tidligereSamboerFortsattRegistrertPåAdresse = dokumentasjonMedVedlegg
    )

    private val søknadBarn = SøknadBarn(
        harSkalHaSammeAdresse = false,
        erBarnetFødt = false,
        terminbekreftelse = dokumentasjonMedVedlegg,
        samvær = Samvær(
            avtaleOmDeltBosted = dokumentasjonSendtInnTidligere,
            samværsavtale = dokumentasjonMedVedlegg,
            skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke = dokumentasjonSendtInnTidligere,
            erklæringOmSamlivsbrudd = dokumentasjonMedVedlegg
        ),
        ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
        lagtTilManuelt = false
    )

    private val aktivitet = Aktivitet(
        virksomhet = Virksomhet(virksomhetsbeskrivelse = "", dokumentasjon = dokumentasjonSendtInnTidligere),
        arbeidssøker = Arbeidssøker(
            registrertSomArbeidssøkerNav = false,
            villigTilÅTaImotTilbudOmArbeid = false,
            kanDuBegynneInnenEnUke = false,
            kanDuSkaffeBarnepassInnenEnUke = null,
            hvorØnskerDuArbeid = "",
            ønskerDuMinst50ProsentStilling = false,
            ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon = dokumentasjonSendtInnTidligere
        ),
        erIArbeidDokumentasjon = dokumentasjonSendtInnTidligere
    )
    private val situasjon = Situasjon(
        sykdom = dokumentasjonMedVedlegg,
        barnsSykdom = dokumentasjonSendtInnTidligere,
        manglendeBarnepass = dokumentasjonMedVedlegg,
        barnMedSærligeBehov = dokumentasjonSendtInnTidligere,
        arbeidskontrakt = dokumentasjonSendtInnTidligere,
        lærlingkontrakt = dokumentasjonSendtInnTidligere,
        utdanningstilbud = dokumentasjonMedVedlegg,
        reduksjonAvArbeidsforholdDokumentasjon = dokumentasjonSendtInnTidligere,
        oppsigelseDokumentasjon = dokumentasjonMedVedlegg
    )

    private val utdanning = UnderUtdanning(
        skoleUtdanningssted = "",
        linjeKursGrad = "",
        fra = LocalDate.now(),
        til = LocalDate.now(),
        offentligEllerPrivat = null,
        heltidEllerDeltid = null,
        hvorMyeSkalDuStudere = null,
        hvaErMåletMedUtdanningen = null,
        utdanningEtterGrunnskolen = false,
        semesteravgift = null,
        studieavgift = null,
        eksamensgebyr = null
    )

    private val opplysningerOmAdresse = OpplysningerOmAdresse(
        søkerBorPåRegistrertAdresse = false, harMeldtFlytteendring = true,
        dokumentasjonFlytteendring = dokumentasjonMedVedlegg
    )

    @Test
    internal fun `skal mappe dokumentasjonsbehov for overgangsstønad`() {
        val søknadOvergangsstønad = SøknadsskjemaOvergangsstønad(
            type = SøknadType.OVERGANGSSTØNAD, fødselsnummer = "1", navn = "",
            datoMottatt = LocalDateTime.now(),
            sivilstand = sivilstand,
            medlemskap = medlemskap,
            bosituasjon = bosituasjon,
            sivilstandsplaner = Sivilstandsplaner(),
            barn = setOf(søknadBarn),
            aktivitet = aktivitet,
            situasjon = situasjon,
            søkerFra = null, søkerFraBestemtMåned = false,
            opplysningerOmAdresse = opplysningerOmAdresse
        )

        val dokumentasjon = DokumentasjonMapper.tilDokumentasjonDto(søknadOvergangsstønad)

        assertThat(dokumentasjon.erIArbeid).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.virksomhet).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.ikkeVilligTilÅTaImotTilbudOmArbeid).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.tidligereSamboerFortsattRegistrertPåAdresse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.uformeltGift).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.uformeltSeparertEllerSkilt).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.separasjonsbekreftelse).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.samlivsbrudd).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.avtaleOmDeltBosted).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.samværsavtale).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke)
            .isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.erklæringOmSamlivsbrudd).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.terminbekreftelse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.barnepassordningFaktura).isEqualTo(null)
        assertThat(dokumentasjon.avtaleBarnepasser).isEqualTo(null)
        assertThat(dokumentasjon.arbeidstid).isEqualTo(null)
        assertThat(dokumentasjon.roterendeArbeidstid).isEqualTo(null)
        assertThat(dokumentasjon.spesielleBehov).isEqualTo(null)
        assertThat(dokumentasjon.sykdom).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.barnsSykdom).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.manglendeBarnepass).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.barnMedSærligeBehov).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.arbeidskontrakt).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.lærlingkontrakt).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.utdanningstilbud).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.reduksjonAvArbeidsforhold).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.oppsigelse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.utdanningsutgifter).isEqualTo(null)
        assertThat(dokumentasjon.meldtFlytteendring).isEqualTo(dokumentasjonMedVedlegg.tilDto())
    }

    @Test
    internal fun `skal mappe dokumentasjonsbehov for barnetilsyn`() {
        val søknadBarnetilsyn = SøknadsskjemaBarnetilsyn(
            type = SøknadType.BARNETILSYN, fødselsnummer = "1", navn = "",
            datoMottatt = LocalDateTime.now(),
            sivilstand = sivilstand,
            medlemskap = medlemskap,
            bosituasjon = bosituasjon,
            sivilstandsplaner = Sivilstandsplaner(),
            barn = setOf(søknadBarn),
            aktivitet = aktivitet,
            dokumentasjon = BarnetilsynDokumentasjon(
                barnepassordningFaktura = dokumentasjonSendtInnTidligere,
                avtaleBarnepasser = dokumentasjonSendtInnTidligere,
                arbeidstid = dokumentasjonSendtInnTidligere,
                roterendeArbeidstid = dokumentasjonMedVedlegg,
                spesielleBehov = dokumentasjonMedVedlegg

            ),
            søkerFra = null,
            søkerFraBestemtMåned = false,
            opplysningerOmAdresse = opplysningerOmAdresse
        )

        val dokumentasjon = DokumentasjonMapper.tilDokumentasjonDto(søknadBarnetilsyn)

        assertThat(dokumentasjon.erIArbeid).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.virksomhet).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.ikkeVilligTilÅTaImotTilbudOmArbeid).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.tidligereSamboerFortsattRegistrertPåAdresse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.uformeltGift).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.uformeltSeparertEllerSkilt).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.separasjonsbekreftelse).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.samlivsbrudd).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.avtaleOmDeltBosted).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.samværsavtale).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke)
            .isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.erklæringOmSamlivsbrudd).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.terminbekreftelse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.barnepassordningFaktura).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.avtaleBarnepasser).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.arbeidstid).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.roterendeArbeidstid).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.spesielleBehov).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.sykdom).isEqualTo(null)
        assertThat(dokumentasjon.barnsSykdom).isEqualTo(null)
        assertThat(dokumentasjon.manglendeBarnepass).isEqualTo(null)
        assertThat(dokumentasjon.barnMedSærligeBehov).isEqualTo(null)
        assertThat(dokumentasjon.arbeidskontrakt).isEqualTo(null)
        assertThat(dokumentasjon.lærlingkontrakt).isEqualTo(null)
        assertThat(dokumentasjon.utdanningstilbud).isEqualTo(null)
        assertThat(dokumentasjon.reduksjonAvArbeidsforhold).isEqualTo(null)
        assertThat(dokumentasjon.oppsigelse).isEqualTo(null)
        assertThat(dokumentasjon.utdanningsutgifter).isEqualTo(null)
        assertThat(dokumentasjon.meldtFlytteendring).isEqualTo(dokumentasjonMedVedlegg.tilDto())
    }

    @Test
    internal fun `skal mappe dokumentasjonsbehov for skolepenger`() {
        val søknadSkolepenger = SøknadsskjemaSkolepenger(
            type = SøknadType.SKOLEPENGER, fødselsnummer = "1", navn = "",
            datoMottatt = LocalDateTime.now(),
            sivilstand = sivilstand,
            medlemskap = medlemskap,
            bosituasjon = bosituasjon,
            sivilstandsplaner = Sivilstandsplaner(),
            barn = setOf(
                søknadBarn
            ),
            utdanning = utdanning,
            utdanningsutgifter = dokumentasjonSendtInnTidligere,
            tidligereUtdanninger = setOf(),
            opplysningerOmAdresse = opplysningerOmAdresse
        )
        val dokumentasjon = DokumentasjonMapper.tilDokumentasjonDto(søknadSkolepenger)

        assertThat(dokumentasjon.erIArbeid).isEqualTo(null)
        assertThat(dokumentasjon.virksomhet).isEqualTo(null)
        assertThat(dokumentasjon.ikkeVilligTilÅTaImotTilbudOmArbeid).isEqualTo(null)
        assertThat(dokumentasjon.tidligereSamboerFortsattRegistrertPåAdresse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.uformeltGift).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.uformeltSeparertEllerSkilt).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.separasjonsbekreftelse).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.samlivsbrudd).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.avtaleOmDeltBosted).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.samværsavtale).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke)
            .isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.erklæringOmSamlivsbrudd).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.terminbekreftelse).isEqualTo(dokumentasjonMedVedlegg.tilDto())
        assertThat(dokumentasjon.barnepassordningFaktura).isEqualTo(null)
        assertThat(dokumentasjon.avtaleBarnepasser).isEqualTo(null)
        assertThat(dokumentasjon.arbeidstid).isEqualTo(null)
        assertThat(dokumentasjon.roterendeArbeidstid).isEqualTo(null)
        assertThat(dokumentasjon.spesielleBehov).isEqualTo(null)
        assertThat(dokumentasjon.sykdom).isEqualTo(null)
        assertThat(dokumentasjon.barnsSykdom).isEqualTo(null)
        assertThat(dokumentasjon.manglendeBarnepass).isEqualTo(null)
        assertThat(dokumentasjon.barnMedSærligeBehov).isEqualTo(null)
        assertThat(dokumentasjon.arbeidskontrakt).isEqualTo(null)
        assertThat(dokumentasjon.lærlingkontrakt).isEqualTo(null)
        assertThat(dokumentasjon.utdanningstilbud).isEqualTo(null)
        assertThat(dokumentasjon.reduksjonAvArbeidsforhold).isEqualTo(null)
        assertThat(dokumentasjon.oppsigelse).isEqualTo(null)
        assertThat(dokumentasjon.utdanningsutgifter).isEqualTo(dokumentasjonSendtInnTidligere.tilDto())
        assertThat(dokumentasjon.meldtFlytteendring).isEqualTo(dokumentasjonMedVedlegg.tilDto())
    }
}
