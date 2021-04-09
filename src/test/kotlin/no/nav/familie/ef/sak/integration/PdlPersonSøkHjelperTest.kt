package no.nav.familie.ef.sak.integration

import no.nav.familie.ef.sak.api.dto.BostedsadresseDto
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregistermetadata
import no.nav.familie.ef.sak.integration.dto.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Vegadresse
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class PdlPersonSøkHjelperTest {

    @Test
    internal fun `søker har matrikkeladresse uten bruksenhetsnummer`() {
        val matrikkelId = 123L
        val resultat = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(null, Matrikkeladresse(matrikkelId, null)))
        Assertions.assertThat(resultat.size).isEqualTo(1)
        Assertions.assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.matrikkeladresse.matrikkelId")
        Assertions.assertThat(resultat[0].searchRule.equals).isEqualTo(matrikkelId.toString())
    }

    @Test
    internal fun `søker har matrikkeladresse med bruksenhetsnummer`() {
        val matrikkelId = 123L
        val bruksenhetsnummer = "2"
        val resultat =
                PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(null, Matrikkeladresse(matrikkelId, bruksenhetsnummer)))
        Assertions.assertThat(resultat.size).isEqualTo(2)
        Assertions.assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.matrikkeladresse.matrikkelId")
        Assertions.assertThat(resultat[0].searchRule.equals).isEqualTo(matrikkelId.toString())
        Assertions.assertThat(resultat[1].fieldName).isEqualTo("person.bostedsadresse.matrikkeladresse.bruksenhetsnummer")
        Assertions.assertThat(resultat[1].searchRule.equals).isEqualTo(bruksenhetsnummer)
    }

    @Test
    internal fun `søker har bare vegadresse`() {
        val vegadresse = Vegadresse("1",
                                    "ABC",
                                    "123",
                                    "Oslogata",
                                    "01",
                                    null,
                                    "0101",
                                    null,
                                    null)

        val resultat = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(vegadresse, null))
        Assertions.assertThat(resultat.size).isEqualTo(5)
        Assertions.assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.vegadresse.adressenavn")
        Assertions.assertThat(resultat[0].searchRule.equals).isEqualTo(vegadresse.adressenavn)
        Assertions.assertThat(resultat[1].fieldName).isEqualTo("person.bostedsadresse.vegadresse.bruksenhetsnummer")
        Assertions.assertThat(resultat[1].searchRule.equals).isEqualTo(vegadresse.bruksenhetsnummer)
        Assertions.assertThat(resultat[2].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husbokstav")
        Assertions.assertThat(resultat[2].searchRule.equals).isEqualTo(vegadresse.husbokstav)
        Assertions.assertThat(resultat[3].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husnummer")
        Assertions.assertThat(resultat[3].searchRule.equals).isEqualTo(vegadresse.husnummer)
        Assertions.assertThat(resultat[4].fieldName).isEqualTo("person.bostedsadresse.vegadresse.postnummer")
        Assertions.assertThat(resultat[4].searchRule.equals).isEqualTo(vegadresse.postnummer)
    }

    @Test
    internal fun `søker har bare vegadresse uten husbokstav og bruksenhet`() {
        val vegadresse = Vegadresse("1",
                                    null,
                                    null,
                                    "Oslogata",
                                    "01",
                                    null,
                                    "0101",
                                    null,
                                    null)

        val resultat = PdlPersonSøkHjelper.lagPdlPersonSøkKriterier(lagAdresse(vegadresse, null))
        Assertions.assertThat(resultat.size).isEqualTo(3)
        Assertions.assertThat(resultat[0].fieldName).isEqualTo("person.bostedsadresse.vegadresse.adressenavn")
        Assertions.assertThat(resultat[0].searchRule.equals).isEqualTo(vegadresse.adressenavn)
        Assertions.assertThat(resultat[1].fieldName).isEqualTo("person.bostedsadresse.vegadresse.husnummer")
        Assertions.assertThat(resultat[1].searchRule.equals).isEqualTo(vegadresse.husnummer)
        Assertions.assertThat(resultat[2].fieldName).isEqualTo("person.bostedsadresse.vegadresse.postnummer")
        Assertions.assertThat(resultat[2].searchRule.equals).isEqualTo(vegadresse.postnummer)
    }

    private fun lagAdresse(vegadresse: Vegadresse?, matrikkeladresse: Matrikkeladresse?): BostedsadresseDto {
        return BostedsadresseDto(
                vegadresse = vegadresse,
                matrikkeladresse = matrikkeladresse
        )
    }
}