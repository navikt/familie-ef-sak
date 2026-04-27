package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.kontrakter.ef.søknad.Adresse
import no.nav.familie.kontrakter.ef.søknad.Selvstendig
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønadRegelendring2026
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.ef.søknad.Utenlandsopphold
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SøknadsskjemaMapperTest {
    @Test
    internal fun `skal mappe søknad som mangler datoer for stønadsstart`() {
        val stønadsstart =
            Søknadsfelt(
                "Stønadsstart",
                Stønadsstart(
                    Søknadsfelt(
                        "Søker du stønad fra et bestemt tidspunkt",
                        false,
                    ),
                    null,
                    null,
                ),
            )
        val kontraktsøknad = Testsøknad.søknadOvergangsstønad.copy(stønadsstart = stønadsstart)
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(kontraktsøknad)
        assertThat(søknadTilLagring.søkerFraBestemtMåned).isEqualTo(false)
        assertThat(søknadTilLagring.søkerFra).isNull()
        assertThat(søknadTilLagring.datoPåbegyntSøknad).isNull()
    }

    @Test
    internal fun `skal mappe samboer fødselsdato`() {
        val nyPerson = TestsøknadBuilder.Builder().defaultPersonMinimum(fødselsdato = LocalDate.now())
        val søknad =
            TestsøknadBuilder
                .Builder()
                .setBosituasjon(samboerdetaljer = nyPerson)
                .build()
                .søknadOvergangsstønad

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
        assertThat(søknadTilLagring.bosituasjon.samboer!!.fødselsdato!!).isEqualTo(LocalDate.now())
    }

    @Test
    internal fun `skal mappe feltet skalBoHosSøker`() {
        val svarSkalBarnetBoHosSøker = "jaMenSamarbeiderIkke"
        val barn =
            TestsøknadBuilder
                .Builder()
                .defaultBarn()
                .copy(
                    skalBarnetBoHosSøker = Søknadsfelt("", "", null, svarSkalBarnetBoHosSøker),
                )
        val søknad =
            TestsøknadBuilder
                .Builder()
                .build()
                .søknadOvergangsstønad
                .copy(barn = Søknadsfelt("", listOf(barn)))

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
        assertThat(søknadTilLagring.barn.first().skalBoHosSøker).isEqualTo(svarSkalBarnetBoHosSøker)
    }

    @Nested
    inner class AdresseTilAdresseopplysninger {
        @Test
        internal fun `skal mappe adress fra personalia til opplysninger om adresse då det er den som vises til brukeren`() {
            val adresse =
                Adresse(
                    adresse = "adresse",
                    postnummer = "1234",
                    poststedsnavn = "Sted",
                    land = null,
                )
            val søknad =
                TestsøknadBuilder
                    .Builder()
                    .setPersonalia(adresse = adresse)
                    .build()
                    .søknadOvergangsstønad
            assertThat(SøknadsskjemaMapper.tilDomene(søknad).adresseopplysninger?.adresse).isEqualTo("adresse, 1234 Sted")
        }

        @Test
        internal fun `poststed skal ikke vises hvis det ikke er med`() {
            val adresse =
                Adresse(
                    adresse = "adresse",
                    postnummer = "1234",
                    poststedsnavn = null,
                    land = "Land",
                )
            val søknad =
                TestsøknadBuilder
                    .Builder()
                    .setPersonalia(adresse = adresse)
                    .build()
                    .søknadOvergangsstønad
            assertThat(SøknadsskjemaMapper.tilDomene(søknad).adresseopplysninger?.adresse).isEqualTo("adresse, 1234, Land")
        }

        @Test
        internal fun `tomme element skal håndteres`() {
            val adresse =
                Adresse(
                    adresse = "adresse",
                    postnummer = "",
                    poststedsnavn = null,
                    land = "",
                )
            val søknad =
                TestsøknadBuilder
                    .Builder()
                    .setPersonalia(adresse = adresse)
                    .build()
                    .søknadOvergangsstønad
            assertThat(SøknadsskjemaMapper.tilDomene(søknad).adresseopplysninger?.adresse).isEqualTo("adresse")
        }
    }

    @Nested
    inner class LandIMedlemskap {
        @Test
        internal fun `skal ikke inneholde oppholdsland om ikke sendt med`() {
            val søknad = TestsøknadBuilder.Builder().build().søknadOvergangsstønad

            val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
            assertThat(søknadTilLagring.medlemskap.oppholdsland).isNull()
        }

        @Test
        internal fun `skal inneholde oppholdsland om innsendt`() {
            val oppholdsland =
                Søknadsfelt(
                    label = "I hvilket land oppholder du deg?",
                    verdi = "Polen",
                    svarId = "POL",
                )
            val søknad =
                TestsøknadBuilder
                    .Builder()
                    .setMedlemskapsdetaljer(oppholdsland = oppholdsland)
                    .build()
                    .søknadOvergangsstønad

            val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)
            assertThat(søknadTilLagring.medlemskap.oppholdsland).isEqualTo("POL")
        }

        @Test
        internal fun `utenlandsperiode skal inneholde land om innsendt`() {
            val utenlandsperioder =
                listOf(
                    Utenlandsopphold(
                        fradato = Søknadsfelt("Fra", LocalDate.of(2021, 1, 1)),
                        tildato = Søknadsfelt("Til", LocalDate.of(2022, 1, 1)),
                        land = Søknadsfelt("I hvilket land oppholdt du deg i?", svarId = "ESP", verdi = "Spania"),
                        årsakUtenlandsopphold = Søknadsfelt("Årsak til utenlandsopphold", "Ferie"),
                    ),
                )

            val søknad =
                TestsøknadBuilder
                    .Builder()
                    .setMedlemskapsdetaljer(utenlandsopphold = utenlandsperioder)
                    .build()
                    .søknadOvergangsstønad
            val søknadTilLagring = SøknadsskjemaMapper.tilDomene(søknad)

            assertThat(
                søknadTilLagring.medlemskap.utenlandsopphold
                    .first()
                    .fradato,
            ).isEqualTo(LocalDate.of(2021, 1, 1))
            assertThat(
                søknadTilLagring.medlemskap.utenlandsopphold
                    .first()
                    .tildato,
            ).isEqualTo(LocalDate.of(2022, 1, 1))
            assertThat(
                søknadTilLagring.medlemskap.utenlandsopphold
                    .first()
                    .land,
            ).isEqualTo("ESP")
            assertThat(
                søknadTilLagring.medlemskap.utenlandsopphold
                    .first()
                    .årsakUtenlandsopphold,
            ).isEqualTo("Ferie")
        }
    }

    @Nested
    inner class RegelendringMapping {
        @Test
        internal fun `skal mappe SøknadOvergangsstønadRegelendring2026 korrekt`() {
            val søknad = lagTestRegelendring2026Søknad()

            val result = SøknadsskjemaMapper.tilDomene(søknad)

            assertThat(result.erRegelendring2026).isTrue()
            assertThat(result.hvaSituasjon?.verdier).containsExactlyInAnyOrder("barnUnder14Måneder", "barnSærligTilsyn")
            assertThat(result.inntekter?.verdier).containsExactly("arbeidstaker")
            assertThat(result.aktivitet.hvordanErArbeidssituasjonen?.verdier).containsExactly("arbeidstaker")
            assertThat(result.situasjon.gjelderDetteDeg.verdier).containsExactlyInAnyOrder("barnUnder14Måneder", "barnSærligTilsyn")
            assertThat(result.situasjon.sagtOppEllerRedusertStilling).isEqualTo("sagtOpp")
            assertThat(result.situasjon.oppsigelseReduksjonÅrsak).isEqualTo("Reduksjon forklaring")
            assertThat(result.aktivitet.firmaer).hasSize(2)
        }

        @Test
        internal fun `skal sette erRegelendring2026 og ikke populere aktivitet og situasjon for gammel søknad`() {
            val søknad = Testsøknad.søknadOvergangsstønad

            val result = SøknadsskjemaMapper.tilDomene(søknad)

            assertThat(result.erRegelendring2026).isFalse()
            assertThat(result.hvaSituasjon).isNull()
            assertThat(result.inntekter).isNull()
        }

        private fun lagTestRegelendring2026Søknad(): SøknadOvergangsstønadRegelendring2026 {
            val gammelSøknad = Testsøknad.søknadOvergangsstønad
            return SøknadOvergangsstønadRegelendring2026(
                innsendingsdetaljer = gammelSøknad.innsendingsdetaljer,
                personalia = gammelSøknad.personalia,
                sivilstandsdetaljer = gammelSøknad.sivilstandsdetaljer,
                medlemskapsdetaljer = gammelSøknad.medlemskapsdetaljer,
                bosituasjon = gammelSøknad.bosituasjon,
                barn = gammelSøknad.barn,
                hvaSituasjon = Søknadsfelt("Hva er situasjonen din?", listOf("Jeg har barn under 14 måneder", "Barnet trenger særlig tilsyn"), svarId = listOf("barnUnder14Måneder", "barnSærligTilsyn")),
                inntekter = Søknadsfelt("Har du inntekt?", listOf("Arbeidstaker"), svarId = listOf("arbeidstaker")),
                sagtOppEllerRedusertStilling = Søknadsfelt("Sagt opp?", "sagtOpp"),
                begrunnelseSagtOppEllerRedusertStilling = Søknadsfelt("Begrunnelse", "Reduksjon forklaring"),
                firmaer =
                    Søknadsfelt(
                        "Firmaer",
                        listOf(
                            Selvstendig(
                                firmanavn = Søknadsfelt("Firmanavn", "Firma AS"),
                                organisasjonsnummer = Søknadsfelt("Orgnr", "123456789"),
                                etableringsdato = Søknadsfelt("Etableringsdato", LocalDate.of(2020, 1, 1)),
                                hvordanSerArbeidsukenUt = Søknadsfelt("Arbeidsuke", "8 timer daglig"),
                            ),
                            Selvstendig(
                                firmanavn = Søknadsfelt("Firmanavn", "Bivirksomhet ENK"),
                                organisasjonsnummer = Søknadsfelt("Orgnr", "987654321"),
                                etableringsdato = Søknadsfelt("Etableringsdato", LocalDate.of(2022, 6, 1)),
                                hvordanSerArbeidsukenUt = Søknadsfelt("Arbeidsuke", "4 timer daglig"),
                            ),
                        ),
                    ),
                stønadsstart = gammelSøknad.stønadsstart,
            )
        }
    }
}
