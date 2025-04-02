package no.nav.familie.ef.sak.opplysninger.søknad.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Adresseopplysninger
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aksjeselskap
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aktivitet
import no.nav.familie.ef.sak.opplysninger.søknad.domain.AnnenForelder
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidsgiver
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssituasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssøker
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Barnepassordning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.BarnetilsynDokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Bosituasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Datoperiode
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokument
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.GjelderDeg
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Medlemskap
import no.nav.familie.ef.sak.opplysninger.søknad.domain.PersonMinimum
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Samvær
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Selvstendig
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Situasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstandsplaner
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadType
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.opplysninger.søknad.domain.TidligereUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.UnderUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Utenlandsopphold
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Virksomhet
import no.nav.familie.kontrakter.ef.søknad.Adresse
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Personalia
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import java.time.YearMonth
import kotlin.math.roundToInt
import no.nav.familie.kontrakter.ef.søknad.Adresseopplysninger as KontraktAdresseopplysninger
import no.nav.familie.kontrakter.ef.søknad.Aksjeselskap as KontraktAksjeselskap
import no.nav.familie.kontrakter.ef.søknad.Aktivitet as KontraktAktivitet
import no.nav.familie.kontrakter.ef.søknad.AnnenForelder as KontraktAnnenForelder
import no.nav.familie.kontrakter.ef.søknad.Arbeidsgiver as KontraktArbeidsgiver
import no.nav.familie.kontrakter.ef.søknad.Arbeidssøker as KontraktArbeidssøker
import no.nav.familie.kontrakter.ef.søknad.Barn as KontraktBarn
import no.nav.familie.kontrakter.ef.søknad.BarnepassOrdning as KontraktBarnepassOrdning
import no.nav.familie.kontrakter.ef.søknad.BarnetilsynDokumentasjon as KontraktBarnetilsynDokumentasjon
import no.nav.familie.kontrakter.ef.søknad.Bosituasjon as KontraktBosituasjon
import no.nav.familie.kontrakter.ef.søknad.Datoperiode as KontraktDatoperiode
import no.nav.familie.kontrakter.ef.søknad.Dokumentasjon as KontraktDokumentasjon
import no.nav.familie.kontrakter.ef.søknad.PersonMinimum as KontraktPersonMinimum
import no.nav.familie.kontrakter.ef.søknad.Samvær as KontraktSamvær
import no.nav.familie.kontrakter.ef.søknad.Selvstendig as KontraktSelvstendig
import no.nav.familie.kontrakter.ef.søknad.Situasjon as KontraktSituasjon
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsplaner as KontraktSivilstandsplaner
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn as KontraktSøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad as KontraktSøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger as KontraktSøknadSkolepenger
import no.nav.familie.kontrakter.ef.søknad.TidligereUtdanning as KontraktTidligereUtdanning
import no.nav.familie.kontrakter.ef.søknad.UnderUtdanning as KontraktUnderUtdanning
import no.nav.familie.kontrakter.ef.søknad.Utenlandsopphold as KontraktUtenlandsopphold
import no.nav.familie.kontrakter.ef.søknad.Virksomhet as KontraktVirksomhet

object SøknadsskjemaMapper {
    fun tilDomene(kontraktsøknad: KontraktSøknadOvergangsstønad): SøknadsskjemaOvergangsstønad =
        SøknadsskjemaOvergangsstønad(
            fødselsnummer = kontraktsøknad.personalia.verdi.fødselsnummer.verdi.verdi,
            navn = kontraktsøknad.personalia.verdi.navn.verdi,
            type = SøknadType.OVERGANGSSTØNAD,
            datoMottatt = kontraktsøknad.innsendingsdetaljer.verdi.datoMottatt.verdi,
            datoPåbegyntSøknad = kontraktsøknad.innsendingsdetaljer.verdi.datoPåbegyntSøknad,
            sivilstand = tilDomene(kontraktsøknad.sivilstandsdetaljer.verdi),
            medlemskap = tilDomene(kontraktsøknad.medlemskapsdetaljer.verdi),
            bosituasjon = tilDomene(kontraktsøknad.bosituasjon.verdi),
            sivilstandsplaner = tilDomene(kontraktsøknad.sivilstandsplaner?.verdi),
            barn = tilDomene(kontraktsøknad.barn.verdi),
            aktivitet = tilDomene(kontraktsøknad.aktivitet.verdi),
            situasjon = tilDomene(kontraktsøknad.situasjon.verdi),
            søkerFra = tilDomene(kontraktsøknad.stønadsstart.verdi),
            søkerFraBestemtMåned = kontraktsøknad.stønadsstart.verdi.søkerFraBestemtMåned.verdi,
            adresseopplysninger = tilDomene(kontraktsøknad.personalia, kontraktsøknad.adresseopplysninger),
        )

    fun tilDomene(kontraktsøknad: KontraktSøknadBarnetilsyn): SøknadsskjemaBarnetilsyn =
        SøknadsskjemaBarnetilsyn(
            fødselsnummer = kontraktsøknad.personalia.verdi.fødselsnummer.verdi.verdi,
            navn = kontraktsøknad.personalia.verdi.navn.verdi,
            type = SøknadType.BARNETILSYN,
            datoMottatt = kontraktsøknad.innsendingsdetaljer.verdi.datoMottatt.verdi,
            datoPåbegyntSøknad = kontraktsøknad.innsendingsdetaljer.verdi.datoPåbegyntSøknad,
            sivilstand = tilDomene(kontraktsøknad.sivilstandsdetaljer.verdi),
            medlemskap = tilDomene(kontraktsøknad.medlemskapsdetaljer.verdi),
            bosituasjon = tilDomene(kontraktsøknad.bosituasjon.verdi),
            sivilstandsplaner = tilDomene(kontraktsøknad.sivilstandsplaner?.verdi),
            barn = tilDomene(kontraktsøknad.barn.verdi),
            aktivitet = tilDomene(kontraktsøknad.aktivitet.verdi),
            søkerFra = tilDomene(kontraktsøknad.stønadsstart.verdi),
            søkerFraBestemtMåned = kontraktsøknad.stønadsstart.verdi.søkerFraBestemtMåned.verdi,
            dokumentasjon = tilDomene(kontraktsøknad.dokumentasjon),
            adresseopplysninger = tilDomene(kontraktsøknad.personalia, kontraktsøknad.adresseopplysninger),
        )

    fun tilDomene(kontraktsøknad: KontraktSøknadSkolepenger): SøknadsskjemaSkolepenger =
        SøknadsskjemaSkolepenger(
            fødselsnummer = kontraktsøknad.personalia.verdi.fødselsnummer.verdi.verdi,
            navn = kontraktsøknad.personalia.verdi.navn.verdi,
            type = SøknadType.SKOLEPENGER,
            datoMottatt = kontraktsøknad.innsendingsdetaljer.verdi.datoMottatt.verdi,
            datoPåbegyntSøknad = kontraktsøknad.innsendingsdetaljer.verdi.datoPåbegyntSøknad,
            medlemskap = tilDomene(kontraktsøknad.medlemskapsdetaljer.verdi),
            bosituasjon = tilDomene(kontraktsøknad.bosituasjon.verdi),
            sivilstand = tilDomene(kontraktsøknad.sivilstandsdetaljer.verdi),
            sivilstandsplaner = tilDomene(kontraktsøknad.sivilstandsplaner?.verdi),
            barn = tilDomene(kontraktsøknad.barn.verdi),
            utdanning = tilDomene(kontraktsøknad.utdanning.verdi)!!,
            utdanningsutgifter = tilDomene(kontraktsøknad.dokumentasjon.utdanningsutgifter?.verdi),
            tidligereUtdanninger =
                tilTidligereUtdanninger(
                    kontraktsøknad.utdanning.verdi.tidligereUtdanninger
                        ?.verdi,
                ),
            utdanningDokumentasjon = tilDomene(kontraktsøknad.dokumentasjon.utdanningDokumentasjon?.verdi),
            adresseopplysninger = tilDomene(kontraktsøknad.personalia, kontraktsøknad.adresseopplysninger),
        )

    private fun tilDomene(sivilstandsdetaljer: Sivilstandsdetaljer): Sivilstand =
        Sivilstand(
            erUformeltGift = sivilstandsdetaljer.erUformeltGift?.verdi,
            erUformeltGiftDokumentasjon = tilDomene(sivilstandsdetaljer.erUformeltGiftDokumentasjon?.verdi),
            erUformeltSeparertEllerSkilt = sivilstandsdetaljer.erUformeltSeparertEllerSkilt?.verdi,
            erUformeltSeparertEllerSkiltDokumentasjon =
                tilDomene(sivilstandsdetaljer.erUformeltSeparertEllerSkiltDokumentasjon?.verdi),
            søktOmSkilsmisseSeparasjon = sivilstandsdetaljer.søktOmSkilsmisseSeparasjon?.verdi,
            datoSøktSeparasjon = sivilstandsdetaljer.datoSøktSeparasjon?.verdi,
            separasjonsbekreftelse = tilDomene(sivilstandsdetaljer.separasjonsbekreftelse?.verdi),
            årsakEnslig = sivilstandsdetaljer.årsakEnslig?.svarId,
            samlivsbruddsdokumentasjon = tilDomene(sivilstandsdetaljer.samlivsbruddsdokumentasjon?.verdi),
            samlivsbruddsdato = sivilstandsdetaljer.samlivsbruddsdato?.verdi,
            fraflytningsdato = sivilstandsdetaljer.fraflytningsdato?.verdi,
            endringSamværsordningDato = sivilstandsdetaljer.endringSamværsordningDato?.verdi,
            tidligereSamboer = tilDomene(sivilstandsdetaljer.tidligereSamboerdetaljer?.verdi),
        )

    private fun tilDomene(dokumentasjon: KontraktBarnetilsynDokumentasjon): BarnetilsynDokumentasjon =
        BarnetilsynDokumentasjon(
            barnepassordningFaktura = tilDomene(dokumentasjon.barnepassordningFaktura?.verdi),
            avtaleBarnepasser = tilDomene(dokumentasjon.avtaleBarnepasser?.verdi),
            arbeidstid = tilDomene(dokumentasjon.arbeidstid?.verdi),
            roterendeArbeidstid = tilDomene(dokumentasjon.roterendeArbeidstid?.verdi),
            spesielleBehov = tilDomene(dokumentasjon.spesielleBehov?.verdi),
        )

    private fun tilDomene(personMinimum: KontraktPersonMinimum?): PersonMinimum? =
        personMinimum?.let {
            PersonMinimum(
                navn = it.navn.verdi,
                fødselsnummer = it.fødselsnummer?.verdi?.verdi,
                fødselsdato = it.fødselsdato?.verdi,
            )
        }

    private fun tilDomene(medlemskapsdetaljer: Medlemskapsdetaljer): Medlemskap =
        Medlemskap(
            oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge.verdi,
            bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene.verdi,
            oppholdsland = medlemskapsdetaljer.oppholdsland?.svarId,
            utenlandsopphold = tilUtenlandsopphold(medlemskapsdetaljer.utenlandsopphold?.verdi),
        )

    private fun tilUtenlandsopphold(list: List<KontraktUtenlandsopphold>?): Set<Utenlandsopphold> =
        list
            ?.map {
                Utenlandsopphold(
                    fradato = it.fradato.verdi,
                    tildato = it.tildato.verdi,
                    land = it.land?.svarId,
                    årsakUtenlandsopphold = it.årsakUtenlandsopphold.verdi,
                    personidentEøsLand = it.personidentEøsLand?.verdi,
                    adresseEøsLand = it.adresseEøsLand?.verdi,
                    erEøsland = it.erEøsLand,
                    kanIkkeOppgiPersonident = it.kanIkkeOppgiPersonident,
                )
            }?.toSet() ?: emptySet()

    private fun tilDomene(bosituasjon: KontraktBosituasjon): Bosituasjon =
        Bosituasjon(
            delerDuBolig = bosituasjon.delerDuBolig.svarId,
            samboer = tilDomene(bosituasjon.samboerdetaljer?.verdi),
            sammenflyttingsdato = bosituasjon.sammenflyttingsdato?.verdi,
            datoFlyttetFraHverandre = bosituasjon.datoFlyttetFraHverandre?.verdi,
            tidligereSamboerFortsattRegistrertPåAdresse =
                tilDomene(bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.verdi),
        )

    private fun tilDomene(sivilstandsplaner: KontraktSivilstandsplaner?): Sivilstandsplaner =
        Sivilstandsplaner(
            harPlaner = sivilstandsplaner?.harPlaner?.verdi,
            fraDato = sivilstandsplaner?.fraDato?.verdi,
            vordendeSamboerEktefelle = tilDomene(sivilstandsplaner?.vordendeSamboerEktefelle?.verdi),
        )

    private fun tilDomene(list: List<KontraktBarn>): Set<SøknadBarn> =
        list
            .map {
                SøknadBarn(
                    navn = it.navn?.verdi,
                    fødselsnummer = it.fødselsnummer?.verdi?.verdi,
                    harSkalHaSammeAdresse = it.harSkalHaSammeAdresse.verdi,
                    ikkeRegistrertPåSøkersAdresseBeskrivelse = it.ikkeRegistrertPåSøkersAdresseBeskrivelse?.verdi,
                    erBarnetFødt = it.erBarnetFødt.verdi,
                    fødselTermindato = it.fødselTermindato?.verdi,
                    terminbekreftelse = tilDomene(it.terminbekreftelse?.verdi),
                    annenForelder = tilDomene(it.annenForelder?.verdi),
                    samvær = tilDomene(it.samvær?.verdi),
                    skalHaBarnepass = it.skalHaBarnepass?.verdi,
                    særligeTilsynsbehov = it.særligeTilsynsbehov?.verdi,
                    årsakBarnepass =
                        it.barnepass
                            ?.verdi
                            ?.årsakBarnepass
                            ?.svarId,
                    barnepassordninger =
                        tilBarnepass(
                            it.barnepass
                                ?.verdi
                                ?.barnepassordninger
                                ?.verdi,
                        ),
                    skalBoHosSøker = it.skalBarnetBoHosSøker?.svarId,
                    lagtTilManuelt = it.lagtTilManuelt ?: false,
                )
            }.toSet()

    private fun tilBarnepass(list: List<KontraktBarnepassOrdning>?): Set<Barnepassordning> =
        list
            ?.map {
                Barnepassordning(
                    hvaSlagsBarnepassordning =
                        it.hvaSlagsBarnepassOrdning.svarId
                            ?: error("Mangler verdi for hvaSlagsbarnepassOrdning"),
                    navn = it.navn.verdi,
                    datoperiode =
                        it.datoperiode?.let { datoperiode -> tilDomene(datoperiode.verdi) }
                            ?: error("Mangler verdi for datoperiode i barnepassordningen"),
                    beløp = it.belop.verdi.roundToInt(),
                )
            }?.toSet() ?: emptySet()

    private fun tilDomene(datoperiode: KontraktDatoperiode?): Datoperiode? =
        datoperiode?.let {
            Datoperiode(it.fra, it.til)
        }

    private fun tilDomene(samvær: KontraktSamvær?): Samvær? =
        samvær?.let {
            Samvær(
                skalAnnenForelderHaSamvær = it.skalAnnenForelderHaSamvær?.svarId,
                harDereSkriftligAvtaleOmSamvær = it.harDereSkriftligAvtaleOmSamvær?.svarId,
                samværsavtale = tilDomene(it.samværsavtale?.verdi),
                skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke =
                    tilDomene(it.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke?.verdi),
                hvordanPraktiseresSamværet = it.hvordanPraktiseresSamværet?.verdi,
                borAnnenForelderISammeHus = it.borAnnenForelderISammeHus?.svarId,
                borAnnenForelderISammeHusBeskrivelse = it.borAnnenForelderISammeHusBeskrivelse?.verdi,
                harDereTidligereBoddSammen = it.harDereTidligereBoddSammen?.verdi,
                nårFlyttetDereFraHverandre = it.nårFlyttetDereFraHverandre?.verdi,
                erklæringOmSamlivsbrudd = tilDomene(it.erklæringOmSamlivsbrudd?.verdi),
                hvorMyeErDuSammenMedAnnenForelder = it.hvorMyeErDuSammenMedAnnenForelder?.svarId,
                beskrivSamværUtenBarn = it.beskrivSamværUtenBarn?.verdi,
            )
        }

    private fun tilDomene(annenForelder: KontraktAnnenForelder?): AnnenForelder? =
        annenForelder?.let {
            AnnenForelder(
                ikkeOppgittAnnenForelderBegrunnelse = annenForelder.ikkeOppgittAnnenForelderBegrunnelse?.verdi,
                bosattNorge = annenForelder.bosattNorge?.verdi,
                land = annenForelder.land?.verdi,
                person = tilDomene(annenForelder.person?.verdi),
            )
        }

    private fun tilDomene(aktivitet: KontraktAktivitet): Aktivitet =
        Aktivitet(
            hvordanErArbeidssituasjonen = Arbeidssituasjon(aktivitet.hvordanErArbeidssituasjonen.svarId ?: emptyList()),
            arbeidsforhold = tilArbeidsgivere(aktivitet.arbeidsforhold?.verdi),
            firmaer = tilFirmaer(aktivitet.firmaer?.verdi),
            virksomhet = tilDomene(aktivitet.virksomhet?.verdi),
            arbeidssøker = tilDomene(aktivitet.arbeidssøker?.verdi),
            underUtdanning = tilDomene(aktivitet.underUtdanning?.verdi),
            aksjeselskap = tilAksjeselskap(aktivitet.aksjeselskap?.verdi),
            erIArbeid = aktivitet.erIArbeid?.svarId,
            erIArbeidDokumentasjon = tilDomene(aktivitet.erIArbeidDokumentasjon?.verdi),
            tidligereUtdanninger =
                tilTidligereUtdanninger(
                    aktivitet.underUtdanning
                        ?.verdi
                        ?.tidligereUtdanninger
                        ?.verdi,
                ),
        )

    private fun tilFirmaer(list: List<KontraktSelvstendig>?): Set<Selvstendig> =
        list
            ?.map {
                Selvstendig(
                    firmanavn = it.firmanavn.verdi,
                    organisasjonsnummer = it.organisasjonsnummer.verdi,
                    etableringsdato = it.etableringsdato.verdi,
                    arbeidsmengde = it.arbeidsmengde?.verdi,
                    hvordanSerArbeidsukenUt = it.hvordanSerArbeidsukenUt.verdi,
                    overskudd = it.overskudd?.verdi,
                )
            }?.toSet() ?: emptySet()

    private fun tilAksjeselskap(list: List<KontraktAksjeselskap>?): Set<Aksjeselskap> =
        list
            ?.map {
                Aksjeselskap(
                    navn = it.navn.verdi,
                    arbeidsmengde = it.arbeidsmengde?.verdi,
                )
            }?.toSet() ?: emptySet()

    private fun tilDomene(underUtdanning: KontraktUnderUtdanning?): UnderUtdanning? =
        underUtdanning?.let {
            UnderUtdanning(
                skoleUtdanningssted = it.skoleUtdanningssted.verdi,
                linjeKursGrad =
                    it.gjeldendeUtdanning!!
                        .verdi.linjeKursGrad.verdi,
                fra =
                    it.gjeldendeUtdanning!!
                        .verdi.nårVarSkalDuVæreElevStudent.verdi.fra,
                til =
                    it.gjeldendeUtdanning!!
                        .verdi.nårVarSkalDuVæreElevStudent.verdi.til,
                offentligEllerPrivat = it.offentligEllerPrivat.svarId,
                heltidEllerDeltid = it.heltidEllerDeltid.svarId,
                hvorMyeSkalDuStudere = it.hvorMyeSkalDuStudere?.verdi,
                hvaErMåletMedUtdanningen = it.hvaErMåletMedUtdanningen?.verdi,
                utdanningEtterGrunnskolen = it.utdanningEtterGrunnskolen.verdi,
                semesteravgift = it.semesteravgift?.verdi?.roundToInt(),
                studieavgift = it.studieavgift?.verdi?.roundToInt(),
                eksamensgebyr = it.eksamensgebyr?.verdi?.roundToInt(),
            )
        }

    private fun tilTidligereUtdanninger(list: List<KontraktTidligereUtdanning>?): Set<TidligereUtdanning> =
        list
            ?.map {
                TidligereUtdanning(
                    linjeKursGrad = it.linjeKursGrad.verdi,
                    fra =
                        YearMonth.of(
                            it.nårVarSkalDuVæreElevStudent.verdi.fraÅr,
                            it.nårVarSkalDuVæreElevStudent.verdi.fraMåned,
                        ),
                    til =
                        YearMonth.of(
                            it.nårVarSkalDuVæreElevStudent.verdi.tilÅr,
                            it.nårVarSkalDuVæreElevStudent.verdi.tilMåned,
                        ),
                )
            }?.toSet() ?: emptySet()

    private fun tilDomene(arbeidssøker: KontraktArbeidssøker?): Arbeidssøker? =
        arbeidssøker?.let {
            Arbeidssøker(
                registrertSomArbeidssøkerNav = it.registrertSomArbeidssøkerNav.verdi,
                villigTilÅTaImotTilbudOmArbeid = it.villigTilÅTaImotTilbudOmArbeid.verdi,
                kanDuBegynneInnenEnUke = it.kanDuBegynneInnenEnUke.verdi,
                kanDuSkaffeBarnepassInnenEnUke = it.kanDuSkaffeBarnepassInnenEnUke?.verdi,
                hvorØnskerDuArbeid = it.hvorØnskerDuArbeid.verdi,
                ønskerDuMinst50ProsentStilling = it.ønskerDuMinst50ProsentStilling.verdi,
                ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon =
                    tilDomene(it.ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon?.verdi),
            )
        }

    private fun tilDomene(virksomhet: KontraktVirksomhet?): Virksomhet? =
        virksomhet?.let {
            Virksomhet(
                virksomhetsbeskrivelse = it.virksomhetsbeskrivelse.verdi,
                dokumentasjon = tilDomene(it.dokumentasjon?.verdi),
            )
        }

    private fun tilArbeidsgivere(list: List<KontraktArbeidsgiver>?): Set<Arbeidsgiver> =
        list
            ?.map {
                Arbeidsgiver(
                    arbeidsgivernavn = it.arbeidsgivernavn.verdi,
                    arbeidsmengde = it.arbeidsmengde?.verdi,
                    fastEllerMidlertidig = it.fastEllerMidlertidig.svarId,
                    harSluttdato = it.harSluttdato?.verdi,
                    sluttdato = it.sluttdato?.verdi,
                )
            }?.toSet() ?: emptySet()

    private fun tilDomene(situasjon: KontraktSituasjon): Situasjon =
        Situasjon(
            gjelderDetteDeg = GjelderDeg(situasjon.gjelderDetteDeg.svarId ?: emptyList()),
            sykdom = tilDomene(situasjon.sykdom?.verdi),
            barnsSykdom = tilDomene(situasjon.barnsSykdom?.verdi),
            manglendeBarnepass = tilDomene(situasjon.manglendeBarnepass?.verdi),
            barnMedSærligeBehov = tilDomene(situasjon.barnMedSærligeBehov?.verdi),
            arbeidskontrakt = tilDomene(situasjon.arbeidskontrakt?.verdi),
            lærlingkontrakt = tilDomene(situasjon.lærlingkontrakt?.verdi),
            oppstartNyJobb = situasjon.oppstartNyJobb?.verdi,
            utdanningstilbud = tilDomene(situasjon.utdanningstilbud?.verdi),
            oppstartUtdanning = situasjon.oppstartUtdanning?.verdi,
            sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling?.svarId,
            oppsigelseReduksjonÅrsak = situasjon.oppsigelseReduksjonÅrsak?.verdi,
            oppsigelseReduksjonTidspunkt = situasjon.oppsigelseReduksjonTidspunkt?.verdi,
            reduksjonAvArbeidsforholdDokumentasjon = tilDomene(situasjon.reduksjonAvArbeidsforholdDokumentasjon?.verdi),
            oppsigelseDokumentasjon = tilDomene(situasjon.oppsigelseDokumentasjon?.verdi),
        )

    private fun tilDomene(
        personalia: Søknadsfelt<Personalia>,
        adresseopplysninger: Søknadsfelt<KontraktAdresseopplysninger>?,
    ) = Adresseopplysninger(
        adresse = mapAdresse(personalia.verdi.adresse.verdi),
        søkerBorPåRegistrertAdresse = adresseopplysninger?.verdi?.søkerBorPåRegistrertAdresse?.verdi,
        harMeldtAdresseendring = adresseopplysninger?.verdi?.harMeldtAdresseendring?.verdi,
        dokumentasjonAdresseendring = tilDomene(adresseopplysninger?.verdi?.dokumentasjonAdresseendring?.verdi),
    )

    private fun mapAdresse(adresse: Adresse): String =
        adresse.let { adresse ->
            listOfNotNull(
                adresse.adresse,
                "${adresse.postnummer}${adresse.poststedsnavn?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""}",
                adresse.land,
            ).filter { it.isNotBlank() }
                .joinToString(", ")
        }

    private fun tilDomene(stønadsstart: Stønadsstart): YearMonth? = stønadsstart.fraMåned?.verdi?.let { måned -> stønadsstart.fraÅr?.verdi?.let { år -> YearMonth.of(år, måned) } }

    private fun tilDomene(dokumentasjon: KontraktDokumentasjon?): Dokumentasjon? =
        dokumentasjon?.let { dok ->
            Dokumentasjon(
                dok.harSendtInnTidligere.verdi,
                dok.dokumenter.map { Dokument(it.id, it.navn) },
            )
        }
}
