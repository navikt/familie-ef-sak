package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikkOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AnnenForelderMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.DeltBostedDto
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
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.UtledEndringerUtil.finnEndringerIPerioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.UtledEndringerUtil.finnEndringerIPersonopplysninger
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.memberProperties

internal class UtledEndringerUtilTest {
    val barnIdent = "ident"
    val forelderIdent = "forelderIdent"

    @Test
    internal fun jsontest() {
        val barnBlirFjernet = BarnDto("barnBlirFjernet", "2", null, emptyList(), true, emptyList(), false, null, null)
        val annenForelder = AnnenForelderMinimumDto("1", "", null, "Adresse 1")
        val annenForelder2 = AnnenForelderMinimumDto("2", "", null, null)
        val barnsForelderBlirEndret =
            BarnDto("forelderFårEndring", "2", annenForelder, emptyList(), true, emptyList(), false, null, null)
        val barnFårEndring =
            BarnDto("barnFårEndretBorHosSøker", "2", null, emptyList(), true, emptyList(), false, null, null)
        val barnFårForelder = BarnDto("barnFårForelder", "2", null, emptyList(), true, emptyList(), false, null, null)
        val nyttBarn = BarnDto("nyttBarn", "2", null, emptyList(), true, emptyList(), false, null, null)
        val tidligere =
            dto(
                dødsdato = LocalDate.of(2021, 1, 1),
                barn = listOf(barnBlirFjernet, barnsForelderBlirEndret, barnFårForelder, barnFårEndring),
            )
        val nye =
            dto(
                status = Folkeregisterpersonstatus.UKJENT,
                fødselsdato = LocalDate.of(2000, 12, 14),
                dødsdato = LocalDate.of(2021, 1, 2),
                statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null)),
                sivilstand = listOf(SivilstandDto(Sivilstandstype.GIFT, null, null, null, null, true)),
                adresse = listOf(AdresseDto("2", AdresseType.BOSTEDADRESSE, null, null, null, true)),
                fullmakt = listOf(FullmaktDto(LocalDate.now(), LocalDate.now(), "1", null, emptyList())),
                barn =
                    listOf(
                        barnsForelderBlirEndret.copy(
                            annenForelder =
                                annenForelder.copy(
                                    dødsdato = LocalDate.of(2022, 1, 1),
                                    bostedsadresse = "Annen adresse",
                                ),
                        ),
                        barnFårForelder.copy(annenForelder = annenForelder2),
                        barnFårEndring.copy(
                            borHosSøker = false,
                            fødselsdato = LocalDate.of(2020, 1, 1),
                            dødsdato = LocalDate.of(2021, 1, 1),
                        ),
                        nyttBarn,
                    ),
                innflyttingTilNorge = listOf(InnflyttingDto("", null, null)),
                utflyttingFraNorge = listOf(UtflyttingDto(null, null, null)),
                oppholdstillatelse = listOf(OppholdstillatelseDto(OppholdType.UKJENT, null, null)),
                vergemål = listOf(VergemålDto(null, null, null, null, null)),
            )
        val endringer = finnEndringerIPersonopplysninger(tidligere, nye)
        val expected =
            this::class.java.classLoader
                .getResource("json/endringer-personopplysninger/endringer.json")!!
                .readText()
        assertThat(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(endringer)).isEqualTo(expected)
    }

    @Nested
    inner class UlikeTyperAsserts {
        @Test
        internal fun `har ingen endringer`() {
            val endringer = finnEndringerIPersonopplysninger(dto(), dto())
            assertThat(endringer.harEndringer).isFalse

            assertIngenAndreEndringer(endringer)
        }

        @Test
        internal fun `folkeregisterpersonstatus endret fra mangler verdi til annet verdi`() {
            val endringer = finnEndringerIPersonopplysninger(dto(status = null), dto(status = Folkeregisterpersonstatus.BOSATT))

            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.detaljer!!.tidligere).isEqualTo("Mangler verdi")
            assertThat(endringer.folkeregisterpersonstatus.detaljer.ny).isEqualTo("Bosatt")
            assertIngenAndreEndringer(endringer, "folkeregisterpersonstatus")
        }

        @Test
        internal fun `folkeregisterpersonstatus endret fra et verdi til annet verdi`() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(status = Folkeregisterpersonstatus.DØD),
                    dto(status = Folkeregisterpersonstatus.BOSATT),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.harEndringer).isTrue
            assertThat(endringer.folkeregisterpersonstatus.detaljer!!.tidligere).isEqualTo("Død")
            assertThat(endringer.folkeregisterpersonstatus.detaljer.ny).isEqualTo("Bosatt")
        }

        @Test
        internal fun `2 tomme lister`() {
            val endringer = finnEndringerIPersonopplysninger(dto(statsborgerskap = listOf()), dto(statsborgerskap = listOf()))
            assertThat(endringer.harEndringer).isFalse
            assertThat(endringer.statsborgerskap.harEndringer).isFalse
            assertIngenAndreEndringer(endringer)
        }

        @Test
        internal fun `2 lister med de liknende data`() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null))),
                    dto(statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null))),
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
            val endringer = finnEndringerIPersonopplysninger(dto(fødselsdato = tidligere), dto(fødselsdato = ny))
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.fødselsdato.harEndringer).isTrue
            assertThat(endringer.fødselsdato.detaljer!!.tidligere).isEqualTo(tidligere.norskFormat())
            assertThat(endringer.fødselsdato.detaljer.ny).isEqualTo(ny.norskFormat())
            assertIngenAndreEndringer(endringer, "fødselsdato")
        }

        @Test
        internal fun dødsdato() {
            val tidligere = LocalDate.now()
            val ny = LocalDate.now().minusDays(1)
            val endringer = finnEndringerIPersonopplysninger(dto(dødsdato = tidligere), dto(dødsdato = ny))
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.dødsdato.harEndringer).isTrue
            assertThat(endringer.dødsdato.detaljer!!.tidligere).isEqualTo(tidligere.norskFormat())
            assertThat(endringer.dødsdato.detaljer.ny).isEqualTo(ny.norskFormat())
            assertIngenAndreEndringer(endringer, "dødsdato")
        }

        @Test
        internal fun statsborgerskap() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(statsborgerskap = listOf(StatsborgerskapDto("land 1", null, null))),
                    dto(statsborgerskap = listOf(StatsborgerskapDto("land 2", null, null))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.statsborgerskap.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "statsborgerskap")
        }

        @Test
        internal fun sivilstand() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(sivilstand = listOf(SivilstandDto(Sivilstandstype.UGIFT, null, null, null, null, true))),
                    dto(sivilstand = listOf(SivilstandDto(Sivilstandstype.GIFT, null, null, null, null, true))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.sivilstand.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "sivilstand")
        }

        @Test
        internal fun adresse() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(adresse = listOf(AdresseDto("1", AdresseType.BOSTEDADRESSE, null, null, null, true))),
                    dto(adresse = listOf(AdresseDto("2", AdresseType.BOSTEDADRESSE, null, null, null, true))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.adresse.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "adresse")
        }

        @Test
        internal fun fullmakt() {
            val fullmaktDto = FullmaktDto(LocalDate.now(), LocalDate.now(), "1", null, emptyList())
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(fullmakt = listOf(fullmaktDto)),
                    dto(fullmakt = listOf(fullmaktDto.copy(motpartsPersonident = "2"))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.fullmakt.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "fullmakt")
        }

        @Test
        internal fun innflyttingTilNorge() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(innflyttingTilNorge = listOf(InnflyttingDto(null, null, null))),
                    dto(innflyttingTilNorge = listOf(InnflyttingDto("", null, null))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.innflyttingTilNorge.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "innflyttingTilNorge")
        }

        @Test
        internal fun utflyttingFraNorge() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(utflyttingFraNorge = listOf(UtflyttingDto(null, null, null))),
                    dto(utflyttingFraNorge = listOf(UtflyttingDto("", null, null))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.utflyttingFraNorge.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "utflyttingFraNorge")
        }

        @Test
        internal fun oppholdstillatelse() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(oppholdstillatelse = listOf()),
                    dto(oppholdstillatelse = listOf(OppholdstillatelseDto(OppholdType.UKJENT, null, null))),
                )
            assertThat(endringer.harEndringer).isTrue
            assertThat(endringer.oppholdstillatelse.harEndringer).isTrue
            assertIngenAndreEndringer(endringer, "oppholdstillatelse")
        }

        @Test
        internal fun vergemål() {
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(vergemål = listOf()),
                    dto(vergemål = listOf(VergemålDto(null, null, null, null, null))),
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
            val barn = BarnDto(barnIdent, "", null, emptyList(), true, emptyList(), false, null, null)
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf()),
                    dto(barn = listOf(barn)),
                )
            assertThat(endringer.harEndringer).isTrue
            assertNyPerson(endringer.barn, barnIdent)

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `fjernet barn`() {
            val barn = BarnDto("ident", "", null, emptyList(), true, emptyList(), false, null, null)
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf()),
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
            val barn = BarnDto(barnIdent, "", null, emptyList(), true, emptyList(), false, null, null)
            val barn2 = BarnDto(barnIdent, "2", null, emptyList(), true, emptyList(), false, null, null)
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
                )
            assertThat(endringer.harEndringer).isFalse
            assertThat(endringer.barn.harEndringer).isFalse
            val detaljer = endringer.barn.detaljer!!
            assertThat(detaljer).isEmpty()

            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `endring bor hos søker trigger endring`() {
            val barn = BarnDto(barnIdent, "", null, emptyList(), true, emptyList(), false, null, null)
            val barn2 = barn.copy(borHosSøker = false)
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
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
        internal fun `endring dødsdato på barn trigger kun endring på barn`() {
            val dødsdato = LocalDate.now()
            val annenForelder = AnnenForelderMinimumDto(forelderIdent, "Navn", null, null)
            val barn = BarnDto(barnIdent, "", annenForelder, emptyList(), true, emptyList(), false, null, null)
            val barn2 = barn.copy(dødsdato = dødsdato)
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
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
            val barn = BarnDto(barnIdent, "", null, emptyList(), true, emptyList(), false, null, null)
            val barn2 =
                BarnDto(
                    barnIdent,
                    "2",
                    AnnenForelderMinimumDto("1", "", null, null),
                    emptyList(),
                    true,
                    emptyList(),
                    false,
                    null,
                    null,
                )
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
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
            val annenForelder = AnnenForelderMinimumDto(forelderIdent, "", null, null)
            val dødsdato = LocalDate.now()
            val barn = BarnDto("ident", "", annenForelder, emptyList(), true, emptyList(), false, null, null)
            val barn2 = barn.copy(annenForelder = annenForelder.copy(dødsdato = dødsdato))
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
                )
            assertForelderHarEndringerMedDetaljer(endringer)

            val detaljer = endringer.annenForelder.detaljer!!
            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Dødsdato")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Mangler verdi")
            assertThat(endringsdetaljer[0].ny).isEqualTo(dødsdato.norskFormat())

            assertIngenAndreEndringer(endringer, "annenForelder")
        }

        @Test
        internal fun `endring bostedsadresse på annen forelder trigger endring på annen forelder`() {
            val annenForelder = AnnenForelderMinimumDto(forelderIdent, "Navn", null, "Adresse 1")
            val barn = BarnDto(barnIdent, "", annenForelder, emptyList(), true, emptyList(), false, null, null)
            val barn2 = barn.copy(annenForelder = annenForelder.copy(bostedsadresse = "Adresse 2"))
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
                )
            assertThat(endringer.barn.harEndringer).isFalse
            val detaljer = endringer.annenForelder.detaljer!!
            assertForelderHarEndringerMedDetaljer(endringer)

            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Bostedsadresse")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Adresse 1")
            assertThat(endringsdetaljer[0].ny).isEqualTo("Adresse 2")

            assertIngenAndreEndringer(endringer, "annenForelder")
        }

        @Test
        internal fun `søkers barn får delt bosted trigger endring`() {
            val barn = BarnDto(barnIdent, "", null, emptyList(), true, emptyList(), false, null, null)
            val barn2 = barn.copy(harDeltBostedNå = true)
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
                )
            val detaljer = endringer.barn.detaljer!!
            assertBarnHarEndringerMedDetaljer(endringer)
            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Delt bosted")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("Nei")
            assertThat(endringsdetaljer[0].ny).isEqualTo("Ja")
            assertIngenAndreEndringer(endringer, "barn")
        }

        @Test
        internal fun `søkers barn får ny, utvidet, periode i delt bosted - trigger endring`() {
            val startdatoForOpprinneligKontrakt = LocalDate.now().minusYears(1)
            val sluttdatoForOpprinneligKontrakt = LocalDate.now().plusYears(1)
            val deltBostedGammel =
                DeltBostedDto(
                    startdatoForKontrakt = startdatoForOpprinneligKontrakt,
                    sluttdatoForKontrakt = sluttdatoForOpprinneligKontrakt,
                    historisk = true,
                )
            val deltBostedNy =
                DeltBostedDto(
                    startdatoForKontrakt = sluttdatoForOpprinneligKontrakt,
                    sluttdatoForKontrakt = sluttdatoForOpprinneligKontrakt.plusYears(5),
                    historisk = false,
                )
            val barn =
                BarnDto(
                    barnIdent,
                    "",
                    null,
                    emptyList(),
                    true,
                    listOf(deltBostedGammel),
                    true,
                    null,
                    null,
                )
            val barn2 = barn.copy(deltBosted = listOf(deltBostedGammel, deltBostedNy))
            val endringer =
                finnEndringerIPersonopplysninger(
                    dto(barn = listOf(barn)),
                    dto(barn = listOf(barn2)),
                )
            val detaljer = endringer.barn.detaljer!!
            assertBarnHarEndringerMedDetaljer(endringer)
            val endringsdetaljer = detaljer[0].endringer
            assertThat(endringsdetaljer).hasSize(1)
            assertThat(endringsdetaljer[0].felt).isEqualTo("Delt bosted perioder")
            assertIngenAndreEndringer(endringer, "barn")
            assertThat(endringsdetaljer[0].ny).isEqualTo("")
            assertThat(endringsdetaljer[0].tidligere).isEqualTo("")
        }
    }

    @Test
    internal fun `Finner endring i perioder hvis det er endringer`() {
        val tidligereGrunnlagsdata = grunnlagsdataMedMetadata()
        val nyGrunnlagsdata = GrunnlagsdataMedMetadata(opprettGrunnlagsdata(tidligereVedtaksperioder = tidligereVedtaksperioder()), LocalDateTime.now())
        val endringerIPerioder = finnEndringerIPerioder(tidligereGrunnlagsdata = tidligereGrunnlagsdata, nyGrunnlagsdata = nyGrunnlagsdata)
        assertThat(endringerIPerioder.harEndringer).isTrue()
    }

    @Test
    internal fun `Finner ikke endring i perioder hvis det ikke er endringer`() {
        val tidligereGrunnlagsdata = grunnlagsdataMedMetadata()
        val nyGrunnlagsdata = grunnlagsdataMedMetadata()
        val endringerIPerioder = finnEndringerIPerioder(tidligereGrunnlagsdata = tidligereGrunnlagsdata, nyGrunnlagsdata = nyGrunnlagsdata)

        assertThat(endringerIPerioder.harEndringer).isFalse
    }

    private fun tidligereVedtaksperioder() =
        TidligereVedtaksperioder(
            infotrygd =
                TidligereInnvilgetVedtak(
                    harTidligereOvergangsstønad = false,
                    harTidligereBarnetilsyn = true,
                    harTidligereSkolepenger = false,
                ),
            sak =
                TidligereInnvilgetVedtak(
                    harTidligereOvergangsstønad = true,
                    harTidligereBarnetilsyn = false,
                    harTidligereSkolepenger = true,
                    periodeHistorikkOvergangsstønad = listOf(grunnlagsdataPeriodeHistorikkOvergangsstønad()),
                ),
            historiskPensjon = false,
        )

    private fun grunnlagsdataPeriodeHistorikkOvergangsstønad() =
        GrunnlagsdataPeriodeHistorikkOvergangsstønad(
            periodeType = VedtaksperiodeType.PERIODE_FØR_FØDSEL,
            fom = LocalDate.now(),
            tom = LocalDate.now().plusMonths(1),
            aktivitet = null,
            beløp = 123,
            inntekt = null,
            samordningsfradrag = null,
        )

    private fun grunnlagsdataMedMetadata(): GrunnlagsdataMedMetadata {
        val nyGrunnlagsdata = GrunnlagsdataMedMetadata(opprettGrunnlagsdata(), LocalDateTime.now())
        return nyGrunnlagsdata
    }

    private fun assertNyPerson(
        personendring: Endring<List<Personendring>>,
        ident: String,
    ) {
        assertThat(personendring.harEndringer).isTrue
        val detaljer = personendring.detaljer!!
        assertThat(detaljer).hasSize(1)
        assertThat(detaljer[0].ident).isEqualTo(ident)
        assertThat(detaljer[0].ny).isTrue
        assertThat(detaljer[0].fjernet).isFalse
        assertThat(detaljer[0].endringer).isEmpty()
    }

    private fun assertBarnHarEndringerMedDetaljer(
        endringer: Endringer,
        ident: String = barnIdent,
    ) {
        assertPersonHarEndringerMedDetaljer(endringer, ident) { it.barn }
    }

    private fun assertForelderHarEndringerMedDetaljer(
        endringer: Endringer,
        ident: String = forelderIdent,
    ) {
        assertPersonHarEndringerMedDetaljer(endringer, ident) { it.annenForelder }
    }

    private fun assertPersonHarEndringerMedDetaljer(
        endringer: Endringer,
        ident: String,
        felt: (Endringer) -> Endring<List<Personendring>>,
    ) {
        assertThat(endringer.harEndringer).isTrue
        val person = felt(endringer)
        assertThat(person.harEndringer).isTrue

        val detaljer = person.detaljer!!
        assertThat(detaljer).hasSize(1)
        assertThat(detaljer[0].ident).isEqualTo(ident)
        assertThat(detaljer[0].ny).isFalse
        assertThat(detaljer[0].fjernet).isFalse
    }

    private fun assertIngenAndreEndringer(
        endringer: Endringer,
        vararg ignorerFelt: String,
    ) {
        Endringer::class
            .memberProperties
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
        vergemål: List<VergemålDto> = emptyList(),
    ) = PersonopplysningerDto(
        personIdent = "",
        navn = NavnDto("", "", "", ""),
        kjønn = Kjønn.MANN,
        adressebeskyttelse = null,
        folkeregisterpersonstatus = status,
        fødselsdato = fødselsdato,
        dødsdato = dødsdato,
        statsborgerskap = statsborgerskap,
        sivilstand = sivilstand,
        adresse = adresse,
        fullmakt = fullmakt,
        egenAnsatt = false,
        barn = barn,
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
        oppholdstillatelse = oppholdstillatelse,
        vergemål = vergemål,
    )
}
