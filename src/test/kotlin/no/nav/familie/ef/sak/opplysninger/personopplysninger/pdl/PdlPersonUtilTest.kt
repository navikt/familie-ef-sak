package no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
        val gjeldendeNavn = listOf(Navn("b", null, "b", metadata = Metadata(historisk = false))).gjeldende().fornavn
        assertThat(gjeldendeNavn).isEqualTo("b")
    }

    @Test
    internal fun `skal finne riktig gjeldende bostedsadresse`() {
        val gjeldendeAdresse = vegadresse("Gjeldende gate", "12", false)
        val historiskeAdresser: List<Bostedsadresse> =
            listOf(
                vegadresse("Historisk gate", "15", true),
                vegadresse("Historisk gate", "13", true),
                vegadresse("Historisk gate", "1", true),
            )
        val adresser: List<Bostedsadresse> = historiskeAdresser + gjeldendeAdresse

        assertThat(adresser.gjeldende()!!.vegadresse!!.adressenavn).isEqualTo(gjeldendeAdresse.vegadresse!!.adressenavn)
        assertThat(adresser.gjeldende()!!.vegadresse!!.husnummer).isEqualTo(gjeldendeAdresse.vegadresse!!.husnummer)

        assertThat(historiskeAdresser.gjeldende()).isNull()
    }

    @Test
    internal fun `skal finne riktig gjeldende oppholdsadresse`() {
        val gjeldendeAdresse = oppholdsadresse("Gjeldende gate", "12", false)
        val historiskeAdresser: List<Oppholdsadresse> =
            listOf(
                oppholdsadresse("Historisk gate", "15", true),
                oppholdsadresse("Historisk gate", "13", true),
                oppholdsadresse("Historisk gate", "1", true),
            )

        val adresser = historiskeAdresser + gjeldendeAdresse

        assertThat(adresser.gjeldende()!!.vegadresse!!.adressenavn).isEqualTo(gjeldendeAdresse.vegadresse!!.adressenavn)
        assertThat(adresser.gjeldende()!!.vegadresse!!.husnummer).isEqualTo(gjeldendeAdresse.vegadresse!!.husnummer)

        assertThat(historiskeAdresser.gjeldende()).isNull()
    }

    @Test
    internal fun `skal finne riktig gjeldende sivilstand`() {
        val sivilstander =
            listOf(
                Sivilstand(Sivilstandstype.UGIFT, null, null, null, Metadata(true)),
                Sivilstand(Sivilstandstype.GIFT, null, null, null, Metadata(true)),
                Sivilstand(Sivilstandstype.SEPARERT, null, null, null, Metadata(false)),
            )

        assertThat(sivilstander.gjeldende().type).isEqualTo(Sivilstandstype.SEPARERT)
    }

    private fun vegadresse(
        gate: String,
        nr: String,
        historisk: Boolean,
    ): Bostedsadresse =
        Bostedsadresse(
            null,
            null,
            null,
            null,
            null,
            Vegadresse(nr, null, null, gate, null, null, null, null, null),
            null,
            null,
            Metadata(historisk),
        )

    private fun oppholdsadresse(
        gate: String,
        nr: String,
        historisk: Boolean,
    ): Oppholdsadresse =
        Oppholdsadresse(
            null,
            null,
            null,
            null,
            Vegadresse(nr, null, null, gate, null, null, null, null, null),
            null,
            Metadata(historisk),
        )

    @Nested
    inner class GjeldendeIdent {
        private val fnr = PdlIdent("12345678901", false, PdlIdentGruppe.FOLKEREGISTERIDENT)
        private val npid = PdlIdent("01226512345", false, PdlIdentGruppe.NPID)

        @Test
        internal fun `skal bruke NPID som gjeldende når personen kun har NPID`() {
            assertThat(PdlIdenter(listOf(npid)).gjeldende()).isEqualTo(npid)
        }

        @Test
        internal fun `skal prioritere folkeregisterident foran NPID`() {
            assertThat(PdlIdenter(listOf(npid, fnr)).gjeldende()).isEqualTo(fnr)
        }

        @Test
        internal fun `skal bruke folkeregisterident når NPID er koblet og historisk`() {
            val identer = PdlIdenter(listOf(npid.copy(historisk = true), fnr))

            assertThat(identer.gjeldende()).isEqualTo(fnr)
            assertThat(identer.identer()).containsExactlyInAnyOrder(fnr.ident, npid.ident)
            assertThat(identer.folkeregisteridenter()).containsExactly(fnr.ident)
        }

        @Test
        internal fun `skal feile når det ikke finnes noen gjeldende ident`() {
            assertThrows<NoSuchElementException> { PdlIdenter(listOf(fnr.copy(historisk = true))).gjeldende() }
        }

        @Test
        internal fun `bolk skal prioritere folkeregisterident foran NPID`() {
            assertThat(PdlIdenterBolk("ok", npid.ident, listOf(npid.copy(historisk = true), fnr)).gjeldende()).isEqualTo(fnr)
            assertThat(PdlIdenterBolk("ok", npid.ident, listOf(npid)).gjeldende()).isEqualTo(npid)
        }
    }
}
