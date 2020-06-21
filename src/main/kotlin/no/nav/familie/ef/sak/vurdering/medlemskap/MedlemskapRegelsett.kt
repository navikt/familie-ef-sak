package no.nav.familie.ef.sak.vurdering.medlemskap

import no.nav.familie.ef.sak.integration.dto.pdl.PdlPerson
import no.nav.familie.ef.sak.nare.evaluations.Evaluering
import no.nav.familie.ef.sak.nare.specifications.Regel
import no.nav.familie.ef.sak.vurdering.antallÅrOpphold
import no.nav.familie.ef.sak.vurdering.landkoderEøs
import no.nav.familie.ef.sak.vurdering.landkoderNorden
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period

@Component
class MedlemskapRegelsett {

    private val erMedlem =
            Regel<Medlemskapsgrunnlag>("Er søker registrert medlem?",
                                       "FP_VK 2.13",
                                       implementasjon = { erMedlem(this) })

    private val erBosattINorge =
            Regel<Medlemskapsgrunnlag>("Er søker registrert bosatt i norge",
                                       "EF-M3",
                                       implementasjon = { erBosattINorge(this) })

    private val oppholdINorge =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Oppholder søker seg i Norge?",
                                       identifikator = "EF-M4",
                                       implementasjon = { oppholdINorge(this) })

    private val utenlandsoppholdGrunnetNorskArbeidsgiver =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Skyldes utenlandsoppholdet arbeid for norsk arbeidsgiver?",
                                       identifikator = "EF-M4",
                                       implementasjon = { utenlandsoppholdGrunnetNorskArbeidsgiver(this) })

    private val medlemskapslengde =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Har søker vært medlem av folketrygden de siste " +
                                                     "$antallÅrOpphold år?",
                                       identifikator = "EF-M7",
                                       implementasjon = { medlemskapslengde(this) })

    private val medlemskapslengdeAvbruddMindreEnn10år =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Har søker etter fylte 16 år vært medlem i minst $antallÅrOpphold " +
                                                     "år og avbruddet mindre enn 10 år?",
                                       identifikator = "EF-M9",
                                       implementasjon = { medlemskapslengdeAvbruddMindreEnn10år(this) })

    private val medlemskapslengdeAvbruddMerEnn10år =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Har søker etter fylte 16 år vært medlem i minst 7 år " +
                                                     "ved avbrudd mer enn 10 år?",
                                       identifikator = "EF-M10",
                                       implementasjon = { medlemskapslengdeAvbruddMerEnn10år(this) })

    private val nordiskStatsborger =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Er søker norsk/nordisk statsborger?",
                                       identifikator = "EF-M8/17",
                                       implementasjon = { nordiskStatsborger(this) })


    private val eøsBorgerEllerLovligOpphold =
            Regel<Medlemskapsgrunnlag>(beskrivelse = "Har søker lovlig opphold i Norge eller " +
                                                     "er søker EØS-borger med oppholdsrett?",
                                       identifikator = "EF-M12/18",
                                       implementasjon = { eøsBorgerEllerLovligOpphold(this) })

    private val oppholdsrett = (nordiskStatsborger eller eøsBorgerEllerLovligOpphold)
            .med("FP_VK 2.12", "Har søker lovlig opphold i Norge eller er søker EØS-borger med oppholdsrett")

    private val forutgåendeMedlemskapSøker =
            (medlemskapslengde eller medlemskapslengdeAvbruddMindreEnn10år eller medlemskapslengdeAvbruddMerEnn10år)
                    .med("EF-M7 EF-M8 EF-M10", "Er vilkår for forutgående medlemskap oppfylt?")

    val vurderingMedlemskapSøker =
            (erMedlem
                    og erBosattINorge
                    og (oppholdINorge eller utenlandsoppholdGrunnetNorskArbeidsgiver)
                    og forutgåendeMedlemskapSøker
                    og oppholdsrett).med("Medlemskap kap. 15",
                                         "Har søker opphold i Norge og forutgående medlemskap?")

    private fun erMedlem(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        val statusNå =
                medlemskapsgrunnlag.medlemskapshistorikk.medlemskapsperioder.find { it.inneholder(LocalDate.now()) }
        if (statusNå == null || statusNå.gyldig == false) {
            return Evaluering.nei("Er IKKE MEDLEM")
        }
        if (statusNå.gyldig == null) {
            return Evaluering.kanskje("Uavklart medlemskapsunntak nå")
        }
        return Evaluering.ja("Søke er registrert som medlem nå.")
    }

    private fun erBosattINorge(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        return if ("bosatt" == medlemskapsgrunnlag.søker.folkeregisterpersonstatus.firstOrNull()?.status) {
            Evaluering.ja("Bosatt i Norge.")
        } else {
            Evaluering.nei("Ikke bosatt i Norge.")
        }
    }

    private fun oppholdINorge(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        return if (medlemskapsgrunnlag.søknad.medlemskapsdetaljer.verdi.oppholderDuDegINorge.verdi) {
            Evaluering.ja("Oppholder seg i Norge.")
        } else {
            Evaluering.nei("Oppholder seg ikke i Norge.")
        }
    }

    private fun utenlandsoppholdGrunnetNorskArbeidsgiver(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        // Hvis dette blir en del av søknaden kan vi gi bedre svar.
        return Evaluering.kanskje("Saksbehandler må ta stilling til om utenlandsopphold grunnes arbeid" +
                                  "for norsk arbeidsgiver.")
    }


    private fun medlemskapslengde(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        val søknadsdato = søknadsdato(medlemskapsgrunnlag)

        val statusSisteÅr = medlemskapsgrunnlag.medlemskapshistorikk.medlemskapsperioder.filter {
            it.tildato.isAfter(søknadsdato.minusYears(antallÅrOpphold))
        }

        if (statusSisteÅr.any { it.gyldig == false }) {
            return Evaluering.nei("Her hatt medlemskapsunntak siste $antallÅrOpphold år")
        }
        if (statusSisteÅr.any { it.gyldig == null }) {
            return Evaluering.kanskje("Her hatt uavklart medlemskapsunntak siste $antallÅrOpphold år")
        }

        return Evaluering.ja("Gyldig medlemskap siste $antallÅrOpphold år.")
    }

    private fun medlemskapslengdeAvbruddMindreEnn10år(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        val perioderSiden16årsdag = perioderMellom16årsdagOgSøknadsdato(medlemskapsgrunnlag)

        val gyldigePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == true }
        val ugyldigePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == false }

        if (ugyldigePerioderSiden16årsdag.any { it.lengde.years >= 10 }) {
            return Evaluering.nei("Lenger avbrudd enn 10 år ")
        }

        val uavklartePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == null }
        if (fusjonerKonsekutivePerioder(ugyldigePerioderSiden16årsdag, uavklartePerioderSiden16årsdag)
                        .any { it.lengde.years >= 10 }) {
            return Evaluering.kanskje("Uavklart avbrudd lenger enn 10 år ")
        }

        val sumGyldigePerioder = gyldigePerioderSiden16årsdag.fold(Period.ZERO) { acc, it -> acc.plus(it.lengde) }
        if (sumGyldigePerioder.years >= antallÅrOpphold) {
            return Evaluering.ja("Min $antallÅrOpphold års medlemskap etter fylte 16år.")
        }

        if (fusjonerKonsekutivePerioder(gyldigePerioderSiden16årsdag, uavklartePerioderSiden16årsdag)
                        .any { it.lengde.years >= antallÅrOpphold }) {
            return Evaluering.kanskje("Uavklarte avbrudd medfører mindre enn $antallÅrOpphold års medlemskap etter fylte 16 år ")
        }

        return Evaluering.nei("Mindre enn $antallÅrOpphold års medlemskap etter fylte 16 år ")
    }

    private fun medlemskapslengdeAvbruddMerEnn10år(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {

        val perioderSiden16årsdag = perioderMellom16årsdagOgSøknadsdato(medlemskapsgrunnlag)

        val gyldigePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == true }
        val uavklartePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == null }

        val sumGyldigePerioder = gyldigePerioderSiden16årsdag.fold(Period.ZERO) { acc, it -> acc.plus(it.lengde) }
        if (sumGyldigePerioder.years >= 7) {
            return Evaluering.ja("Min 7 års medlemskap etter fylte 16år.")
        }

        if (fusjonerKonsekutivePerioder(gyldigePerioderSiden16årsdag, uavklartePerioderSiden16årsdag)
                        .any { it.lengde.years >= 7 }) {
            return Evaluering.kanskje("Uavklarte avbrudd medfører mindre enn 7 års medlemskap etter fylte 16 år ")
        }

        return Evaluering.nei("Mindre enn 7 års medlemskap etter fylte 16 år ")
    }

    private fun perioderMellom16årsdagOgSøknadsdato(medlemskapsgrunnlag: Medlemskapsgrunnlag): List<Periode> {
        val søknadsdato = søknadsdato(medlemskapsgrunnlag)
        val datoFor16årsdag = datoFor16Årsdag(medlemskapsgrunnlag.søker)
        return medlemskapsgrunnlag.medlemskapshistorikk.medlemskapsperioder
                .filter { it.tildato.isAfter(datoFor16årsdag) }
                .map {
                    it.copy(fradato = if (it.fradato.isBefore(datoFor16årsdag)) datoFor16årsdag else it.fradato,
                            tildato = if (it.tildato.isAfter(søknadsdato)) søknadsdato else it.tildato)
                }
    }

    private fun fusjonerKonsekutivePerioder(vararg periodelister: List<Periode>): List<Periode> {

        val perioder = periodelister.asList().flatten().sortedBy { it.fradato }

        var periodeTilFusjonering: Periode? = null
        val fusjonertePerioder = ArrayList<Periode>()

        for (periode in perioder) {
            periodeTilFusjonering = when {
                periodeTilFusjonering == null -> { // Opprett ny periode
                    periode
                }
                periodeTilFusjonering.tildato == periode.fradato.minusDays(1) -> {
                    // konsekutive perioder utvid gjeldende periode
                    periodeTilFusjonering.copy(tildato = periode.tildato)
                }
                else -> { // Ikke konsekutive. Ferdig fusjonert periode legges til liste
                    fusjonertePerioder.add(periodeTilFusjonering)
                    periode
                }
            }
        }
        if (periodeTilFusjonering != null) {
            fusjonertePerioder.add(periodeTilFusjonering)
        }
        return fusjonertePerioder.toList()
    }

    private fun søknadsdato(medlemskapsgrunnlag: Medlemskapsgrunnlag): LocalDate {
        if (medlemskapsgrunnlag.søknad.stønadsstart.verdi.søkerFraBestemtMåned.verdi) {
            LocalDate.of(medlemskapsgrunnlag.søknad.stønadsstart.verdi.fraÅr!!.verdi,
                         medlemskapsgrunnlag.søknad.stønadsstart.verdi.fraMåned!!.verdi, 1)
        }
        return LocalDate.now()
    }

    private fun datoFor16Årsdag(person: PdlPerson?): LocalDate {
        val fødselsdato = person?.fødsel?.firstOrNull()?.fødselsdato ?: error("Person er ikke født")
        return fødselsdato.plusYears(16)
    }

    private fun nordiskStatsborger(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        if (medlemskapsgrunnlag.søker.statsborgerskap.any { landkoderNorden.contains(it.land.toUpperCase()) }) {
            return Evaluering.ja("Søker er nordisk statsborger")
        }
        return Evaluering.nei("Søker er ikke nordisk statsborger")
    }

    private fun eøsBorgerEllerLovligOpphold(medlemskapsgrunnlag: Medlemskapsgrunnlag): Evaluering {
        if (medlemskapsgrunnlag.søker.statsborgerskap.any { landkoderEøs.contains(it.land.toUpperCase()) }) {
            return Evaluering.kanskje("Søker er eøs-borger")
        }

        return Evaluering.kanskje("Søker er ikke eøs-borger")
    }

}
