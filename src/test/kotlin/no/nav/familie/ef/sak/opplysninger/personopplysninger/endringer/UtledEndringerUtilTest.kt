package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AnnenForelderMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.OppholdType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.OppholdstillatelseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.SivilstandDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.VergemålDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.UtledEndringerUtil.finnEndringer
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.full.memberProperties

internal class UtledEndringerUtilTest {

    @Test
    internal fun jsontest() {
        val barnBlirFjernet = BarnDto("barnBlirFjernet", "2", null, emptyList(), true, null, null)
        val annenForelder = AnnenForelderMinimumDto("1", "", null)
        val barnsForelderBlirEndret =
            BarnDto("forelderFårEndring", "2", annenForelder, emptyList(), true, null, null)
        val barnFårEndring = BarnDto("barnFårEndretBorHosSøker", "2", null, emptyList(), true, null, null)
        val nyttBarn = BarnDto("nyttBarn", "2", null, emptyList(), true, null, null)
        val tidligere = dto(
            dødsdato = LocalDate.of(2021, 1, 1),
            barn = listOf(barnBlirFjernet, barnsForelderBlirEndret, barnFårEndring)
        )
        val nye = dto(
            status = Folkeregisterpersonstatus.UKJENT,
            fødselsdato = LocalDate.of(2000, 12, 14),
            dødsdato = LocalDate.of(2021, 1, 2),
            statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null)),
            sivilstand = listOf(SivilstandDto(Sivilstandstype.GIFT, null, null, null, null, true)),
            adresse = listOf(AdresseDto("2", AdresseType.BOSTEDADRESSE, null, null, null, true)),
            fullmakt = listOf(FullmaktDto(LocalDate.now(), LocalDate.now(), "1", null, emptyList())),
            barn = listOf(
                barnsForelderBlirEndret.copy(annenForelder = annenForelder.copy(dødsdato = LocalDate.of(2022, 1, 1))),
                barnFårEndring.copy(
                    borHosSøker = false,
                    fødselsdato = LocalDate.of(2020, 1, 1),
                    dødsdato = LocalDate.of(2021, 1, 1)
                ),
                nyttBarn
            ),
            innflyttingTilNorge = listOf(InnflyttingDto("", null, null)),
            utflyttingFraNorge = listOf(UtflyttingDto(null, null, null)),
            oppholdstillatelse = listOf(OppholdstillatelseDto(OppholdType.UKJENT, null, null)),
            vergemål = listOf(VergemålDto(null, null, null, null, null))
        )
        val endringer = finnEndringer(tidligere, nye)
        val expected =
            this::class.java.classLoader.getResource("json/endringer-personopplysninger/endringer.json")!!.readText()
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(endringer)).isEqualTo(expected)
    }

    @Nested
    inner class UlikeTyperAsserts {

        @Test
        internal fun `har ingen endringer`() {
            val endringer = finnEndringer(dto(), dto())
            assertThat(endringer.harEndringer).isFalse

            assertIngenAndreEndringer(endringer)
        }

        @Test
        internal fun `folkeregisterpersonstatus endret fra mangler verdi til annet verdi`() {
            val endringer = finnEndringer(dto(status = null), dto(status = Folkeregisterpersonstatus.BOSATT))

            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.detaljer!!.tidligere).isEqualTo("Mangler verdi")
            assertThat(endringer.folkeregisterpersonstatus.detaljer!!.ny).isEqualTo("Bosatt")
            assertIngenAndreEndringer(endringer, "folkeregisterpersonstatus")
        }

        @Test
        internal fun `folkeregisterpersonstatus endret fra et verdi til annet verdi`() {
            val endringer = finnEndringer(
                dto(status = Folkeregisterpersonstatus.DØD),
                dto(status = Folkeregisterpersonstatus.BOSATT)
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.detaljer!!.tidligere).isEqualTo("Død")
            assertThat(endringer.folkeregisterpersonstatus.detaljer!!.ny).isEqualTo("Bosatt")
        }

        @Test
        internal fun `2 tomme lister`() {
            val endringer = finnEndringer(dto(statsborgerskap = listOf()), dto(statsborgerskap = listOf()))
            assertThat(endringer.harEndringer).isFalse
            assertThat(endringer.statsborgerskap.harEndringer).isFalse
            assertIngenAndreEndringer(endringer)
        }

        @Test
        internal fun `2 lister med de liknende data`() {
            val endringer = finnEndringer(
                dto(statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null))),
                dto(statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null)))
            )
            assertThat(endringer.harEndringer).isFalse
            assertThat(endringer.statsborgerskap.harEndringer).isFalse
            assertIngenAndreEndringer(endringer)
        }
    }

    @Nested
    inner class FeltNivå {

        @Test
        internal fun fødselsdato() {
            val tidligere = LocalDate.now()
            val ny = LocalDate.now().minusDays(1)
            val endringer = finnEndringer(dto(fødselsdato = tidligere), dto(fødselsdato = ny))
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.fødselsdato.harEndringer).isTrue
            assertThat(endringer.fødselsdato.detaljer!!.tidligere).isEqualTo(tidligere.norskFormat())
            assertThat(endringer.fødselsdato.detaljer!!.ny).isEqualTo(ny.norskFormat())
            assertIngenAndreEndringer(endringer, "fødselsdato")
        }

        @Test
        internal fun dødsdato() {
            val tidligere = LocalDate.now()
            val ny = LocalDate.now().minusDays(1)
            val endringer = finnEndringer(dto(dødsdato = tidligere), dto(dødsdato = ny))
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.dødsdato.harEndringer).isTrue
            assertThat(endringer.dødsdato.detaljer!!.tidligere).isEqualTo(tidligere.norskFormat())
            assertThat(endringer.dødsdato.detaljer!!.ny).isEqualTo(ny.norskFormat())
            assertIngenAndreEndringer(endringer, "dødsdato")
        }

        @Test
        internal fun statsborgerskap() {
            val endringer = finnEndringer(
                dto(statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null))),
                dto(statsborgerskap = listOf(StatsborgerskapDto("land 2", null, null)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.statsborgerskap.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "statsborgerskap")
        }

        @Test
        internal fun sivilstand() {
            val endringer = finnEndringer(
                dto(sivilstand = listOf(SivilstandDto(Sivilstandstype.UGIFT, null, null, null, null, true))),
                dto(sivilstand = listOf(SivilstandDto(Sivilstandstype.GIFT, null, null, null, null, true)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.sivilstand.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "sivilstand")
        }

        @Test
        internal fun adresse() {
            val endringer = finnEndringer(
                dto(adresse = listOf(AdresseDto("1", AdresseType.BOSTEDADRESSE, null, null, null, true))),
                dto(adresse = listOf(AdresseDto("2", AdresseType.BOSTEDADRESSE, null, null, null, true)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.adresse.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "adresse")
        }

        @Test
        internal fun fullmakt() {
            val fullmaktDto = FullmaktDto(LocalDate.now(), LocalDate.now(), "1", null, emptyList())
            val endringer = finnEndringer(
                dto(fullmakt = listOf(fullmaktDto)),
                dto(fullmakt = listOf(fullmaktDto.copy(motpartsPersonident = "2")))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.fullmakt.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "fullmakt")
        }

        @Test
        internal fun innflyttingTilNorge() {
            val endringer = finnEndringer(
                dto(innflyttingTilNorge = listOf(InnflyttingDto(null, null, null))),
                dto(innflyttingTilNorge = listOf(InnflyttingDto("", null, null)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.innflyttingTilNorge.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "innflyttingTilNorge")
        }

        @Test
        internal fun utflyttingFraNorge() {
            val endringer = finnEndringer(
                dto(utflyttingFraNorge = listOf(UtflyttingDto(null, null, null))),
                dto(utflyttingFraNorge = listOf(UtflyttingDto("", null, null)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.utflyttingFraNorge.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "utflyttingFraNorge")
        }

        @Test
        internal fun oppholdstillatelse() {
            val endringer = finnEndringer(
                dto(oppholdstillatelse = listOf()),
                dto(oppholdstillatelse = listOf(OppholdstillatelseDto(OppholdType.UKJENT, null, null)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.oppholdstillatelse.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "oppholdstillatelse")
        }

        @Test
        internal fun vergemål() {
            val endringer = finnEndringer(
                dto(vergemål = listOf()),
                dto(vergemål = listOf(VergemålDto(null, null, null, null, null)))
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.vergemål.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "vergemål")
        }
    }

    @Nested
    inner class Personeendringer {
        @Test
        internal fun `nytt barn`() {
            val barn = BarnDto("ident", "", null, emptyList(), true, null, null)
            val endringer = finnEndringer(
                dto(barn = listOf()),
                dto(barn = listOf(barn))
            )
            assertThat(endringer.harEndringer).isTrue
            assertNyPerson(endringer.barn, "ident")

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `fjernet barn`() {
            val barn = BarnDto("ident", "", null, emptyList(), true, null, null)
            val endringer = finnEndringer(
                dto(barn = listOf(barn)),
                dto(barn = listOf())
            )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.barn.harEndringer).isTrue
            val detaljer = endringer.barn.detaljer!!
            assertThat(detaljer).hasSize(1)
            assertThat(detaljer[0].ident).isEqualTo("ident")
            assertThat(detaljer[0].ny).isFalse
            assertThat(detaljer[0].fjernet).isTrue
            assertThat(detaljer[0].endringer).isEmpty()

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `endring på navn trigger ikke endring`() {
            val barn = BarnDto("ident", "", null, emptyList(), true, null, null)
            val barn2 = BarnDto("ident", "2", null, emptyList(), true, null, null)
            val endringer = finnEndringer(
                dto(barn = listOf(barn)),
                dto(barn = listOf(barn2))
            )
            assertThat(endringer.harEndringer).isFalse
            assertThat(endringer.barn.harEndringer).isFalse
            val detaljer = endringer.barn.detaljer!!
            assertThat(detaljer).isEmpty()

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `endring bor hos søker trigger endring`() {
            val barn = BarnDto("ident", "", null, emptyList(), true, null, null)
            val barn2 = barn.copy(borHosSøker = false)
            val endringer = finnEndringer(
                dto(barn = listOf(barn)),
                dto(barn = listOf(barn2))
            )
            val detaljer = endringer.barn.detaljer!!
            assertBarnHarEndringerMedDetaljer(endringer)

            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Bor hos søker")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Ja")
            assertThat(endringsdetaljer[0].ny).isEqualTo("Nei")

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `endring dødsdato på annen forelder trigger endring både på barn og annen forelder`() {
            val dødsdato = LocalDate.now()
            val barn = BarnDto("ident", "", null, emptyList(), true, null, null)
            val barn2 = barn.copy(dødsdato = dødsdato)
            val endringer = finnEndringer(
                dto(barn = listOf(barn)),
                dto(barn = listOf(barn2))
            )
            val detaljer = endringer.barn.detaljer!!
            assertBarnHarEndringerMedDetaljer(endringer)

            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Dødsdato")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Mangler verdi")
            assertThat(endringsdetaljer[0].ny).isEqualTo(dødsdato.norskFormat())

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `endring av personident på annen forelder trigger endring både på barn og annen forelder`() {
            val barn = BarnDto("ident", "", null, emptyList(), true, null, null)
            val barn2 = BarnDto("ident", "2", AnnenForelderMinimumDto("1", "", null), emptyList(), true, null, null)
            val endringer = finnEndringer(
                dto(barn = listOf(barn)),
                dto(barn = listOf(barn2))
            )
            assertBarnHarEndringerMedDetaljer(endringer)

            val detaljer = endringer.barn.detaljer!!
            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Annen forelder")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Mangler verdi")
            assertThat(endringsdetaljer[0].ny).isEqualTo("1")

            assertNyPerson(endringer.annenForelder, "1")

            assertIngenAndreEndringer(endringer, "barn", "annenForelder")
        }

        @Test
        internal fun `dødsdato på annen forelder skal kun trigge endring på annen forelder`() {
            val annenForelder = AnnenForelderMinimumDto("1", "", null)
            val dødsdato = LocalDate.now()
            val barn = BarnDto("ident", "", annenForelder, emptyList(), true, null, null)
            val barn2 = barn.copy(annenForelder = annenForelder.copy(dødsdato = dødsdato))
            val endringer = finnEndringer(
                dto(barn = listOf(barn)),
                dto(barn = listOf(barn2))
            )
            assertThat(endringer.barn.harEndringer).isFalse

            assertThat(endringer.annenForelder.harEndringer).isTrue
            val detaljer = endringer.annenForelder.detaljer!!
            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Dødsdato")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Mangler verdi")
            assertThat(endringsdetaljer[0].ny).isEqualTo(dødsdato.norskFormat())

            assertIngenAndreEndringer(endringer, "annenForelder")
        }
    }

    private fun assertNyPerson(personendring: Endring<List<Personendring>>, ident: String) {
        assertThat(personendring.harEndringer).isTrue
        val detaljer = personendring.detaljer!!
        assertThat(detaljer).hasSize(1)
        assertThat(detaljer[0].ident).isEqualTo(ident)
        assertThat(detaljer[0].ny).isTrue
        assertThat(detaljer[0].fjernet).isFalse
        assertThat(detaljer[0].endringer).isEmpty()
    }

    private fun assertBarnHarEndringerMedDetaljer(endringer: Endringer) {
        assertThat(endringer.harEndringer).isTrue
        assertThat(endringer.barn.harEndringer).isTrue

        val detaljer = endringer.barn.detaljer!!
        assertThat(detaljer).hasSize(1)
        assertThat(detaljer[0].ident).isEqualTo("ident")
        assertThat(detaljer[0].ny).isFalse
        assertThat(detaljer[0].fjernet).isFalse
    }

    private fun assertIngenAndreEndringer(endringer: Endringer, vararg ignorerFelt: String) {
        Endringer::class.memberProperties
            .filter { it.name != "harEndringer" && !ignorerFelt.toSet().contains(it.name) }
            .forEach {
                try {
                    val endring = it.get(endringer) as Endring<*>
                    assertThat(endring.harEndringer).isFalse
                    val detaljer = endring.detaljer
                    if (detaljer is List<*>) {
                        assertThat(detaljer).isEmpty()
                    } else {
                        assertThat(detaljer).isNull()
                    }
                } catch (e: Throwable) {
                    throw RuntimeException("Feilet ${it.name}", e)
                }
            }
    }

    private fun dto(
        status: Folkeregisterpersonstatus? = null,
        fødselsdato: LocalDate? = null,
        dødsdato: LocalDate? = null,
        statsborgerskap: List<StatsborgerskapDto> = emptyList(),
        sivilstand: List<SivilstandDto> = emptyList(),
        adresse: List<AdresseDto> = emptyList(),
        fullmakt: List<FullmaktDto> = emptyList(),
        barn: List<BarnDto> = emptyList(),
        innflyttingTilNorge: List<InnflyttingDto> = emptyList(),
        utflyttingFraNorge: List<UtflyttingDto> = emptyList(),
        oppholdstillatelse: List<OppholdstillatelseDto> = emptyList(),
        vergemål: List<VergemålDto> = emptyList()
    ) = PersonopplysningerDto(
        personIdent = "",
        navn = NavnDto("", "", "", ""),
        kjønn = Kjønn.MANN,
        adressebeskyttelse = null,
        folkeregisterpersonstatus = status,
        fødselsdato = fødselsdato,
        dødsdato = dødsdato,
        telefonnummer = null,
        statsborgerskap = statsborgerskap,
        sivilstand = sivilstand,
        adresse = adresse,
        fullmakt = fullmakt,
        egenAnsatt = false,
        navEnhet = "",
        barn = barn,
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
        oppholdstillatelse = oppholdstillatelse,
        vergemål = vergemål
    )
}
