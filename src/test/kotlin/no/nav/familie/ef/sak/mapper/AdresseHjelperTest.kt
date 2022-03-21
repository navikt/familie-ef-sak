package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseHjelper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Matrikkeladresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now

internal class AdresseHjelperTest {

    private fun adresseOslo() = Vegadresse("1", "ABC", "123", "Oslogata", "01", null, "0101", null, null)
    private fun adresseTrondheim() = Vegadresse("1", "ABC", "123", "Trøndergata", "01", null, "7080", null, null)
    private fun adresseTromsø() = Vegadresse("1", "ABC", "123", "Tromsøygata", "01", null, "9099", null, null)
    private fun adresseTromsøMatrikkel() = Vegadresse("1", "ABC", "123", "Tromsøygata", "01", null, "9099", null, 123L)
    private fun adresseBergen() = Vegadresse("1", "ABC", "123", "Bergensgata", "01", null, "5020", null, null)
    private fun matrikkeladresse(matrikkelId: Long? = 123L) = Matrikkeladresse(matrikkelId, "H0103", null, null)

    private val metadataGjeldende = Metadata(false)
    private val metadataHistorisk = Metadata(true)

    @Nested
    inner class BorPåSammeAdresse() {

        @Test
        internal fun `forelder og barn bor på samme adresse`() {

            val barnAdresser = listOf(
                    lagAdresse(adresseBergen(), now().minusDays(100), null, null, metadataHistorisk),
                    lagAdresse(adresseTromsø(), now().minusDays(1), null, null, metadataGjeldende),
                    lagAdresse(adresseTrondheim(), null, null, null, metadataHistorisk)
            )
            val forelderAdresser = listOf(
                    lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100), null, metadataHistorisk),
                    lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1), null, metadataHistorisk),
                    lagAdresse(adresseTromsø(), now().minusDays(1), null, null, metadataGjeldende)
            )

            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")
            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `forelder og barn med samme vegadresse med matrikkelId`() {

            val barnAdresser = listOf(
                    lagAdresse(adresseTromsøMatrikkel(), now().minusDays(1)),
            )
            val forelderAdresser = listOf(
                    lagAdresse(adresseTromsøMatrikkel(), now().minusDays(1))
            )

            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")
            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `forelder og barn med samme matrikkeladresse`() {

            val barnAdresser = listOf(lagAdresse(adresseBergen(), now().minusDays(1), null, matrikkeladresse()))
            val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse()))

            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")
            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isTrue
        }

        @Test
        internal fun `returnere false for matrikkeladresser med samme matrikkelId men forskjellig bruksenhetsnummer`() {

            val barnAdresser = listOf(lagAdresse(adresseBergen(),
                                                 now().minusDays(1),
                                                 null,
                                                 matrikkeladresse().copy(bruksenhetsnummer = "H1701")))
            val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse()))

            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")
            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isFalse()
        }

        @Test
        internal fun `returnere false for vegadresser med samme matrikkelId men forskjellig bruksenhetsnummer`() {

            val barnAdresser = listOf(lagAdresse(adresseBergen().copy(matrikkelId = 123, bruksenhetsnummer = "H0103")))
            val forelderAdresser = listOf(lagAdresse(adresseBergen().copy(matrikkelId = 123, bruksenhetsnummer = "H1701")))

            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")
            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isFalse()
        }

        @Test
        internal fun `forelder og barn med ulik matrikkeladresse`() {

            val barnAdresser = listOf(lagAdresse(adresseBergen(), now().minusDays(1), null, matrikkeladresse()))
            val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse(999L)))

            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")
            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isFalse
        }

        @Test
        internal fun `forelder og barn bor ikke på samme adresse`() {

            val barn1Adresser = listOf(
                    lagAdresse(adresseBergen(), now().minusDays(1)),
                    lagAdresse(adresseTromsø(), now().minusDays(100))
            )
            val barn2Adresser = listOf(
                    lagAdresse(adresseBergen(), now().minusDays(1)),
                    lagAdresse(adresseTromsø(), null)
            )

            val ugyldigeAdresser = listOf(
                    lagAdresse(null, null)
            )

            val forelderAdresser = listOf(
                    lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100)),
                    lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1)),
                    lagAdresse(adresseTromsø(), now().minusDays(1))
            )


            val barn1 = BarnMedIdent(emptyList(),
                                     barn1Adresser,
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     Navn("", "", "", Metadata(false)),
                                     "")
            val barn2 = BarnMedIdent(emptyList(),
                                     barn2Adresser,
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     Navn("", "", "", Metadata(false)),
                                     "")
            val barn3 =
                    BarnMedIdent(emptyList(),
                                 ugyldigeAdresser,
                                 emptyList(),
                                 emptyList(),
                                 emptyList(),
                                 emptyList(),
                                 Navn("", "", "", Metadata(false)),
                                 "")
            val barn4 = BarnMedIdent(emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     emptyList(),
                                     Navn("", "", "", Metadata(false)),
                                     "")

            assertThat(AdresseHjelper.borPåSammeAdresse(barn1, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.borPåSammeAdresse(barn2, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.borPåSammeAdresse(barn3, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.borPåSammeAdresse(barn4, forelderAdresser)).isFalse
            assertThat(AdresseHjelper.borPåSammeAdresse(barn4, emptyList())).isFalse
        }

        @Test
        internal fun `barn har delt bosted`() {

            val barnMedDeltBosted = BarnMedIdent(listOf(),
                                                 emptyList(),
                                                 listOf(DeltBosted(LocalDate.MIN, null, null, null, metadataGjeldende)),
                                                 emptyList(),
                                                 emptyList(),
                                                 emptyList(),
                                                 Navn("", "", "", Metadata(false)),
                                                 "")
            val forelderAdresser = listOf(lagAdresse(adresseOslo(), now().minusDays(1000), null))

            assertThat(AdresseHjelper.borPåSammeAdresse(barnMedDeltBosted, forelderAdresser)).isFalse
        }

        @Test
        internal fun `forelder og barn bor på samme adresse selv om det ikke finnes gyldighetsdato`() {

            val barnAdresser = listOf(lagAdresse(vegadresse = adresseTromsø(), metadata = metadataGjeldende),
                                      lagAdresse(vegadresse = adresseOslo(), metadata = metadataHistorisk))
            val forelderAdresser = listOf(
                    lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100), null, metadataHistorisk),
                    lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1), null, metadataHistorisk),
                    lagAdresse(adresseTromsø(), now().minusDays(1), null, null, metadataGjeldende)
            )
            val barn = BarnMedIdent(emptyList(),
                                    barnAdresser,
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    emptyList(),
                                    Navn("", "", "", Metadata(false)),
                                    "")

            assertThat(AdresseHjelper.borPåSammeAdresse(barn, forelderAdresser)).isTrue
        }
    }

    @Nested
    inner class SorterAdresser {

        @Test
        internal fun `sortering av adresser skal sortere de per type først, og sen per startdato`() {

            val aktivBostedsadresse = lagAdresseDto(AdresseType.BOSTEDADRESSE, now().minusDays(5))
            val historiskBostedsadresse = lagAdresseDto(
                    AdresseType.BOSTEDADRESSE, now().minusYears(1), now().minusDays(5))
            val aktivOppholdsadresse = lagAdresseDto(AdresseType.OPPHOLDSADRESSE, now())
            val historiskKontaktadresse = lagAdresseDto(
                    AdresseType.KONTAKTADRESSE, now().minusDays(15), now().minusDays(14))
            val historiskKontaktadresseUtland = lagAdresseDto(AdresseType.KONTAKTADRESSE_UTLAND, now(), now())

            val adresser = listOf(historiskBostedsadresse,
                                  historiskKontaktadresseUtland,
                                  aktivOppholdsadresse,
                                  historiskKontaktadresse,
                                  aktivBostedsadresse)
            assertThat(AdresseHjelper.sorterAdresser(adresser))
                    .containsExactly(aktivBostedsadresse,
                                     historiskBostedsadresse,
                                     aktivOppholdsadresse,
                                     historiskKontaktadresse,
                                     historiskKontaktadresseUtland)
        }

    }

    private fun lagAdresse(vegadresse: Vegadresse?,
                           gyldighetstidspunkt: LocalDate? = null,
                           opphørstidspunkt: LocalDate? = null,
                           matrikkeladresse: Matrikkeladresse? = null,
                           metadata: Metadata? = null): Bostedsadresse {
        return Bostedsadresse(
                vegadresse = vegadresse,
                angittFlyttedato = null,
                gyldigFraOgMed = gyldighetstidspunkt,
                gyldigTilOgMed = opphørstidspunkt,
                coAdressenavn = null,
                utenlandskAdresse = null,
                ukjentBosted = null,
                matrikkeladresse = matrikkeladresse,
                metadata = metadata ?: metadataGjeldende
        )
    }

    private fun lagAdresseDto(type: AdresseType,
                              gyldigFraOgMed: LocalDate?,
                              gyldigTilOgMed: LocalDate? = null): AdresseDto {
        return AdresseDto(
                visningsadresse = "Oslogata 1",
                type = type,
                gyldigFraOgMed = gyldigFraOgMed,
                gyldigTilOgMed = gyldigTilOgMed
        )
    }
}