package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.gui.dto.GuiSak
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.Period

@Component
class Medlemskapsvurdering(val medlemskapsinfo: Medlemskapsinfo,
                           val guiSak: GuiSak) {

    private val landkoderNorden = listOf("NO", "SE", "DK", "IS", "FI")
    private val landkoderEøs = listOf("BE",
                                      "BG",
                                      "DK",
                                      "EE",
                                      "FI",
                                      "FR",
                                      "GR",
                                      "IE",
                                      "IS",
                                      "IT",
                                      "HR",
                                      "CY",
                                      "LV",
                                      "LI",
                                      "LT",
                                      "LU",
                                      "MT",
                                      "NL",
                                      "NO",
                                      "PL",
                                      "PT",
                                      "RO",
                                      "SK",
                                      "SL",
                                      "ES",
                                      "GB", // tom 2020-12-31
                                      "CH", // (Sveits er ikke EØS -land, men omfattes av reglene for koordinering av trygd)
                                      "SE",
                                      "CZ",
                                      "DE",
                                      "HU",
                                      "AT")

    private val erMedlem =
            SpørsmålNode<GuiSak>("Er søker registrert som IKKE MEDLEM?",
                                 "FP_VK 2.13",
                                 gjørEvaluering = { erIkkeMedlem(this) },
                                 hvisJa = { Evaluering(Resultat.NEI, "Søker er registrert som ikke medlem.") },
                                 hvisNei = { erBosattINorge.evaluer(this) })

    fun erIkkeMedlem(guiSak: GuiSak): Evaluering {
        val gyldigUnntakNå = medlemskapsinfo.gyldigePerioder.find { LocalDate.now().isBefore(it.tom) }
        if (gyldigUnntakNå != null) {
            return Evaluering(Resultat.JA, "Er IKKE MEDLEM")
        }
        val uavklartUnntakNå = medlemskapsinfo.uavklartePerioder.find { LocalDate.now().isBefore(it.tom) }
        if (uavklartUnntakNå != null) {
            return Evaluering(Resultat.KANSKJE, "Uavklart medlemskapsunntak nå")
        }
        return Evaluering.nei("Ikke registrert med medlemskapsunntak nå.")
    }

    private val erBosattINorge = SpørsmålNode<GuiSak>("Er søker registrert bosatt i norge",
                                                      "EF-M3",
                                                      gjørEvaluering = { erBosattINorge(this) },
                                                      hvisJa = { oppholdINorge.evaluer(this) },
                                                      hvisNei = { Evaluering.nei("Medlemskap ikke oppfylt") })

    fun erBosattINorge(guiSak: GuiSak): Evaluering {
        return if ("bosatt" == guiSak.søker.person?.folkeregisterpersonstatus?.firstOrNull()?.status) {
            Evaluering(Resultat.JA, "Bosatt i Norge.")
        } else {
            Evaluering(Resultat.NEI, "Ikke bosatt i Norge.")
        }
    }

    private val oppholdINorge =
            SpørsmålNode<GuiSak>("Oppholder søker seg i Norge?",
                                 "EF-M4",
                                 { oppholdINorge(this) },
                                 hvisJa = { medlemskapslengde.evaluer(this) },
                                 hvisNei = { medlemskapslengde.evaluer(this) })

    private fun oppholdINorge(guiSak: GuiSak): Evaluering {
        return if (guiSak.søknad.medlemskapsdetaljer.verdi.oppholderDuDegINorge.verdi) {
            Evaluering(Resultat.JA, "Oppholder seg i Norge.")
        } else {
            Evaluering(Resultat.NEI, "Oppholder seg ikke i Norge.")
        }
    }

    private val medlemskapslengde =
            SpørsmålNode<GuiSak>("Har søker vært medlem av folketrygden de siste 3 år?",
                                 "EF-M7",
                                 { oppholdINorge(this) },
                                 hvisJa = { nordiskStatsborger.evaluer(this) },
                                 hvisNei = { medlemskapslengdeAvbruddMindreEnn10år.evaluer(this) })

    private fun medlemskapslengde(guiSak: GuiSak): Evaluering {
        guiSak.søker
                .person
                ?.bostedsadresse
                ?.filter {
                    it.angittFlyttedato != null && it.angittFlyttedato.isBefore(LocalDate.now().minusYears(3))
                }?.maxBy { it.angittFlyttedato!! }
        ?: return Evaluering.nei("Ikke hatt bostedsadresse i Norge siste 3 år")
        val gyldigeUnntakSiste3år = medlemskapsinfo.gyldigePerioder.filter { it.tom.isAfter(LocalDate.now().minusYears(3)) }
        if (gyldigeUnntakSiste3år.isNotEmpty()) {
            return Evaluering.nei("Her hatt opphold i medlemskap siste 3 år")
        }
        val uavklarteUnntakSiste3år = medlemskapsinfo.uavklartePerioder.filter { it.tom.isAfter(LocalDate.now().minusYears(3)) }
        if (uavklarteUnntakSiste3år.isNotEmpty()) {
            return Evaluering.kanskje("Her hatt uavklart opphold i medlemskap siste 3 år")
        }

        return Evaluering.ja("Gyldig medlemskap siste 3 år.")
    }

    private val medlemskapslengdeAvbruddMindreEnn10år =
            SpørsmålNode<GuiSak>("Har søker etter fylte 16 år vært medlem i minst 3 år og avbruddet mindre enn 10 år?",
                                 "EF-M9",
                                 { medlemskapslengdeAvbruddMindreEnn10år(this) },
                                 hvisJa = { nordiskStatsborger.evaluer(this) },
                                 hvisNei = { medlemskapslengdeAvbruddMerEnn10år.evaluer(this) })

    private fun medlemskapslengdeAvbruddMindreEnn10år(guiSak: GuiSak): Evaluering {



        val fødselsdato = guiSak.søker.person?.foedsel?.firstOrNull()?.foedselsdato ?: error("Person er ikke født")
        val datoFor16årsdag = fødselsdato.plusYears(16)
        val tidSiden16årsdag = Period.between(datoFor16årsdag, LocalDate.now()) // TODO Skal være søknadsdato ikke d.d.


        val gyldigeUnntakSiden16årsdag =
                medlemskapsinfo.gyldigePerioder
                        .filter { it.tom.isAfter(datoFor16årsdag) }
                        .map {
                            Period.between(if (it.fom.isBefore(datoFor16årsdag)) datoFor16årsdag else it.fom, it.tom)
                        }
        if (gyldigeUnntakSiden16årsdag.find { it.years > 9 } != null) {
            return Evaluering.nei("Lenger avbrudd enn 10 år ")
        }

        val sumGyldigeUnntak = gyldigeUnntakSiden16årsdag.fold(Period.ZERO) { acc, period -> acc.plus(period) }
        if (tidSiden16årsdag.minus(sumGyldigeUnntak).years < 3) {
            return Evaluering.nei("Mindre enn 3 års medlemskap etter fylte 16 år ")
        }


        val uavklarteUnntakSiden16årsdag =
                medlemskapsinfo.uavklartePerioder
                        .filter { it.tom.isAfter(datoFor16årsdag) }
                        .map {
                            Period.between(if (it.fom.isBefore(datoFor16årsdag)) datoFor16årsdag else it.fom, it.tom)
                        }
        if (uavklarteUnntakSiden16årsdag.find { it.years > 9 } != null) {
            return Evaluering.kanskje("Uavklart avbrudd lenger enn 10 år ")
        }
        val sumUavklarteUnntak = uavklarteUnntakSiden16årsdag.fold(Period.ZERO) { acc, period -> acc.plus(period) }
        if (tidSiden16årsdag.minus(sumUavklarteUnntak).years < 3) {
            return Evaluering.kanskje("Uavklarte avbrudd medfører mindre enn 3 års medlemskap etter fylte 16 år ")
        }

        return Evaluering.ja("Min 3 års medlemskap etter fylte 16år.")
    }

    private val medlemskapslengdeAvbruddMerEnn10år =
            SpørsmålNode<GuiSak>("Har søker etter fylte 16 år vært medlem i minst 7 år ved avbrud mer enn 10 år?",
                                 "EF-M10",
                                 { medlemskapslengdeAvbruddMerEnn10år(this) },
                                 hvisJa = { nordiskStatsborger.evaluer(this) },
                                 hvisNei = { .evaluer(this) })

    private fun medlemskapslengdeAvbruddMerEnn10år(guiSak: GuiSak): Evaluering {
        val fødselsdato = guiSak.søker.person?.foedsel?.firstOrNull()?.foedselsdato ?: error("Person er ikke født")
        val datoFor16årsdag = fødselsdato.plusYears(16)
        val tidSiden16årsdag = Period.between(datoFor16årsdag, LocalDate.now()) // TODO Skal være søknadsdato ikke d.d.

        val gyldigeUnntakSiden16årsdag =
                medlemskapsinfo.gyldigePerioder
                        .filter { it.tom.isAfter(datoFor16årsdag) }
                        .map {
                            Period.between(if (it.fom.isBefore(datoFor16årsdag)) datoFor16årsdag else it.fom, it.tom)
                        }

        val sumGyldigeUnntak = gyldigeUnntakSiden16årsdag.fold(Period.ZERO) { acc, period -> acc.plus(period) }
        if (tidSiden16årsdag.minus(sumGyldigeUnntak).years < 7) {
            return Evaluering.nei("Mindre enn 7 års medlemskap etter fylte 16 år ")
        }


        val uavklarteUnntakSiden16årsdag =
                medlemskapsinfo.uavklartePerioder
                        .filter { it.tom.isAfter(datoFor16årsdag) }
                        .map {
                            Period.between(if (it.fom.isBefore(datoFor16årsdag)) datoFor16årsdag else it.fom, it.tom)
                        }
        val sumUavklarteUnntak = uavklarteUnntakSiden16årsdag.fold(Period.ZERO) { acc, period -> acc.plus(period) }
        if (tidSiden16årsdag.minus(sumUavklarteUnntak).years < 7) {
            return Evaluering.kanskje("Uavklarte avbrudd medfører mindre enn 7 års medlemskap etter fylte 16 år ")
        }

        return Evaluering.ja("Min 7 års medlemskap etter fylte 16år.")
    }

    private val nordiskStatsborger =
            SpørsmålNode<GuiSak>("Er søker nordisk statsborger?",
                                 "EF-M8",
                                 { nordiskStatsborger(this) },
                                 hvisJa = { Evaluering.ja("Opphold og forutgående medlemskap oppfylt") },
                                 hvisNei = { eøsBorgerEllerLovligOpphold.evaluer(this) })

    fun nordiskStatsborger(guiSak: GuiSak): Evaluering {
        if (guiSak.søker.person?.statsborgerskap?.find { landkoderNorden.contains(it.land.toUpperCase()) } != null) {
            return Evaluering.ja("Søker er nordisk statsborger")
        }
        return Evaluering.nei("Søker er ikke nordisk statsborger")
    }

    private val eøsBorgerEllerLovligOpphold =
            SpørsmålNode<GuiSak>("Er søker nordisk statsborger?",
                                 "EF-M8",
                                 { eøsBorgerEllerLovligOpphold(this) },
                                 hvisJa = { Evaluering.ja("Opphold og forutgående medlemskap oppfylt") },
                                 hvisNei = { Evaluering.nei("Medlemsvilkår ikke oppfylt") })

    fun eøsBorgerEllerLovligOpphold(guiSak: GuiSak): Evaluering {
        if (guiSak.søker.person?.statsborgerskap?.find { landkoderEøs.contains(it.land.toUpperCase()) } != null) {
            return Evaluering.ja("Søker er eøs-borger")
        }

        return Evaluering.nei("Søker er ikke nordisk statsborger")
    }


}