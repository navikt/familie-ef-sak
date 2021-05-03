package no.nav.familie.ef.sak.integration.dto.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PdlPersonUtilTest {

    @Test
    internal fun `skal formetere navn med og uten mellomnavn`() {
        val navn = Navn("fornavn", null, "etternavn", metadata = Metadata(historisk = false))
        assertThat(navn.visningsnavn())
                .isEqualTo("fornavn etternavn")

        assertThat(navn.copy(mellomnavn = "mellomnavn").visningsnavn())
                .isEqualTo("fornavn mellomnavn etternavn")
    }

    @Test
    internal fun `gjeldende navn skal hente navnet som ble sist registrert`() {
        val gjeldendeNavn = listOf(Navn("a", null, "a", metadata = Metadata(historisk = true)),
                                   Navn("b", null, "b", metadata = Metadata(historisk = false)),
                                   Navn("c", null, "c", metadata = Metadata(historisk = true)))
                .gjeldende().fornavn
        assertThat(gjeldendeNavn).isEqualTo("b")
    }

    @Test
    internal fun `skal finne riktig gjeldende bostedsadresse`() {
        val gjeldendeAdresse = vegadresse("Gjeldende gate", "12", false)
        val historiskeAdresser: List<Bostedsadresse> = listOf(vegadresse("Historisk gate", "15", true),
                                                    vegadresse("Historisk gate", "13", true),
                                                    vegadresse("Historisk gate", "1", true))
        val adresser: List<Bostedsadresse> = historiskeAdresser + gjeldendeAdresse

        assertThat(adresser.gjeldende()!!.vegadresse!!.adressenavn).isEqualTo(gjeldendeAdresse.vegadresse!!.adressenavn)
        assertThat(adresser.gjeldende()!!.vegadresse!!.husnummer).isEqualTo(gjeldendeAdresse.vegadresse!!.husnummer)

        assertThat(historiskeAdresser.gjeldende()).isNull()
    }


    @Test
    internal fun `skal finne riktig gjeldende oppholdsadresse`() {
        val gjeldendeAdresse = oppholdsadresse("Gjeldende gate", "12", false)
        val historiskeAdresser: List<Oppholdsadresse> = listOf(oppholdsadresse("Historisk gate", "15", true),
                                                    oppholdsadresse("Historisk gate", "13", true),
                                                    oppholdsadresse("Historisk gate", "1", true))

        val adresser = historiskeAdresser + gjeldendeAdresse

        assertThat(adresser.gjeldende()!!.vegadresse!!.adressenavn).isEqualTo(gjeldendeAdresse.vegadresse!!.adressenavn)
        assertThat(adresser.gjeldende()!!.vegadresse!!.husnummer).isEqualTo(gjeldendeAdresse.vegadresse!!.husnummer)

        assertThat(historiskeAdresser.gjeldende()).isNull()
    }


    @Test
    internal fun `skal finne riktig gjeldende sivilstand`() {
        val sivilstander = listOf(Sivilstand(Sivilstandstype.UGIFT, null, null,  null, Metadata(true)),
        Sivilstand(Sivilstandstype.GIFT, null, null, null,  Metadata(true)),
        Sivilstand(Sivilstandstype.SEPARERT, null, null, null, Metadata(false)))

        assertThat(sivilstander.gjeldende().type).isEqualTo(Sivilstandstype.SEPARERT)
    }

    private fun vegadresse(gate: String, nr: String, historisk: Boolean): Bostedsadresse {
        return Bostedsadresse(
                null,
                null,
                null,
                null,
                null,
                Vegadresse(nr, null, null, gate, null, null, null, null, null),
                null,
                null,
                Metadata(historisk)
        )
    }

    private fun oppholdsadresse(gate: String, nr: String, historisk: Boolean): Oppholdsadresse {
        return Oppholdsadresse(
                null,
                null,
                null,
                null,
                Vegadresse(nr, null, null, gate, null, null, null, null, null),
                null,
                Metadata(historisk)
        )
    }

}
