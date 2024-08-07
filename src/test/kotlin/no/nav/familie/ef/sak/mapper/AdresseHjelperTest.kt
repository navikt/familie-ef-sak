package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.felles.util.opprettBarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now

internal class AdresseHjelperTest {
    private fun adresseOslo() = Vegadresse("1", "ABC", "123", "Oslogata", "01", null, "0101", null, null)

    private fun adresseTrondheim() = Vegadresse("1", "ABC", "123", "Trøndergata", "01", null, "7080", null, null)

    private fun adresseTromsø(brukenhetsnummer: String? = "123") = Vegadresse("1", "ABC", brukenhetsnummer, "Tromsøygata", "01", null, "9099", null, null)

    private fun adresseTromsøMatrikkel() = Vegadresse("1", "ABC", "123", "Tromsøygata", "01", null, "9099", null, 123L)

    private fun adresseBergen() = Vegadresse("1", "ABC", "123", "Bergensgata", "01", null, "5020", null, null)

    private fun matrikkeladresse(matrikkelId: Long? = 123L) = Matrikkeladresse(matrikkelId, "H0103", null, null)

    private val metadataGjeldende = Metadata(false)
    private val metadataHistorisk = Metadata(true)

    @Nested
    inner class BorPåSammeAdresse {
        @Test
        internal fun `forelder og barn bor på samme adresse`() {
            val barnAdresser =
                listOf(
                    lagAdresse(adresseBergen(), now().minusDays(100), null, null, metadataHistorisk),
                    lagAdresse(adresseTromsø(), now().minusDays(1), null, null, metadataGjeldende),
                    lagAdresse(adresseTrondheim(), null, null, null, metadataHistorisk),
                )
            val forelderAdresser =
                listOf(
                    lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100), null, metadataHistorisk),
                    lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1), null, metadataHistorisk),
                    lagAdresse(adresseTromsø(), now().minusDays(1), null, null, metadataGjeldende),
                )
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `forelder og barn bor på samme adresse også med brukenhetsnummer null og tom streng `() {
            val barnAdresser =
                listOf(
                    lagAdresse(adresseTromsø(brukenhetsnummer = ""), now().minusDays(1), null, null, metadataGjeldende),
                )
            val forelderAdresser =
                listOf(
                    lagAdresse(adresseTromsø(brukenhetsnummer = null), now().minusDays(2), null, null, metadataGjeldende),
                )
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)

            val vegadresseForelder = forelderAdresser.gjeldende()!!.vegadresse!!
            val vegadresseBarn = barn.bostedsadresse.gjeldende()!!.vegadresse!!

            // Forventer at de ikke er like da de har forskjellige bruksenhetsnummer: null og ""
            assertThat(vegadresseBarn == vegadresseForelder).isFalse
            assertThat(vegadresseBarn.erSammeVegadresse(vegadresseForelder)).isTrue
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `Forventer ulike adresser ved ulike bruksenhetsnummer `() {
            val barnAdresse = adresseTromsø(brukenhetsnummer = "")
            val forelderAdresse = adresseTromsø(brukenhetsnummer = null)

            // Guard - forventer at de ikke er like da de har forskjellige bruksenhetsnummer: null og ""
            assertThat(barnAdresse == forelderAdresse).isFalse
            assertThat(barnAdresse.erSammeVegadresse(forelderAdresse)).isTrue

            val ulikVegadresseBarn = forelderAdresse.copy(bruksenhetsnummer = "H0404")
            assertThat(ulikVegadresseBarn.erSammeVegadresse(forelderAdresse)).isFalse
        }

        @Test
        internal fun `forelder og barn med samme vegadresse med matrikkelId`() {
            val barnAdresser =
                listOf(
                    lagAdresse(adresseTromsøMatrikkel(), now().minusDays(1)),
                )
            val forelderAdresser =
                listOf(
                    lagAdresse(adresseTromsøMatrikkel(), now().minusDays(1)),
                )
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `forelder og barn med samme matrikkeladresse`() {
            val barnAdresser = listOf(lagAdresse(adresseBergen(), now().minusDays(1), null, matrikkeladresse()))
            val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse()))
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `returnere false for matrikkeladresser med samme matrikkelId men forskjellig bruksenhetsnummer`() {
            val barnAdresser =
                listOf(
                    lagAdresse(
                        adresseBergen(),
                        now().minusDays(1),
                        null,
                        matrikkeladresse().copy(bruksenhetsnummer = "H1701"),
                    ),
                )
            val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse()))
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isFalse()
        }

        @Test
        internal fun `returnere false for vegadresser med samme matrikkelId men forskjellig bruksenhetsnummer`() {
            val barnAdresser = listOf(lagAdresse(adresseBergen().copy(matrikkelId = 123, bruksenhetsnummer = "H0103")))
            val forelderAdresser =
                listOf(lagAdresse(adresseBergen().copy(matrikkelId = 123, bruksenhetsnummer = "H1701")))
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isFalse()
        }

        @Test
        internal fun `forelder og barn med ulik matrikkeladresse`() {
            val barnAdresser = listOf(lagAdresse(adresseBergen(), now().minusDays(1), null, matrikkeladresse()))
            val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse(999L)))
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isFalse
        }

        @Test
        internal fun `forelder og barn bor ikke på samme adresse`() {
            val barn1Adresser =
                listOf(
                    lagAdresse(adresseBergen(), now().minusDays(1)),
                    lagAdresse(adresseTromsø(), now().minusDays(100)),
                )
            val barn2Adresser =
                listOf(
                    lagAdresse(adresseBergen(), now().minusDays(1)),
                    lagAdresse(adresseTromsø(), null),
                )

            val ugyldigeAdresser =
                listOf(
                    lagAdresse(null, null),
                )

            val forelderAdresser =
                listOf(
                    lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100)),
                    lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1)),
                    lagAdresse(adresseTromsø(), now().minusDays(1)),
                )

            val barn1 = opprettBarnMedIdent(personIdent = "", bostedsadresse = barn1Adresser)
            val barn2 = opprettBarnMedIdent(personIdent = "", bostedsadresse = barn2Adresser)
            val barn3 = opprettBarnMedIdent(personIdent = "", bostedsadresse = ugyldigeAdresser)
            val barn4 = opprettBarnMedIdent(personIdent = "")

            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn1, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn2, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn3, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn4, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn4, emptyList())).isFalse
        }

        @Test
        internal fun `forelder og barn bor på samme adresse selv om det ikke finnes gyldighetsdato`() {
            val barnAdresser =
                listOf(
                    lagAdresse(vegadresse = adresseTromsø(), metadata = metadataGjeldende),
                    lagAdresse(vegadresse = adresseOslo(), metadata = metadataHistorisk),
                )
            val forelderAdresser =
                listOf(
                    lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100), null, metadataHistorisk),
                    lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1), null, metadataHistorisk),
                    lagAdresse(adresseTromsø(), now().minusDays(1), null, null, metadataGjeldende),
                )
            val barn = opprettBarnMedIdent(personIdent = "", bostedsadresse = barnAdresser)

            assertThat(AdresseHjelper.harRegistrertSammeBostedsadresseSomForelder(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `delt bosted er innenfor nåtid med angitt dato now, forvent harDeltBosted lik true`() {
            val barnMedDeltBosted =
                opprettBarnMedIdent(
                    personIdent = "",
                    fødsel = PdlTestdataHelper.fødsel(now().minusYears(2)),
                    deltBosted = listOf(DeltBosted(now(), null, null, null, metadataGjeldende)),
                )
            assertThat(AdresseHjelper.harDeltBosted(barnMedDeltBosted, now())).isTrue
        }

        @Test
        internal fun `delt bosted er utenfor nåtid med angitt dato now, forvent harDeltBosted lik false`() {
            val barnMedDeltBosted =
                opprettBarnMedIdent(
                    personIdent = "",
                    fødsel = PdlTestdataHelper.fødsel(now().minusYears(2)),
                    deltBosted = listOf(DeltBosted(now().plusDays(1), null, null, null, metadataGjeldende)),
                )
            assertThat(AdresseHjelper.harDeltBosted(barnMedDeltBosted, now())).isFalse
        }

        @Test
        internal fun `Barn over 18, skal ikke ha delt bosted`() {
            val barnMedDeltBosted =
                opprettBarnMedIdent(
                    personIdent = "",
                    fødsel = PdlTestdataHelper.fødsel(now().minusYears(18)),
                    deltBosted = listOf(DeltBosted(now().minusDays(1), null, null, null, metadataGjeldende)),
                )
            assertThat(AdresseHjelper.harDeltBosted(barnMedDeltBosted, now())).isFalse
        }

        @Test
        internal fun `delt bosted finnes ikke, forvent harDeltBosted lik false`() {
            val barnMedDeltBosted = opprettBarnMedIdent(personIdent = "", deltBosted = emptyList(), fødsel = PdlTestdataHelper.fødsel(now().minusYears(2)))
            assertThat(AdresseHjelper.harDeltBosted(barnMedDeltBosted, now())).isFalse
        }

        @Test
        internal fun `delt bosted er før dato for delt bosted, forvent harDeltBosted lik false`() {
            val barnMedDeltBosted =
                opprettBarnMedIdent(
                    personIdent = "",
                    fødsel = PdlTestdataHelper.fødsel(now().minusYears(2)),
                    deltBosted = listOf(DeltBosted(now(), null, null, null, metadataGjeldende)),
                )
            assertThat(AdresseHjelper.harDeltBosted(barnMedDeltBosted, now().minusDays(1))).isFalse
        }

        @Test
        internal fun `delt bosted er innenfor dato for delt bosted, forvent harDeltBosted lik true`() {
            val barnMedDeltBosted =
                opprettBarnMedIdent(
                    personIdent = "",
                    fødsel = PdlTestdataHelper.fødsel(now().minusYears(2)),
                    deltBosted = listOf(DeltBosted(now(), null, null, null, metadataGjeldende)),
                )
            assertThat(AdresseHjelper.harDeltBosted(barnMedDeltBosted, now().plusDays(1))).isTrue
        }
    }

    @Nested
    inner class SorterAdresser {
        @Test
        internal fun `sortering av adresser skal sortere de per type først, aktiv, og sen per startdato`() {
            val gjeldendeBostedsadresse =
                lagAdresseDto(AdresseType.BOSTEDADRESSE, now().minusDays(5), erGjeldende = true)
            val historiskBostedsadresse =
                lagAdresseDto(
                    AdresseType.BOSTEDADRESSE,
                    now().minusYears(1),
                    now().minusDays(5),
                )
            val historiskBostedsadresseEtterAktivAdresse =
                lagAdresseDto(
                    AdresseType.BOSTEDADRESSE,
                    now(),
                )
            val aktivOppholdsadresse = lagAdresseDto(AdresseType.OPPHOLDSADRESSE, now())
            val historiskKontaktadresse =
                lagAdresseDto(
                    AdresseType.KONTAKTADRESSE,
                    now().minusDays(15),
                    now().minusDays(14),
                )
            val historiskKontaktadresseUtland = lagAdresseDto(AdresseType.KONTAKTADRESSE_UTLAND, now(), now())

            val adresser =
                listOf(
                    historiskBostedsadresse,
                    historiskBostedsadresseEtterAktivAdresse,
                    historiskKontaktadresseUtland,
                    aktivOppholdsadresse,
                    historiskKontaktadresse,
                    gjeldendeBostedsadresse,
                )
            val sorterteAdresser = AdresseHjelper.sorterAdresser(adresser)
            assertThat(sorterteAdresser)
                .containsExactly(
                    gjeldendeBostedsadresse,
                    historiskBostedsadresseEtterAktivAdresse,
                    historiskBostedsadresse,
                    aktivOppholdsadresse,
                    historiskKontaktadresse,
                    historiskKontaktadresseUtland,
                )
        }
    }

    private fun lagAdresse(
        vegadresse: Vegadresse?,
        gyldighetstidspunkt: LocalDate? = null,
        opphørstidspunkt: LocalDate? = null,
        matrikkeladresse: Matrikkeladresse? = null,
        metadata: Metadata? = null,
    ): Bostedsadresse =
        Bostedsadresse(
            vegadresse = vegadresse,
            angittFlyttedato = null,
            gyldigFraOgMed = gyldighetstidspunkt,
            gyldigTilOgMed = opphørstidspunkt,
            coAdressenavn = null,
            utenlandskAdresse = null,
            ukjentBosted = null,
            matrikkeladresse = matrikkeladresse,
            metadata = metadata ?: metadataGjeldende,
        )

    private fun lagAdresseDto(
        type: AdresseType,
        gyldigFraOgMed: LocalDate?,
        gyldigTilOgMed: LocalDate? = null,
        erGjeldende: Boolean = false,
    ): AdresseDto =
        AdresseDto(
            visningsadresse = "Oslogata 1",
            type = type,
            gyldigFraOgMed = gyldigFraOgMed,
            gyldigTilOgMed = gyldigTilOgMed,
            erGjeldende = erGjeldende,
        )
}
