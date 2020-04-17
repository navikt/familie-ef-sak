package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.gui.dto.GuiSak
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPerson
import no.nav.nare.core.evaluations.Evaluering
import no.nav.nare.core.evaluations.Resultat
import java.time.LocalDate
import java.time.Period
import no.nav.nare.core.specifications.Spesifikasjon as SpørsmålNode


class MedlemskapRegelsett(private val medlemskapshistorikkSøker: Medlemskapshistorikk,
                          private val medlemskapshistorikkAnnenForelder: Medlemskapshistorikk) {

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

    private val erIkkeMedlem =
            SpørsmålNode<GuiSak>("Er søker registrert som IKKE MEDLEM?",
                                 "FP_VK 2.13",
                                 implementasjon = { erIkkeMedlem(this) })

    private fun erIkkeMedlem(guiSak: GuiSak): Evaluering {
        val statusNå =
                medlemskapshistorikkSøker.medlemskapsperioder.find { it.inneholder(LocalDate.now()) }
        if (statusNå == null || statusNå.gyldig == false) {
            return Evaluering(Resultat.JA, "Er IKKE MEDLEM")
        }
        if (statusNå.gyldig == null) {
            return Evaluering(Resultat.KANSKJE, "Uavklart medlemskapsunntak nå")
        }
        return Evaluering.nei("Ikke registrert med medlemskapsunntak nå.")
    }

    private val erBosattINorge = SpørsmålNode<GuiSak>("Er søker registrert bosatt i norge",
                                                      "EF-M3",
                                                      implementasjon = { erBosattINorge(this) })

    fun erBosattINorge(guiSak: GuiSak): Evaluering {
        return if ("bosatt" == guiSak.søker.person?.folkeregisterpersonstatus?.firstOrNull()?.status) {
            Evaluering(Resultat.JA, "Bosatt i Norge.")
        } else {
            Evaluering(Resultat.NEI, "Ikke bosatt i Norge.")
        }
    }

    private val oppholdINorge =
            SpørsmålNode<GuiSak>(beskrivelse = "Oppholder søker seg i Norge?",
                                 identifikator = "EF-M4",
                                 implementasjon = { oppholdINorge(this) })

    private fun oppholdINorge(guiSak: GuiSak): Evaluering {
        return if (guiSak.søknad.medlemskapsdetaljer.verdi.oppholderDuDegINorge.verdi) {
            Evaluering(Resultat.JA, "Oppholder seg i Norge.")
        } else {
            Evaluering(Resultat.NEI, "Oppholder seg ikke i Norge.")
        }
    }

    private val utenlandsoppholdGrunnetNorskArbeidsgiver =
            SpørsmålNode<GuiSak>(beskrivelse = "Oppholder søker seg i Norge?",
                                 identifikator = "EF-M4",
                                 implementasjon = { utenlandsoppholdGrunnetNorskArbeidsgiver(this) })

    private fun utenlandsoppholdGrunnetNorskArbeidsgiver(guiSak: GuiSak): Evaluering {
        return Evaluering(Resultat.KANSKJE, "Saksbehandler må ta stilling til dette.")
    }


    private val medlemskapslengde =
            SpørsmålNode<GuiSak>(beskrivelse = "Har søker vært medlem av folketrygden de siste 3 år?",
                                 identifikator = "EF-M7",
                                 implementasjon = { medlemskapslengde(this, medlemskapshistorikkSøker) })

    private val medlemskapslengdeAvbruddMindreEnn10år =
            SpørsmålNode<GuiSak>(beskrivelse = "Har søker etter fylte 16 år vært medlem i minst 3 år og avbruddet mindre enn 10 år?",
                                 identifikator = "EF-M9",
                                 implementasjon = {
                                     medlemskapslengdeAvbruddMindreEnn10år(this,
                                                                           this.søker.person,
                                                                           medlemskapshistorikkSøker)
                                 })

    private val medlemskapslengdeAvbruddMerEnn10år =
            SpørsmålNode<GuiSak>(beskrivelse = "Har søker etter fylte 16 år vært medlem i minst 7 år ved avbrudd mer enn 10 år?",
                                 identifikator = "EF-M10",
                                 implementasjon = {
                                     medlemskapslengdeAvbruddMerEnn10år(this,
                                                                        this.søker.person,
                                                                        medlemskapshistorikkSøker)
                                 })

    private val medlemskapslengdeAnnenForelder =
            SpørsmålNode<GuiSak>(beskrivelse = "Har den andre forelderen vært medlem av folketrygden de siste 3 år?",
                                 identifikator = "EF-M13",
                                 implementasjon = { medlemskapslengde(this, medlemskapshistorikkAnnenForelder) })

    private val medlemskapslengdeAvbruddMindreEnn10årAnnenForelder =
            SpørsmålNode<GuiSak>(beskrivelse = "Har den andre forelderen etter fylte 16 år vært medlem i minst 3 år " +
                                               "og avbruddet mindre enn 10 år?",
                                 identifikator = "EF-M14",
                                 implementasjon = {
                                     medlemskapslengdeAvbruddMindreEnn10år(this,
                                                                           this.annenForelder.person,
                                                                           medlemskapshistorikkAnnenForelder)
                                 })

    private val medlemskapslengdeAvbruddMerEnn10årAnnenForelder =
            SpørsmålNode<GuiSak>(beskrivelse = "Har den andre forelderen etter fylte 16 år vært medlem i minst 7 år " +
                                               "ved avbrudd mer enn 10 år?",
                                 identifikator = "EF-M15",
                                 implementasjon = {
                                     medlemskapslengdeAvbruddMerEnn10år(this,
                                                                        this.annenForelder.person,
                                                                        medlemskapshistorikkAnnenForelder)
                                 })

    private fun medlemskapslengde(guiSak: GuiSak, medlemskapshistorikk: Medlemskapshistorikk): Evaluering {
        val søknadsdato = søknadsdato(guiSak)

        val statusSiste3År = medlemskapshistorikkSøker.medlemskapsperioder.filter {
            it.tildato.isAfter(søknadsdato.minusYears(3))
        }

        if (statusSiste3År.find { it.gyldig == false } != null) {
            return Evaluering.nei("Her hatt medlemskapsunntak siste 3 år")
        }
        if (statusSiste3År.find { it.gyldig == null } != null) {
            return Evaluering.kanskje("Her hatt uavklart medlemskapsunntak siste 3 år")
        }

        return Evaluering.ja("Gyldig medlemskap siste 3 år.")
    }

    private fun medlemskapslengdeAvbruddMindreEnn10år(guiSak: GuiSak,
                                                      person: PdlPerson?,
                                                      medlemskapshistorikk: Medlemskapshistorikk): Evaluering {
        val perioderSiden16årsdag = perioderMellom16årsdagOgSøknadsdato(guiSak, person, medlemskapshistorikk)

        val gyldigePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == true }
        val ugyldigePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == false }

        if (ugyldigePerioderSiden16årsdag.find { it.lengde.years >= 10 } != null) {
            return Evaluering.nei("Lenger avbrudd enn 10 år ")
        }

        val uavklartePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == null }
        if (fusjonerKonsekutivePerioder(ugyldigePerioderSiden16årsdag, uavklartePerioderSiden16årsdag)
                        .find { it.lengde.years >= 10 } != null) {
            return Evaluering.kanskje("Uavklart avbrudd lenger enn 10 år ")
        }

        val sumGyldigePerioder = gyldigePerioderSiden16årsdag.fold(Period.ZERO) { acc, it -> acc.plus(it.lengde) }
        if (sumGyldigePerioder.years >= 3) {
            return Evaluering.ja("Min 3 års medlemskap etter fylte 16år.")
        }

        if (fusjonerKonsekutivePerioder(gyldigePerioderSiden16årsdag, uavklartePerioderSiden16årsdag)
                        .find { it.lengde.years >= 3 } != null) {
            return Evaluering.kanskje("Uavklarte avbrudd medfører mindre enn 3 års medlemskap etter fylte 16 år ")
        }

        return Evaluering.nei("Mindre enn 3 års medlemskap etter fylte 16 år ")
    }

    private fun medlemskapslengdeAvbruddMerEnn10år(guiSak: GuiSak,
                                                   person: PdlPerson?,
                                                   medlemskapshistorikk: Medlemskapshistorikk): Evaluering {

        val perioderSiden16årsdag = perioderMellom16årsdagOgSøknadsdato(guiSak, person, medlemskapshistorikk)

        val gyldigePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == true }
        val uavklartePerioderSiden16årsdag = perioderSiden16årsdag.filter { it.gyldig == null }

        val sumGyldigePerioder = gyldigePerioderSiden16årsdag.fold(Period.ZERO) { acc, it -> acc.plus(it.lengde) }
        if (sumGyldigePerioder.years >= 7) {
            return Evaluering.ja("Min 7 års medlemskap etter fylte 16år.")
        }

        if (fusjonerKonsekutivePerioder(gyldigePerioderSiden16årsdag, uavklartePerioderSiden16årsdag)
                        .find { it.lengde.years >= 7 } != null) {
            return Evaluering.kanskje("Uavklarte avbrudd medfører mindre enn 7 års medlemskap etter fylte 16 år ")
        }

        return Evaluering.nei("Mindre enn 7 års medlemskap etter fylte 16 år ")
    }


    private fun perioderMellom16årsdagOgSøknadsdato(guiSak: GuiSak,
                                                    pdlPerson: PdlPerson?,
                                                    medlemskapshistorikk: Medlemskapshistorikk): List<Periode> {
        val søknadsdato = søknadsdato(guiSak)
        val datoFor16årsdag = datoFor16Årsdag(pdlPerson)
        return medlemskapshistorikk.medlemskapsperioder
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

    private fun søknadsdato(guiSak: GuiSak) =
            LocalDate.of(guiSak.søknad.stønadsstart.verdi.fraÅr.verdi, guiSak.søknad.stønadsstart.verdi.fraMåned.verdi, 1)

    private fun datoFor16Årsdag(person: PdlPerson?): LocalDate {
        val fødselsdato = person?.foedsel?.firstOrNull()?.foedselsdato ?: error("Person er ikke født")
        return fødselsdato.plusYears(16)
    }

    private val nordiskStatsborger =
            SpørsmålNode<GuiSak>(beskrivelse = "Er søker norsk/nordisk statsborger?",
                                 identifikator = "EF-M8/17",
                                 implementasjon = { nordiskStatsborger(this) })

    fun nordiskStatsborger(guiSak: GuiSak): Evaluering {
        if (guiSak.søker.person?.statsborgerskap?.find { landkoderNorden.contains(it.land.toUpperCase()) } != null) {
            return Evaluering.ja("Søker er nordisk statsborger")
        }
        return Evaluering.nei("Søker er ikke nordisk statsborger")
    }

    private val eøsBorgerEllerLovligOpphold =
            SpørsmålNode<GuiSak>(beskrivelse = "Har søker lovlig opphold i Norge eller er søker EØS-borger med oppholdsrett?",
                                 identifikator = "EF-M12/18",
                                 implementasjon = { eøsBorgerEllerLovligOpphold(this) })

    fun eøsBorgerEllerLovligOpphold(guiSak: GuiSak): Evaluering {
        if (guiSak.søker.person?.statsborgerskap?.find { landkoderEøs.contains(it.land.toUpperCase()) } != null) {
            return Evaluering.kanskje("Søker er eøs-borger")
        }

        return Evaluering.kanskje("Søker er ikke eøs-borger")
    }


    private val oppholdsrett = (nordiskStatsborger eller eøsBorgerEllerLovligOpphold)
            .med("FP_VK 2.12", "Har søker lovlig opphold i Norge eller er søker EØS-borger med oppholdsrett")


    private val forutgåendeMedlemskapSøker =
            (medlemskapslengde eller medlemskapslengdeAvbruddMindreEnn10år eller medlemskapslengdeAvbruddMerEnn10år)
                    .med("EF-M7 EF-M8 EF-M10", "Er vilkår for forutgående medlemskap oppfylt?")

    private val komSøkerPåGrunnAvGjenforening =
            SpørsmålNode<GuiSak>(beskrivelse = "Kom søker til landet pga gjenforening med ektefelle/samboer med felles barn, " +
                                               "eller for å gifte seg med en som er bosatt i Norge, " +
                                               "og hadde gyldig oppholdstillatelse ved ankomst?",
                                 identifikator = "EF-M11",
                                 implementasjon = { komSøkerPåGrunnAvGjenforening(this) })

    private val forutgåendeMedlemskapAnnenForelderEllerGjenforening =
            (medlemskapslengdeAnnenForelder
                    eller medlemskapslengdeAvbruddMindreEnn10årAnnenForelder
                    eller medlemskapslengdeAvbruddMerEnn10årAnnenForelder
                    eller komSøkerPåGrunnAvGjenforening)
                    .med("EF-M7 EF-M8 EF-M10", "Er vilkår for forutgående medlemskap oppfylt?")

    fun komSøkerPåGrunnAvGjenforening(guiSak: GuiSak): Evaluering {
        return Evaluering.kanskje("Dette må saksbehandler vurdere.")
    }

    private val oppstodStønadstilfelletINorge =
            SpørsmålNode<GuiSak>(beskrivelse = "Oppstod stønadstilfellet i Norge?",
                                 identifikator = "EF-M16",
                                 implementasjon = { oppstodStønadstilfelletINorge(this) })

    fun oppstodStønadstilfelletINorge(guiSak: GuiSak): Evaluering {
        return Evaluering.kanskje("Dette må saksbehandler vurdere.")
    }

    private val fravikFraMedlemskapskravVedTotalvurdering =
            SpørsmålNode<GuiSak>(beskrivelse = "Oppstod stønadstilfellet i Norge?",
                                 identifikator = "EF-M16",
                                 implementasjon = { fravikFraMedlemskapskravVedTotalvurdering(this) })

    fun fravikFraMedlemskapskravVedTotalvurdering(guiSak: GuiSak): Evaluering {
        return Evaluering.kanskje("Dette må saksbehandler vurdere.")
    }


    private val totalvurderingMedlemskap =
            (erIkkeMedlem.ikke()
                    og erBosattINorge
                    og (oppholdINorge eller utenlandsoppholdGrunnetNorskArbeidsgiver)
                    og (forutgåendeMedlemskapSøker
                    eller (forutgåendeMedlemskapAnnenForelderEllerGjenforening og oppstodStønadstilfelletINorge)
                    eller fravikFraMedlemskapskravVedTotalvurdering)
                    og oppholdsrett).med("Medlemskap kap. 15",
                                         "Har søker opphold i Norge og forutgående medlemskap?")

}