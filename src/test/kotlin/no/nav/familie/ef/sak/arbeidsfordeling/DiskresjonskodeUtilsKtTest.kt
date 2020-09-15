package no.nav.familie.ef.sak.arbeidsfordeling

import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DiskresjonskodeUtilsKtTest {

    private val strengtFortrolig = IdentMedAdressebeskyttelse("1", AdressebeskyttelseGradering.STRENGT_FORTROLIG)
    private val fortrolig = IdentMedAdressebeskyttelse("2", AdressebeskyttelseGradering.FORTROLIG)
    private val ugradert = IdentMedAdressebeskyttelse("3", AdressebeskyttelseGradering.UGRADERT)
    private val savner = IdentMedAdressebeskyttelse("4", null)

    @Test
    internal fun `finnPersonMedStrengesteAdressebeskyttelse - strengt fortrolig er høyest gradert`() {
        verifiserPersonMedStrengesteAdressebeskyttelse("1", strengtFortrolig)
        verifiserPersonMedStrengesteAdressebeskyttelse("1", strengtFortrolig, fortrolig, savner)
        verifiserPersonMedStrengesteAdressebeskyttelse("1", fortrolig, savner, strengtFortrolig, ugradert)
        verifiserPersonMedStrengesteAdressebeskyttelse("1", fortrolig, savner, savner, strengtFortrolig)
    }

    @Test
    internal fun `finnPersonMedStrengesteAdressebeskyttelse - strengt fortrolig er nest høyest gradert`() {
        verifiserPersonMedStrengesteAdressebeskyttelse("2", fortrolig)
        verifiserPersonMedStrengesteAdressebeskyttelse("2", fortrolig, savner)
        verifiserPersonMedStrengesteAdressebeskyttelse("2", fortrolig, savner, ugradert)
        verifiserPersonMedStrengesteAdressebeskyttelse("2", savner, savner, fortrolig)
    }

    @Test
    internal fun `finnPersonMedStrengesteAdressebeskyttelse - ugradert`() {
        verifiserPersonMedStrengesteAdressebeskyttelse(null, ugradert, savner)
        verifiserPersonMedStrengesteAdressebeskyttelse(null, savner, ugradert)
        verifiserPersonMedStrengesteAdressebeskyttelse(null, savner)
        verifiserPersonMedStrengesteAdressebeskyttelse(null, ugradert)
    }

    private fun verifiserPersonMedStrengesteAdressebeskyttelse(forventetIdent: String?, vararg identer: IdentMedAdressebeskyttelse) {
        assertThat(finnPersonMedStrengesteAdressebeskyttelse(identer.toList())).isEqualTo(forventetIdent)
    }
}