package no.nav.familie.ef.sak.mapper

import io.mockk.verify
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Kontaktadresse
import no.nav.familie.ef.sak.integration.dto.pdl.KontaktadresseType
import no.nav.familie.ef.sak.integration.dto.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.PostadresseIFrittFormat
import no.nav.familie.ef.sak.integration.dto.pdl.Postboksadresse
import no.nav.familie.ef.sak.integration.dto.pdl.UkjentBosted
import no.nav.familie.ef.sak.integration.dto.pdl.UtenlandskAdresse
import no.nav.familie.ef.sak.integration.dto.pdl.UtenlandskAdresseIFrittFormat
import no.nav.familie.ef.sak.integration.dto.pdl.Vegadresse
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.config.KodeverkServiceMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class AdresseMapperTest {

    private val startdato = LocalDate.of(2020, 1, 1)
    private val sluttdato = startdato.plusDays(1)

    private val kodeverkService = KodeverkServiceMock().kodeverkService()
    private val mapper = AdresseMapper(kodeverkService)
    private val metadataGjeldende = Metadata(historisk = false)
    private val matrikkeladresse = Matrikkeladresse(matrikkelId = null,
                                                    bruksenhetsnummer = "bruksenhet",
                                                    tilleggsnavn = "tilleggsnavn",
                                                    postnummer = "")
    private val bostedsadresse = Bostedsadresse(angittFlyttedato = startdato.plusDays(1),
                                                gyldigFraOgMed = startdato,
                                                gyldigTilOgMed = startdato.plusDays(1),
                                                coAdressenavn = null,
                                                utenlandskAdresse = utenlandskAdresse(),
                                                vegadresse = vegadresse(),
                                                ukjentBosted = UkjentBosted(bostedskommune = "ukjentBostedKommune"),
                                                matrikkeladresse = matrikkeladresse,
                                                metadata = metadataGjeldende
    )

    @Test
    internal fun `Bostedsadresse formatert adresse`() {
        assertThat(mapper.tilAdresse(bostedsadresse).visningsadresse)
                .isEqualTo("Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "co")).visningsadresse)
                .isEqualTo("c/o co, Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "")).visningsadresse)
                .isEqualTo("Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(vegadresse = null)).visningsadresse)
                .withFailMessage("Skal skrive ut matrikkeladresse når vegadresse er null")
                .isEqualTo("tilleggsnavn, bruksenhet, Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(vegadresse = null, matrikkeladresse = null)).visningsadresse)
                .withFailMessage("Skal skrive ut utenlands adresse når vegadresse er null")
                .isEqualTo("Vei 1, 19800 Svenskt sted, region, Sverige")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(vegadresse = null,
                                                         matrikkeladresse = null,
                                                         utenlandskAdresse = null)).visningsadresse)
                .withFailMessage("Skal skrive ut ukjentBosted når vegadresse er null")
                .isEqualTo("ukjentBostedKommune")
    }

    @Test
    internal fun `Skal ikke endre format på coAdresse hvis prefiks finnes fra før`() {
        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "c/o co")).visningsadresse)
                .isEqualTo("c/o co, Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "C/O co")).visningsadresse)
                .isEqualTo("C/O co, Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "V/ co")).visningsadresse)
                .isEqualTo("V/ co, Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "v/DBO co")).visningsadresse)
                .isEqualTo("v/DBO co, Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(bostedsadresse.copy(coAdressenavn = "DBO v/ co")).visningsadresse)
                .isEqualTo("DBO v/ co, Charlies vei 13 b, tilleggsnavn, 0575 Oslo")
    }

    @Test
    internal fun `Skal kalle på hentPoststed med gyldigFraOgMed når gyldigFraOgMed finnes`() {
        mapper.tilAdresse(bostedsadresse)
        verify { kodeverkService.hentPoststed(any(), bostedsadresse.gyldigFraOgMed!!) }
    }

    @Test
    internal fun `skal håndtere feilregistrert dato som null`() {
        val adresse = mapper.tilAdresse(bostedsadresse.copy(angittFlyttedato = LocalDate.of(1, 1, 1)))
        assertThat(adresse.angittFlyttedato).isNull()
    }

    @Test
    internal fun `Oppholdsadresse formatert adresse`() {
        val oppholdsadresse = Oppholdsadresse(gyldigFraOgMed = null,
                                              coAdressenavn = null,
                                              utenlandskAdresse = null,
                                              vegadresse = null,
                                              oppholdAnnetSted = null,
                                              metadata = metadataGjeldende)

        val adresseMedVegadresseMedAdressenavn = oppholdsadresse.copy(vegadresse = tomVegadresse().copy(adressenavn = "adresse"))
        assertThat(mapper.tilAdresse(adresseMedVegadresseMedAdressenavn).visningsadresse)
                .isEqualTo("adresse")

        assertThat(mapper.tilAdresse(oppholdsadresse.copy(coAdressenavn = "co")).visningsadresse)
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co")

        assertThat(mapper.tilAdresse(oppholdsadresse.copy(utenlandskAdresse = utenlandskAdresse())).visningsadresse)
                .withFailMessage("Skal skrive ut utenlandskAdresse når vegadresse er null")
                .isEqualTo("Vei 1, 19800 Svenskt sted, region, Sverige")
    }

    @Test
    internal fun `Kontaktadresse formatert adresse utland`() {
        val kontaktadresse = kontaktadresse(KontaktadresseType.UTLAND)

        assertThat(mapper.tilAdresse(kontaktadresse.copy(utenlandskAdresse = utenlandskAdresse())).visningsadresse)
                .isEqualTo("Vei 1, 19800 Svenskt sted, region, Sverige")

        assertThat(mapper.tilAdresse(kontaktadresse.copy(coAdressenavn = "co")).visningsadresse)
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co")

        assertThat(mapper.tilAdresse(kontaktadresse.copy(utenlandskAdresseIFrittFormat =
                                                         utenlandskAdresseFrittFormat())).visningsadresse)
                .withFailMessage("Skal skrive ut utenlandskAdresseIFrittFormat når utenlandskAdresse er null")
                .isEqualTo("1, 2, 3, 0575 by, Norge")

    }

    @Test
    internal fun `Kontaktadresse formatert adresse innland`() {
        val kontaktadresse = kontaktadresse(KontaktadresseType.INNLAND)

        assertThat(mapper.tilAdresse(kontaktadresse.copy(vegadresse = vegadresse())).visningsadresse)
                .isEqualTo("Charlies vei 13 b, tilleggsnavn, 0575 Oslo")

        assertThat(mapper.tilAdresse(kontaktadresse.copy(coAdressenavn = "co")).visningsadresse)
                .withFailMessage("Skal skrive ut co adressen")
                .isEqualTo("c/o co")

        val adresseMedPostboksadresse = kontaktadresse.copy(postboksadresse = Postboksadresse(postboks = "postboks",
                                                                                              postbokseier = "eier",
                                                                                              postnummer = "0575"))
        assertThat(mapper.tilAdresse(adresseMedPostboksadresse).visningsadresse)
                .withFailMessage("Skal skrive ut postboksadresse når vegadresse er null")
                .isEqualTo("eier, postboks, 0575 Oslo")

        assertThat(mapper.tilAdresse(kontaktadresse.copy(postadresseIFrittFormat = postadresseFrittFormat())
        ).visningsadresse)
                .withFailMessage("Skal skrive ut postboksadresse når vegadresse er null")
                .isEqualTo("1, 2, 3, 0575 Oslo")

    }

    private fun utenlandskAdresseFrittFormat(): UtenlandskAdresseIFrittFormat {
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
                       koordinater = null,
                       matrikkelId = null)

    private fun utenlandskAdresse(): UtenlandskAdresse =
            UtenlandskAdresse(adressenavnNummer = "Vei 1",
                              bySted = "Svenskt sted",
                              bygningEtasjeLeilighet = "etasje",
                              landkode = "SWE",
                              postboksNummerNavn = "000",
                              postkode = "19800",
                              regionDistriktOmraade = "region")

    private fun tomVegadresse(): Vegadresse =
            Vegadresse(husnummer = null,
                       husbokstav = null,
                       adressenavn = null,
                       kommunenummer = null,
                       postnummer = null,
                       bruksenhetsnummer = null,
                       tilleggsnavn = null,
                       koordinater = null,
                       matrikkelId = null)
}
