package no.nav.familie.ef.sak.felles.integration

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlPersonSøkHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.SearchRuleEquals
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.SearchRuleExists
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PdlPersonSøkHjelperTest {
    @Test
    internal fun `søker har matrikkeladresse uten bruksenhetsnummer`() {
        val matrikkelId = 123L
        val resultat =
            PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(null, Matrikkeladresse(matrikkelId, null, null, null)))
        assertThat(resultat.size).isEqualTo(1)
        assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.matrikkeladresse.matrikkelId")
        assertThat((resultat[0].searchRule as SearchRuleEquals).equals).isEqualTo(matrikkelId.toString())
    }

    @Test
    internal fun `søker har matrikkeladresse med bruksenhetsnummer`() {
        val matrikkelId = 123L
        val bruksenhetsnummer = "2"
        val resultat =
            PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(
                lagAdresse(
                    null,
                    Matrikkeladresse(
                        matrikkelId,
                        bruksenhetsnummer,
                        null,
                        null,
                    ),
                ),
            )
        assertThat(resultat.size).isEqualTo(2)
        assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.matrikkeladresse.matrikkelId")
        assertThat((resultat[0].searchRule as SearchRuleEquals).equals).isEqualTo(matrikkelId.toString())
        assertThat(resultat[1].fieldName).isEqualTo("person.bostedsadresse.matrikkeladresse.bruksenhetsnummer")
        assertThat((resultat[1].searchRule as SearchRuleEquals).equals).isEqualTo(bruksenhetsnummer)
    }

    @Test
    internal fun `søker har bare vegadresse`() {
        val vegadresse =
            Vegadresse(
                "1",
                "ABC",
                "123",
                "Oslogata",
                "01",
                null,
                "0101",
                null,
                null,
            )

        val resultat = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(vegadresse, null))
        assertThat(resultat.size).isEqualTo(5)
        assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.vegadresse.adressenavn")
        assertThat((resultat[0].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.adressenavn)
        assertThat(resultat[1].fieldName).isEqualTo("person.bostedsadresse.vegadresse.bruksenhetsnummer")
        assertThat((resultat[1].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.bruksenhetsnummer)
        assertThat(resultat[2].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husbokstav")
        assertThat((resultat[2].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.husbokstav)
        assertThat(resultat[3].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husnummer")
        assertThat((resultat[3].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.husnummer)
        assertThat(resultat[4].fieldName).isEqualTo("person.bostedsadresse.vegadresse.postnummer")
        assertThat((resultat[4].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.postnummer)
    }

    @Test
    internal fun `søker har bare vegadresse uten husbokstav og bruksenhet`() {
        val vegadresse =
            Vegadresse(
                "1",
                null,
                null,
                "Oslogata",
                "01",
                null,
                "0101",
                null,
                null,
            )

        val resultat = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(vegadresse, null))
        assertThat(resultat.size).isEqualTo(5)
        assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.vegadresse.adressenavn")
        assertThat((resultat[0].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.adressenavn)
        assertThat(resultat[1].fieldName).isEqualTo("person.bostedsadresse.vegadresse.bruksenhetsnummer")
        assertThat((resultat[1].searchRule as SearchRuleExists).exists).isFalse
        assertThat(resultat[2].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husbokstav")
        assertThat((resultat[2].searchRule as SearchRuleExists).exists).isFalse
        assertThat(resultat[3].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husnummer")
        assertThat((resultat[3].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.husnummer)
        assertThat(resultat[4].fieldName).isEqualTo("person.bostedsadresse.vegadresse.postnummer")
        assertThat((resultat[4].searchRule as SearchRuleEquals).equals).isEqualTo(vegadresse.postnummer)
    }

    private fun lagAdresse(
        vegadresse: Vegadresse?,
        matrikkeladresse: Matrikkeladresse?,
    ): Bostedsadresse =
        Bostedsadresse(
            angittFlyttedato = null,
            gyldigFraOgMed = null,
            gyldigTilOgMed = null,
            vegadresse = vegadresse,
            matrikkeladresse = matrikkeladresse,
            coAdressenavn = null,
            utenlandskAdresse = null,
            ukjentBosted = null,
            metadata = Metadata(false),
        )
}
