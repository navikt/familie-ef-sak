package no.nav.familie.ef.sak.mapper

import io.mockk.mockk
import no.nav.familie.ef.sak.api.dto.AdresseDto
import no.nav.familie.ef.sak.api.dto.AdresseType
import no.nav.familie.ef.sak.integration.dto.pdl.*
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now

internal class PersonopplysningerMapperTest {

    val personopplysningerMapper = PersonopplysningerMapper(mockk(), mockk(), mockk(), mockk())

    fun adresseOslo() = Vegadresse("1", "ABC", "123", "Oslogata", "01", null, "0101", null, null)
    fun adresseTrondheim() = Vegadresse("1", "ABC", "123", "Trøndergata", "01", null, "7080", null, null)
    fun adresseTromsø() = Vegadresse("1", "ABC", "123", "Tromsøygata", "01", null, "9099", null, null)
    fun adresseTromsøMatrikkel() = Vegadresse("1", "ABC", "123", "Tromsøygata", "01", null, "9099", null, matrikkelId = 123L)
    fun adresseBergen() = Vegadresse("1", "ABC", "123", "Bergensgata", "01", null, "5020", null, null)
    fun matrikkeladresse(matrikkelId: Long? = 123L) = Matrikkeladresse(matrikkelId)


    @Test
    internal fun `forelder og barn bor på samme adresse`() {

        val barnAdresser = listOf(
                lagAdresse(adresseBergen(), now().minusDays(100)),
                lagAdresse(adresseTromsø(), now().minusDays(1)),
                lagAdresse(adresseTrondheim(), null)
        )
        val forelderAdresser = listOf(
                lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100)),
                lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1)),
                lagAdresse(adresseTromsø(), now().minusDays(1))
        )

        val pdlBarn = PdlBarn(emptyList(), barnAdresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn, forelderAdresser)).isTrue
    }

    @Test
    internal fun `forelder og barn med samme vegadresse med matrikkelId`() {

        val barnAdresser = listOf(
                lagAdresse(adresseTromsøMatrikkel(), now().minusDays(1)),
        )
        val forelderAdresser = listOf(
                lagAdresse(adresseTromsøMatrikkel(), now().minusDays(1))
        )

        val pdlBarn = PdlBarn(emptyList(), barnAdresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn, forelderAdresser)).isTrue
    }

    @Test
    internal fun `forelder og barn med samme matrikkeladresse`() {

        val barnAdresser = listOf(lagAdresse(adresseBergen(), now().minusDays(1), null, matrikkeladresse()))
        val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse()))

        val pdlBarn = PdlBarn(emptyList(), barnAdresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn, forelderAdresser)).isTrue
    }

    @Test
    internal fun `forelder og barn med ulik matrikkeladresse`() {

        val barnAdresser = listOf(lagAdresse(adresseBergen(), now().minusDays(1), null, matrikkeladresse()))
        val forelderAdresser = listOf(lagAdresse(null, now().minusDays(1), null, matrikkeladresse(999L)))

        val pdlBarn = PdlBarn(emptyList(), barnAdresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn, forelderAdresser)).isFalse
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


        val pdlBarn1 = PdlBarn(emptyList(), barn1Adresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val pdlBarn2 = PdlBarn(emptyList(), barn2Adresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val pdlBarn3 = PdlBarn(emptyList(), ugyldigeAdresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        val pdlBarn4 = PdlBarn(emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), listOf())

        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn1, forelderAdresser)).isFalse
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn2, forelderAdresser)).isFalse
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn3, forelderAdresser)).isFalse
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn4, forelderAdresser)).isFalse
        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn4, emptyList())).isFalse
    }

    @Test
    internal fun `barn har delt bosted`() {

        val pdlBarnMedDeltBosted = PdlBarn(listOf(),
                                           emptyList(),
                                           listOf(DeltBosted(LocalDateTime.MIN, null, null, null)),
                                           emptyList(),
                                           emptyList(),
                                           emptyList(),
                                           emptyList())
        val forelderAdresser = listOf(lagAdresse(adresseOslo(), now().minusDays(1000), null))

        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarnMedDeltBosted, forelderAdresser)).isFalse
    }

    @Test
    internal fun `forelder og barn bor på samme adresse selv om det ikke finnes gyldighetsdato`() {

        val barnAdresser = listOf(lagAdresse(adresseTromsø(), null), lagAdresse(adresseOslo(), null))
        val forelderAdresser = listOf(
                lagAdresse(adresseOslo(), now().minusDays(1000), now().minusDays(100)),
                lagAdresse(adresseBergen(), now().minusDays(100), now().minusDays(1)),
                lagAdresse(adresseTromsø(), now().minusDays(1))
        )
        val pdlBarn = PdlBarn(emptyList(), barnAdresser, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        Assertions.assertThat(personopplysningerMapper.borPåSammeAdresse(pdlBarn, forelderAdresser)).isTrue()
    }

    @Test
    internal fun `sorter adresser`() {

        val aktivBostedsadresse = lagAdresseDto(AdresseType.BOSTEDADRESSE, LocalDate.now().minusDays(5))
        val historiskBostedsadresse = lagAdresseDto(
                AdresseType.BOSTEDADRESSE, LocalDate.now().minusYears(1), LocalDate.now().minusDays(5))
        val aktivOppholdsadresse = lagAdresseDto(AdresseType.OPPHOLDSADRESSE, LocalDate.now())
        val historiskKontaktadresse = lagAdresseDto(
                AdresseType.KONTAKTADRESSE, LocalDate.now().minusDays(15), LocalDate.now().minusDays(14))

        val adresser = listOf(historiskBostedsadresse, aktivOppholdsadresse, historiskKontaktadresse, aktivBostedsadresse)

        Assertions.assertThat(personopplysningerMapper.sorterAdresser(adresser))
                .containsExactly(aktivBostedsadresse, aktivOppholdsadresse, historiskKontaktadresse, historiskBostedsadresse)
    }

    fun lagAdresse(vegadresse: Vegadresse?,
                   gyldighetstidspunkt: LocalDateTime?,
                   opphørstidspunkt: LocalDateTime? = null,
                   matrikkeladresse: Matrikkeladresse? = null): Bostedsadresse {
        return Bostedsadresse(
                vegadresse = vegadresse,
                angittFlyttedato = null,
                coAdressenavn = null,
                folkeregistermetadata = Folkeregistermetadata(
                        gyldighetstidspunkt = gyldighetstidspunkt,
                        opphørstidspunkt = opphørstidspunkt),
                utenlandskAdresse = null,
                ukjentBosted = null,
                matrikkeladresse = matrikkeladresse
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