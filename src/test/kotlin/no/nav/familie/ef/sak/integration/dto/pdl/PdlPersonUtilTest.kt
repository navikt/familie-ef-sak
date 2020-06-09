package no.nav.familie.ef.sak.integration.dto.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class PdlPersonUtilTest {

    private val startdato = LocalDate.of(2020, 1, 1)
    private val sluttdato = startdato.plusDays(1)

    @Test
    internal fun `Bostedsadresse formatert adresse`() {
        val bostedsadresse = Bostedsadresse(
                angittFlyttedato = startdato,
                coAdressenavn = null,
                folkeregistermetadata = Folkeregistermetadata(gyldighetstidspunkt = LocalDateTime.now(),
                                                              opphørstidspunkt = startdato.plusDays(1).atStartOfDay()),
                vegadresse = vegadresse(),
                ukjentBosted = UkjentBosted(bostedskommune = "ukjentBostedKommune")
        )
        assertThat(bostedsadresse.tilFormatertAdresse())
                .isEqualTo("Charlies vei 13 b, 0575 Oslo")

        assertThat(bostedsadresse.copy(coAdressenavn = "co").tilFormatertAdresse())
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co, Charlies vei 13 b, 0575 Oslo")

        assertThat(bostedsadresse.copy(vegadresse = null).tilFormatertAdresse())
                .withFailMessage("Skal skrive ut ukjentBosted når vegadresse er null")
                .isEqualTo("ukjentBostedKommune")
    }

    @Test
    internal fun `Oppholdsadresse formatert adresse`() {
        val oppholdsadresse = Oppholdsadresse(oppholdsadressedato = null,
                                              coAdressenavn = null,
                                              utenlandskAdresse = null,
                                              vegadresse = null,
                                              oppholdAnnetSted = null)

        assertThat(oppholdsadresse.copy(vegadresse = tomVegadresse().copy(adressenavn = "adresse")).tilFormatertAdresse())
                .isEqualTo("adresse, Oslo")

        assertThat(oppholdsadresse.copy(coAdressenavn = "co").tilFormatertAdresse())
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co")

        assertThat(oppholdsadresse.copy(utenlandskAdresse = utenlandskAdresse()).tilFormatertAdresse())
                .withFailMessage("Skal skrive ut utenlandskAdresse når vegadresse er null")
                .isEqualTo("a 1, 001 bysted, region, NOR")
    }

    @Test
    internal fun `Kontaktadresse formatert adresse utland`() {
        val kontaktadresse = kontaktadresse(KontaktadresseType.UTLAND)

        assertThat(kontaktadresse.copy(utenlandskAdresse = utenlandskAdresse()).tilFormatertAdresse())
                .isEqualTo("a 1, 001 bysted, region, NOR")

        assertThat(kontaktadresse.copy(coAdressenavn = "co").tilFormatertAdresse())
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co")

        assertThat(kontaktadresse.copy(utenlandskAdresseIFrittFormat = utenlandAdresseFrittFormat())
                           .tilFormatertAdresse())
                .withFailMessage("Skal skrive ut utenlandskAdresseIFrittFormat når utenlandskAdresse er null")
                .isEqualTo("1, 2, 3, 0575 by, NOR")

    }

    @Test
    internal fun `Kontaktadresse formatert adresse innland`() {
        val kontaktadresse = kontaktadresse(KontaktadresseType.INNLAND)

        assertThat(kontaktadresse.copy(vegadresse = vegadresse()).tilFormatertAdresse())
                .isEqualTo("Charlies vei 13 b, 0575 Oslo")

        assertThat(kontaktadresse.copy(coAdressenavn = "co").tilFormatertAdresse())
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co")

        assertThat(kontaktadresse.copy(postboksadresse = Postboksadresse(postboks = "postboks",
                                                                         postbokseier = "eier",
                                                                         postnummer = "0575")).tilFormatertAdresse())
                .withFailMessage("Skal skrive ut postboksadresse når vegadresse er null")
                .isEqualTo("eier, postboks, 0575 Oslo")

        assertThat(kontaktadresse.copy(postadresseIFrittFormat = postadresseFrittFormat())
                           .tilFormatertAdresse())
                .withFailMessage("Skal skrive ut postboksadresse når vegadresse er null")
                .isEqualTo("1, 2, 3, 0575 Oslo")

    }

    private fun utenlandAdresseFrittFormat(): UtenlandskAdresseIFrittFormat {
        return UtenlandskAdresseIFrittFormat("1",
                                             "2",
                                             "3",
                                             "by",
                                             "NOR",
                                             "0575")
    }

    private fun postadresseFrittFormat(): PostadresseIFrittFormat {
        return PostadresseIFrittFormat("1",
                                       "2",
                                       "3",
                                       "0575")
    }

    private fun kontaktadresse(kontaktadresseType: KontaktadresseType) =
            Kontaktadresse(coAdressenavn = null,
                           gyldigFraOgMed = startdato,
                           gyldigTilOgMed = sluttdato,
                           postadresseIFrittFormat = null,
                           postboksadresse = null,
                           type = kontaktadresseType,
                           utenlandskAdresse = null,
                           utenlandskAdresseIFrittFormat = null,
                           vegadresse = null)

    private fun vegadresse(): Vegadresse =
            Vegadresse(husnummer = "13",
                       husbokstav = "b",
                       adressenavn = "Charlies vei",
                       kommunenummer = "0301",
                       postnummer = "0575",
                       bruksenhetsnummer = "",
                       tilleggsnavn = "tilleggsnavn",
                       koordinater = null)

    private fun utenlandskAdresse(): UtenlandskAdresse =
            UtenlandskAdresse(adressenavnNummer = "a 1",
                              bySted = "bysted",
                              bygningEtasjeLeilighet = "etasje",
                              landkode = "NOR",
                              postboksNummerNavn = "000",
                              postkode = "001",
                              regionDistriktOmraade = "region")

    private fun tomVegadresse(): Vegadresse =
            Vegadresse(husnummer = null,
                       husbokstav = null,
                       adressenavn = null,
                       kommunenummer = null,
                       postnummer = null,
                       bruksenhetsnummer = null,
                       tilleggsnavn = null,
                       koordinater = null)
}
