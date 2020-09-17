package no.nav.familie.ef.sak.vurdering.medlemskap

import io.mockk.mockk
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.nare.evaluations.Resultat
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.pdlSøker
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.søknad
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

internal class MedlemskapRegelsettTest {

    @Test
    fun `vurderingMedlemskapSøker returnerer Nei hvis søker er registrert som ikke medlem`() {
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val pdlSøker = pdlSøker(fødsel = listOf(Fødsel(null, LocalDate.of(1999, 4, 5), null, null, null)),
                                bostedsadresse = emptyList(),
                                folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosatt", "bo")),
                                statsborgerskap = listOf(Statsborgerskap("SE", LocalDate.of(2002, 2, 2), null)))
        val søknad = søknad(medlemskapsdetaljer = Søknadsfelt("", Medlemskapsdetaljer(Søknadsfelt("", true), mockk(), mockk())),
                            stønadsstart = Søknadsfelt("", Stønadsstart(Søknadsfelt("", Month.AUGUST),
                                                                        Søknadsfelt("", 2014),
                                                                        Søknadsfelt("", true))))
        val medlemskapshistorikk = Medlemskapshistorikk(pdlSøker, medlemskapsinfoUtenUnntak)
        val medlemskapsgrunnlag = Medlemskapsgrunnlag(pdlSøker, medlemskapshistorikk, søknad)

        val evaluering = MedlemskapRegelsett().vurderingMedlemskapSøker.evaluer(medlemskapsgrunnlag)

        assertThat(evaluering.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.find { it.beskrivelse == "Er søker registrert medlem?" }?.resultat).isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.find { it.beskrivelse == "Er søker registrert bosatt i norge" }?.resultat)
                .isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find {
            it.beskrivelse == "Oppholder søker seg i Norge? ELLER Skyldes utenlandsoppholdet arbeid for norsk arbeidsgiver?"
        }?.resultat).isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find { it.beskrivelse == "Er vilkår for forutgående medlemskap oppfylt?" }?.resultat)
                .isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.find {
            it.beskrivelse == "Har søker lovlig opphold i Norge eller er søker EØS-borger med oppholdsrett"
        }?.resultat).isEqualTo(Resultat.JA)
    }

    @Test
    fun `vurderingMedlemskapSøker returnerer Ja hvis søker er medlem og oppfyller kravet til forutgående medlemskap`() {
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val pdlSøker = pdlSøker(fødsel = listOf(Fødsel(null, LocalDate.of(1999, 4, 5), null, null, null)),
                                folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosatt", "bo")),
                                bostedsadresse = listOf(Bostedsadresse(null,
                                                                       null,
                                                                       Folkeregistermetadata(LocalDateTime.of(1999, 4, 5, 0, 0),
                                                                                             null),
                                                                       null,
                                                                       null,
                                                                       null)),
                                statsborgerskap = listOf(Statsborgerskap("SE", LocalDate.of(2002, 2, 2), null)))
        val søknad = søknad(medlemskapsdetaljer = Søknadsfelt("",
                                                              Medlemskapsdetaljer(Søknadsfelt("oppholdINorge", true),
                                                                                  mockk(),
                                                                                  mockk())),
                            stønadsstart = Søknadsfelt("", Stønadsstart(Søknadsfelt("", Month.AUGUST),
                                                                        Søknadsfelt("", 2014),
                                                                        Søknadsfelt("", true))))
        val medlemskapshistorikk = Medlemskapshistorikk(pdlSøker, medlemskapsinfoUtenUnntak)
        val medlemskapsgrunnlag = Medlemskapsgrunnlag(pdlSøker, medlemskapshistorikk, søknad)

        val evaluering = MedlemskapRegelsett().vurderingMedlemskapSøker.evaluer(medlemskapsgrunnlag)

        assertThat(evaluering.resultat).isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find { it.beskrivelse == "Er søker registrert medlem?" }?.resultat).isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find { it.beskrivelse == "Er søker registrert bosatt i norge" }?.resultat)
                .isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find {
            it.beskrivelse == "Oppholder søker seg i Norge? ELLER Skyldes utenlandsoppholdet arbeid for norsk arbeidsgiver?"
        }?.resultat).isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find { it.beskrivelse == "Er vilkår for forutgående medlemskap oppfylt?" }?.resultat)
                .isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find {
            it.beskrivelse == "Har søker lovlig opphold i Norge eller er søker EØS-borger med oppholdsrett"
        }?.resultat).isEqualTo(Resultat.JA)
    }

    @Test
    fun `vurderingMedlemskapSøker returnerer Kanskje hvis søker ikke oppholder seg i Norge`() {
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val pdlSøker = pdlSøker(fødsel = listOf(Fødsel(null, LocalDate.of(1999, 4, 5), null, null, null)),
                                folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("bosatt", "bo")),
                                bostedsadresse = listOf(Bostedsadresse(null,
                                                                       null,
                                                                       Folkeregistermetadata(LocalDateTime.of(1999, 4, 5, 0, 0),
                                                                                             null),
                                                                       null,
                                                                       null,
                                                                       null)),
                                statsborgerskap = listOf(Statsborgerskap("SE", LocalDate.of(2002, 2, 2), null)))
        val søknad = søknad(medlemskapsdetaljer = Søknadsfelt("",
                                                              Medlemskapsdetaljer(Søknadsfelt("oppholdINorge", false),
                                                                                  mockk(),
                                                                                  mockk())),
                            stønadsstart = Søknadsfelt("", Stønadsstart(Søknadsfelt("", Month.AUGUST),
                                                                        Søknadsfelt("", 2014),
                                                                        Søknadsfelt("", true))))
        val medlemskapshistorikk = Medlemskapshistorikk(pdlSøker, medlemskapsinfoUtenUnntak)
        val medlemskapsgrunnlag = Medlemskapsgrunnlag(pdlSøker, medlemskapshistorikk, søknad)

        val evaluering = MedlemskapRegelsett().vurderingMedlemskapSøker.evaluer(medlemskapsgrunnlag)

        assertThat(evaluering.resultat).isEqualTo(Resultat.KANSKJE)
        assertThat(evaluering.children.find { it.beskrivelse == "Er søker registrert medlem?" }?.resultat).isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find { it.beskrivelse == "Er søker registrert bosatt i norge" }?.resultat)
                .isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find { it.beskrivelse == "Oppholder søker seg i Norge?" }?.resultat)
                .isEqualTo(Resultat.NEI)
        assertThat(evaluering.children.find {
            it.beskrivelse == "Skyldes utenlandsoppholdet arbeid for norsk arbeidsgiver?"
        }?.resultat).isEqualTo(Resultat.KANSKJE)
        assertThat(evaluering.children.find { it.beskrivelse == "Er vilkår for forutgående medlemskap oppfylt?" }?.resultat)
                .isEqualTo(Resultat.JA)
        assertThat(evaluering.children.find {
            it.beskrivelse == "Har søker lovlig opphold i Norge eller er søker EØS-borger med oppholdsrett"
        }?.resultat).isEqualTo(Resultat.JA)
    }

}
