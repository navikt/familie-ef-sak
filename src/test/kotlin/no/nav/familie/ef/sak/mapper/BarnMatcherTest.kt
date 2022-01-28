package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class BarnMatcherTest {

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler et barn fra søknad til barn fra pdl som er født etter søknad`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer(2018, 5, 4)
        val søknadsbarn = setOf(søknadsbarn(fnr = fnrBarn1),
                                søknadsbarn(fnr = fnrBarn2),
                                søknadsbarn(fnr = fnrBarn3),
                                søknadsbarn(LocalDate.of(2018, 5, 4)))
        val barnGrunnlag = listOf(pdlBarn(fnrBarn1),
                                  pdlBarn(fnrBarn2),
                                  pdlBarn(fnrBarn3),
                                  pdlBarn(fnrBarn4))

        val barn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknadsbarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.søknadsbarn.fødselTermindato)
                .isEqualTo(LocalDate.of(2018, 5, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler et barn fra søknad til barn fra pdl som er født for tidlig`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer(2018, 5, 4)
        val søknadsbarn = setOf(søknadsbarn(fnr = fnrBarn1),
                                søknadsbarn(fnr = fnrBarn2),
                                søknadsbarn(fnr = fnrBarn3),
                                søknadsbarn(LocalDate.of(2018, 9, 4)))
        val barnGrunnlag = listOf(pdlBarn(fnrBarn1),
                                  pdlBarn(fnrBarn2),
                                  pdlBarn(fnrBarn3),
                                  pdlBarn(fnrBarn4))

        val barn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknadsbarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.søknadsbarn.fødselTermindato)
                .isEqualTo(LocalDate.of(2018, 9, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler et barn fra søknad til barn fra pdl som er født for sent`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer(2018, 5, 31)
        val søknadsbarn = setOf(søknadsbarn(fnr = fnrBarn1),
                                søknadsbarn(fnr = fnrBarn2),
                                søknadsbarn(fnr = fnrBarn3),
                                søknadsbarn(LocalDate.of(2018, 5, 4)))
        val barnGrunnlag = listOf(pdlBarn(fnrBarn1),
                                  pdlBarn(fnrBarn2),
                                  pdlBarn(fnrBarn3),
                                  pdlBarn(fnrBarn4))

        val barn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknadsbarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.søknadsbarn.fødselTermindato)
                .isEqualTo(LocalDate.of(2018, 5, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler tvillinger fra søknad til tvillinger fra pdl som er født etter søknad`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer(2018, 5, 31)
        val fnrBarn4 = FnrGenerator.generer(2018, 5, 31)
        val søknadsbarn = setOf(søknadsbarn(fnr = fnrBarn1),
                                søknadsbarn(fnr = fnrBarn2),
                                søknadsbarn(LocalDate.of(2018, 5, 4)),
                                søknadsbarn(LocalDate.of(2018, 5, 4)))
        val barnGrunnlag = listOf(pdlBarn(fnrBarn1),
                                  pdlBarn(fnrBarn2),
                                  pdlBarn(fnrBarn3),
                                  pdlBarn(fnrBarn4))

        val barn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknadsbarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.søknadsbarn.fødselTermindato)
                .isEqualTo(LocalDate.of(2018, 5, 4))
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.søknadsbarn.fødselTermindato)
                .isEqualTo(LocalDate.of(2018, 5, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler barn fra søknad til alternativ fra pdl som er født nærmest termindato`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer(2018, 5, 15)
        val fnrBarn4 = FnrGenerator.generer(2018, 5, 31)
        val søknadsbarn = setOf(søknadsbarn(fnr = fnrBarn1),
                                søknadsbarn(fnr = fnrBarn2),
                                søknadsbarn(LocalDate.of(2018, 5, 4)))
        val barnGrunnlag = listOf(pdlBarn(fnrBarn1),
                                  pdlBarn(fnrBarn2),
                                  pdlBarn(fnrBarn3),
                                  pdlBarn(fnrBarn4))

        val barn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknadsbarn, barnGrunnlag)

        assertThat(barn).hasSize(3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.søknadsbarn.fødselsnummer).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.søknadsbarn.fødselTermindato)
                .isEqualTo(LocalDate.of(2018, 5, 4))
        assertThat(barn.firstOrNull { it.fødselsnummer == fnrBarn4 }).isNull()
    }


    private fun søknadsbarn(terminDato: LocalDate? = null, fnr: String? = null) =
            SøknadBarn(fødselsnummer = fnr,
                       fødselTermindato = terminDato,
                       erBarnetFødt = true,
                       harSkalHaSammeAdresse = true,
                       ikkeRegistrertPåSøkersAdresseBeskrivelse = "",
                       lagtTilManuelt = false)

    private fun pdlBarn(fnr: String) =
            BarnMedIdent(emptyList(),
                         emptyList(),
                         emptyList(),
                         emptyList(),
                         emptyList(),
                         emptyList(),
                         Navn("", "", "", Metadata(false)),
                         fnr)

}