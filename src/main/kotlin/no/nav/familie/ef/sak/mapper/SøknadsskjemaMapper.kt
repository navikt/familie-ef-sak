package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.repository.domain.SøknadType
import no.nav.familie.ef.sak.repository.domain.søknad.*
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import org.apache.commons.lang3.StringUtils
import java.time.YearMonth
import kotlin.math.roundToInt
import no.nav.familie.kontrakter.ef.søknad.Aksjeselskap as KontraktAksjeselskap
import no.nav.familie.kontrakter.ef.søknad.Aktivitet as KontraktAktivitet
import no.nav.familie.kontrakter.ef.søknad.AnnenForelder as KontraktAnnenForelder
import no.nav.familie.kontrakter.ef.søknad.Arbeidsgiver as KontraktArbeidsgiver
import no.nav.familie.kontrakter.ef.søknad.Arbeidssøker as KontraktArbeidssøker
import no.nav.familie.kontrakter.ef.søknad.Barn as KontraktBarn
import no.nav.familie.kontrakter.ef.søknad.Barnepass as KontraktBarnepass
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

    fun tilDomene(kontraktsøknad: KontraktSøknadOvergangsstønad): SøknadsskjemaOvergangsstønad {
        return SøknadsskjemaOvergangsstønad(fødselsnummer = kontraktsøknad.personalia.verdi.fødselsnummer.verdi.verdi,
                                            navn = kontraktsøknad.personalia.verdi.navn.verdi,
                                            type = SøknadType.OVERGANGSSTØNAD,
                                            telefonnummer = kontraktsøknad.personalia.verdi.telefonnummer?.verdi,
                                            datoMottatt = kontraktsøknad.innsendingsdetaljer.verdi.datoMottatt.verdi,
                                            sivilstand = tilDomene(kontraktsøknad.sivilstandsdetaljer.verdi),
                                            medlemskap = tilDomene(kontraktsøknad.medlemskapsdetaljer.verdi),
                                            bosituasjon = tilDomene(kontraktsøknad.bosituasjon.verdi),
                                            sivilstandsplaner = tilDomene(kontraktsøknad.sivilstandsplaner?.verdi),
                                            barn = tilDomene(kontraktsøknad.barn.verdi),
                                            aktivitet = tilDomene(kontraktsøknad.aktivitet.verdi),
                                            situasjon = tilDomene(kontraktsøknad.situasjon.verdi),
                                            søkerFra = tilDomene(kontraktsøknad.stønadsstart.verdi),
                                            søkerFraBestemtMåned = kontraktsøknad.stønadsstart.verdi.søkerFraBestemtMåned.verdi)
    }

    fun tilDomene(kontraktsøknad: KontraktSøknadBarnetilsyn): SøknadsskjemaBarnetilsyn {
        return SøknadsskjemaBarnetilsyn(fødselsnummer = kontraktsøknad.personalia.verdi.fødselsnummer.verdi.verdi,
                                        navn = kontraktsøknad.personalia.verdi.navn.verdi,
                                        type = SøknadType.BARNETILSYN,
                                        telefonnummer = kontraktsøknad.personalia.verdi.telefonnummer?.verdi,
                                        datoMottatt = kontraktsøknad.innsendingsdetaljer.verdi.datoMottatt.verdi,
                                        sivilstand = tilDomene(kontraktsøknad.sivilstandsdetaljer.verdi),
                                        medlemskap = tilDomene(kontraktsøknad.medlemskapsdetaljer.verdi),
                                        bosituasjon = tilDomene(kontraktsøknad.bosituasjon.verdi),
                                        sivilstandsplaner = tilDomene(kontraktsøknad.sivilstandsplaner?.verdi),
                                        barn = tilDomene(kontraktsøknad.barn.verdi),
                                        aktivitet = tilDomene(kontraktsøknad.aktivitet.verdi),
                                        søkerFra = tilDomene(kontraktsøknad.stønadsstart.verdi),
                                        søkerFraBestemtMåned = kontraktsøknad.stønadsstart.verdi.søkerFraBestemtMåned.verdi,
                                        dokumentasjon = tilDomene(kontraktsøknad.dokumentasjon))
    }

    fun tilDomene(kontraktsøknad: KontraktSøknadSkolepenger): SøknadsskjemaSkolepenger {
        return SøknadsskjemaSkolepenger(fødselsnummer = kontraktsøknad.personalia.verdi.fødselsnummer.verdi.verdi,
                                        navn = kontraktsøknad.personalia.verdi.navn.verdi,
                                        type = SøknadType.SKOLEPENGER,
                                        telefonnummer = kontraktsøknad.personalia.verdi.telefonnummer?.verdi,
                                        datoMottatt = kontraktsøknad.innsendingsdetaljer.verdi.datoMottatt.verdi,
                                        medlemskap = tilDomene(kontraktsøknad.medlemskapsdetaljer.verdi),
                                        bosituasjon = tilDomene(kontraktsøknad.bosituasjon.verdi),
                                        sivilstandsplaner = tilDomene(kontraktsøknad.sivilstandsplaner?.verdi),
                                        barn = tilDomene(kontraktsøknad.barn.verdi),
                                        utdanning = tilDomene(kontraktsøknad.utdanning.verdi)!!,
                                        utdanningsutgifter = tilDomene(kontraktsøknad.dokumentasjon.utdanningsutgifter?.verdi))
    }

    private fun tilDomene(sivilstandsdetaljer: Sivilstandsdetaljer): Sivilstand {
        return Sivilstand(erUformeltGift = sivilstandsdetaljer.erUformeltGift?.verdi,
                          erUformeltGiftDokumentasjon = tilDomene(sivilstandsdetaljer.erUformeltGiftDokumentasjon?.verdi),
                          erUformeltSeparertEllerSkilt = sivilstandsdetaljer.erUformeltSeparertEllerSkilt?.verdi,
                          erUformeltSeparertEllerSkiltDokumentasjon =
                          tilDomene(sivilstandsdetaljer.erUformeltSeparertEllerSkiltDokumentasjon?.verdi),
                          søktOmSkilsmisseSeparasjon = sivilstandsdetaljer.søktOmSkilsmisseSeparasjon?.verdi,
                          datoSøktSeparasjon = sivilstandsdetaljer.datoSøktSeparasjon?.verdi,
                          separasjonsbekreftelse = tilDomene(sivilstandsdetaljer.separasjonsbekreftelse?.verdi),
                          årsakEnslig = sivilstandsdetaljer.årsakEnslig?.let {
                              EnumTekstverdiMedSvarId(it.verdi, it.svarId ?: "manglerSvarid")
                          },
                          samlivsbruddsdokumentasjon = tilDomene(sivilstandsdetaljer.samlivsbruddsdokumentasjon?.verdi),
                          samlivsbruddsdato = sivilstandsdetaljer.samlivsbruddsdato?.verdi,
                          fraflytningsdato = sivilstandsdetaljer.fraflytningsdato?.verdi,
                          endringSamværsordningDato = sivilstandsdetaljer.endringSamværsordningDato?.verdi,
                          tidligereSamboer = tilDomene(sivilstandsdetaljer.tidligereSamboerdetaljer?.verdi))


    }


    private fun tilDomene(dokumentasjon: KontraktBarnetilsynDokumentasjon): BarnetilsynDokumentasjon =
            BarnetilsynDokumentasjon(barnepassordningFaktura = tilDomene(dokumentasjon.barnepassordningFaktura?.verdi),
                                     avtaleBarnepasser = tilDomene(dokumentasjon.avtaleBarnepasser?.verdi),
                                     arbeidstid = tilDomene(dokumentasjon.arbeidstid?.verdi),
                                     roterendeArbeidstid = tilDomene(dokumentasjon.roterendeArbeidstid?.verdi),
                                     spesielleBehov = tilDomene(dokumentasjon.spesielleBehov?.verdi))

    private fun tilDomene(personMinimum: KontraktPersonMinimum?): PersonMinimum? =
            personMinimum?.let {
                PersonMinimum(navn = it.navn.verdi,
                              fødselsnummer = it.fødselsnummer?.verdi?.verdi,
                              fødselsdato = it.fødselsdato?.verdi,
                              land = it.land?.verdi)
            }

    private fun tilDomene(medlemskapsdetaljer: Medlemskapsdetaljer): Medlemskap =
            Medlemskap(oppholderDuDegINorge = medlemskapsdetaljer.oppholderDuDegINorge.verdi,
                       bosattNorgeSisteÅrene = medlemskapsdetaljer.bosattNorgeSisteÅrene.verdi,
                       utenlandsopphold = tilUtenlandsopphold(medlemskapsdetaljer.utenlandsopphold?.verdi))

    private fun tilUtenlandsopphold(list: List<KontraktUtenlandsopphold>?): Set<Utenlandsopphold> =
            list?.map {
                Utenlandsopphold(fradato = it.fradato.verdi,
                                 tildato = it.tildato.verdi,
                                 årsakUtenlandsopphold = it.årsakUtenlandsopphold.verdi)
            }?.toSet() ?: emptySet()

    private fun tilDomene(bosituasjon: KontraktBosituasjon): Bosituasjon =
            Bosituasjon(delerDuBolig = EnumTekstverdiMedSvarId(bosituasjon.delerDuBolig.verdi,
                                                               bosituasjon.delerDuBolig.svarId ?: "manglerSvarid"),
                        samboer = tilDomene(bosituasjon.samboerdetaljer?.verdi),
                        sammenflyttingsdato = bosituasjon.sammenflyttingsdato?.verdi,
                        datoFlyttetFraHverandre = bosituasjon.datoFlyttetFraHverandre?.verdi,
                        tidligereSamboerFortsattRegistrertPåAdresse =
                        tilDomene(bosituasjon.tidligereSamboerFortsattRegistrertPåAdresse?.verdi))

    private fun tilDomene(sivilstandsplaner: KontraktSivilstandsplaner?): Sivilstandsplaner =
            Sivilstandsplaner(harPlaner = sivilstandsplaner?.harPlaner?.verdi,
                              fraDato = sivilstandsplaner?.fraDato?.verdi,
                              vordendeSamboerEktefelle = tilDomene(sivilstandsplaner?.vordendeSamboerEktefelle?.verdi))

    private fun tilDomene(list: List<KontraktBarn>): Set<Barn> =
            list.map {
                Barn(navn = it.navn?.verdi,
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
                     barnepass = tilDomene(it.barnepass?.verdi))
            }.toSet()

    private fun tilDomene(barnepass: KontraktBarnepass?): Barnepass? =
            barnepass?.let {
                Barnepass(årsakBarnepass = it.årsakBarnepass?.verdi,
                          barnepassordninger = tilBarnepass(it.barnepassordninger.verdi))
            }

    private fun tilBarnepass(list: List<KontraktBarnepassOrdning>): Set<Barnepassordning> = list.map {
        Barnepassordning(hvaSlagsBarnepassordning = it.hvaSlagsBarnepassOrdning.verdi,
                         navn = it.navn.verdi,
                         datoperiode = tilDomene(it.datoperiode?.verdi),
                         beløp = it.belop.verdi.roundToInt())
    }.toSet()

    private fun tilDomene(datoperiode: KontraktDatoperiode?): Datoperiode? {

        return datoperiode?.let {
            Datoperiode(it.fra, it.til)
        }
    }

    private fun tilDomene(samvær: KontraktSamvær?): Samvær? =
            samvær?.let {
                Samvær(spørsmålAvtaleOmDeltBosted = it.spørsmålAvtaleOmDeltBosted?.verdi,
                       avtaleOmDeltBosted = tilDomene(it.avtaleOmDeltBosted?.verdi),
                       skalAnnenForelderHaSamvær = it.skalAnnenForelderHaSamvær?.verdi,
                       harDereSkriftligAvtaleOmSamvær = it.harDereSkriftligAvtaleOmSamvær?.verdi,
                       samværsavtale = tilDomene(it.samværsavtale?.verdi),
                       skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke =
                       tilDomene(it.skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke?.verdi),
                       hvordanPraktiseresSamværet = it.hvordanPraktiseresSamværet?.verdi,
                       borAnnenForelderISammeHus = it.borAnnenForelderISammeHus?.verdi,
                       borAnnenForelderISammeHusBeskrivelse = it.borAnnenForelderISammeHusBeskrivelse?.verdi,
                       harDereTidligereBoddSammen = it.harDereTidligereBoddSammen?.verdi,
                       nårFlyttetDereFraHverandre = it.nårFlyttetDereFraHverandre?.verdi,
                       erklæringOmSamlivsbrudd = tilDomene(it.erklæringOmSamlivsbrudd?.verdi),
                       hvorMyeErDuSammenMedAnnenForelder = it.hvorMyeErDuSammenMedAnnenForelder?.verdi,
                       beskrivSamværUtenBarn = it.beskrivSamværUtenBarn?.verdi)
            }

    private fun tilDomene(annenForelder: KontraktAnnenForelder?): AnnenForelder? =
            annenForelder?.let {
                AnnenForelder(ikkeOppgittAnnenForelderBegrunnelse = annenForelder.ikkeOppgittAnnenForelderBegrunnelse?.verdi,
                              bosattNorge = annenForelder.bosattNorge?.verdi,
                              land = annenForelder.land?.verdi,
                              person = tilDomene(annenForelder.person?.verdi))
            }

    private fun tilDomene(aktivitet: KontraktAktivitet): Aktivitet =
            Aktivitet(hvordanErArbeidssituasjonen = aktivitet.hvordanErArbeidssituasjonen.let {
                EnumTekstverdiMedSvarId(StringUtils.join(it.verdi, ";"),
                                        StringUtils.join(it.svarId, ";"))
            },
                      arbeidsforhold = tilArbeidsgivere(aktivitet.arbeidsforhold?.verdi),
                      firmaer = tilFirmaer(aktivitet.firmaer?.verdi),
                      virksomhet = tilDomene(aktivitet.virksomhet?.verdi),
                      arbeidssøker = tilDomene(aktivitet.arbeidssøker?.verdi),
                      underUtdanning = tilDomene(aktivitet.underUtdanning?.verdi),
                      aksjeselskap = tilAksjeselskap(aktivitet.aksjeselskap?.verdi),
                      erIArbeid = aktivitet.erIArbeid?.verdi,
                      erIArbeidDokumentasjon = tilDomene(aktivitet.erIArbeidDokumentasjon?.verdi),
                      tidligereUtdanninger = tilTidligereUtdanninger(aktivitet.underUtdanning?.verdi?.tidligereUtdanninger?.verdi))

    private fun tilFirmaer(list: List<KontraktSelvstendig>?): Set<Selvstendig>? =
            list?.map {
                Selvstendig(firmanavn = it.firmanavn.verdi,
                            organisasjonsnummer = it.organisasjonsnummer.verdi,
                            etableringsdato = it.etableringsdato.verdi,
                            arbeidsmengde = it.arbeidsmengde?.verdi,
                            hvordanSerArbeidsukenUt = it.hvordanSerArbeidsukenUt.verdi)
            }?.toSet() ?: emptySet()

    private fun tilAksjeselskap(list: List<KontraktAksjeselskap>?): Set<Aksjeselskap>? = list?.map {
        Aksjeselskap(navn = it.navn.verdi,
                     arbeidsmengde = it.arbeidsmengde?.verdi)
    }?.toSet() ?: emptySet()

    private fun tilDomene(underUtdanning: KontraktUnderUtdanning?): UnderUtdanning? =
            underUtdanning?.let {
                UnderUtdanning(skoleUtdanningssted = it.skoleUtdanningssted.verdi,
                               linjeKursGrad = it.gjeldendeUtdanning!!.verdi.linjeKursGrad.verdi,
                               fra = it.gjeldendeUtdanning!!.verdi.nårVarSkalDuVæreElevStudent.verdi.fra,
                               til = it.gjeldendeUtdanning!!.verdi.nårVarSkalDuVæreElevStudent.verdi.til,
                               offentligEllerPrivat = it.offentligEllerPrivat.verdi,
                               heltidEllerDeltid = it.heltidEllerDeltid.verdi,
                               hvorMyeSkalDuStudere = it.hvorMyeSkalDuStudere?.verdi,
                               hvaErMåletMedUtdanningen = it.hvaErMåletMedUtdanningen?.verdi,
                               utdanningEtterGrunnskolen = it.utdanningEtterGrunnskolen.verdi,
                               semesteravgift = it.semesteravgift?.verdi?.roundToInt(),
                               studieavgift = it.studieavgift?.verdi?.roundToInt(),
                               eksamensgebyr = it.eksamensgebyr?.verdi?.roundToInt())
            }

    private fun tilTidligereUtdanninger(list: List<KontraktTidligereUtdanning>?): Set<TidligereUtdanning> =
            list?.map {
                TidligereUtdanning(linjeKursGrad = it.linjeKursGrad.verdi,
                                   fra = YearMonth.of(it.nårVarSkalDuVæreElevStudent.verdi.fraÅr,
                                                      it.nårVarSkalDuVæreElevStudent.verdi.fraMåned),
                                   til = YearMonth.of(it.nårVarSkalDuVæreElevStudent.verdi.tilÅr,
                                                      it.nårVarSkalDuVæreElevStudent.verdi.tilMåned))
            }?.toSet() ?: emptySet()

    private fun tilDomene(arbeidssøker: KontraktArbeidssøker?): Arbeidssøker? =
            arbeidssøker?.let {
                Arbeidssøker(registrertSomArbeidssøkerNav = it.registrertSomArbeidssøkerNav.verdi,
                             villigTilÅTaImotTilbudOmArbeid = it.villigTilÅTaImotTilbudOmArbeid.verdi,
                             kanDuBegynneInnenEnUke = it.kanDuBegynneInnenEnUke.verdi,
                             kanDuSkaffeBarnepassInnenEnUke = it.kanDuSkaffeBarnepassInnenEnUke?.verdi,
                             hvorØnskerDuArbeid = it.hvorØnskerDuArbeid.verdi,
                             ønskerDuMinst50ProsentStilling = it.ønskerDuMinst50ProsentStilling.verdi,
                             ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon =
                             tilDomene(it.ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon?.verdi))
            }

    private fun tilDomene(virksomhet: KontraktVirksomhet?): Virksomhet? =
            virksomhet?.let {
                Virksomhet(virksomhetsbeskrivelse = it.virksomhetsbeskrivelse.verdi,
                           dokumentasjon = tilDomene(it.dokumentasjon?.verdi))
            }

    private fun tilArbeidsgivere(list: List<KontraktArbeidsgiver>?): Set<Arbeidsgiver>? =
            list?.map {
                Arbeidsgiver(arbeidsgivernavn = it.arbeidsgivernavn.verdi,
                             arbeidsmengde = it.arbeidsmengde?.verdi,
                             fastEllerMidlertidig = it.fastEllerMidlertidig.verdi,
                             harSluttdato = it.harSluttdato?.verdi,
                             sluttdato = it.sluttdato?.verdi)
            }?.toSet() ?: emptySet()

    private fun tilDomene(situasjon: KontraktSituasjon): Situasjon =
            Situasjon(gjelderDetteDeg = EnumTekstverdiMedSvarId(StringUtils.join(situasjon.gjelderDetteDeg.verdi,
                                                                                 ";"), // TODO: whitespace etter ;?
                                                                StringUtils.join(situasjon.gjelderDetteDeg.svarId, ";")),
                      sykdom = tilDomene(situasjon.sykdom?.verdi),
                      barnsSykdom = tilDomene(situasjon.barnsSykdom?.verdi),
                      manglendeBarnepass = tilDomene(situasjon.manglendeBarnepass?.verdi),
                      barnMedSærligeBehov = tilDomene(situasjon.barnMedSærligeBehov?.verdi),
                      arbeidskontrakt = tilDomene(situasjon.arbeidskontrakt?.verdi),
                      lærlingkontrakt = tilDomene(situasjon.lærlingkontrakt?.verdi),
                      oppstartNyJobb = situasjon.oppstartNyJobb?.verdi,
                      utdanningstilbud = tilDomene(situasjon.utdanningstilbud?.verdi),
                      oppstartUtdanning = situasjon.oppstartUtdanning?.verdi,
                      sagtOppEllerRedusertStilling = situasjon.sagtOppEllerRedusertStilling?.let {
                          EnumTekstverdiMedSvarId(it.verdi,
                                                  it.svarId
                                                  ?: "manglerSvarId")
                      },
                      oppsigelseReduksjonÅrsak = situasjon.oppsigelseReduksjonÅrsak?.verdi,
                      oppsigelseReduksjonTidspunkt = situasjon.oppsigelseReduksjonTidspunkt?.verdi,
                      reduksjonAvArbeidsforholdDokumentasjon = tilDomene(situasjon.reduksjonAvArbeidsforholdDokumentasjon?.verdi),
                      oppsigelseDokumentasjon = tilDomene(situasjon.oppsigelseDokumentasjon?.verdi))

    private fun tilDomene(stønadsstart: Stønadsstart): YearMonth? =
            stønadsstart.fraMåned?.verdi?.let { måned -> stønadsstart.fraÅr?.verdi?.let { år -> YearMonth.of(år, måned) } }


    private fun tilDomene(dokumentasjon: KontraktDokumentasjon?): Dokumentasjon? =
            dokumentasjon?.let { dok ->
                Dokumentasjon(dok.harSendtInnTidligere.verdi,
                              dok.dokumenter.map { Dokument(it.id, it.navn) })
            }
}