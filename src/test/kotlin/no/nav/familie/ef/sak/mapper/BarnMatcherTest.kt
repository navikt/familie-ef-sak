package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class BarnMatcherTest {
    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler et barn fra søknad til barn fra pdl som er født etter søknad`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer()
        val behandlingBarn =
            listOf(
                behandlingBarn(fnr = fnrBarn1),
                behandlingBarn(fnr = fnrBarn2),
                behandlingBarn(fnr = fnrBarn3),
                behandlingBarn(LocalDate.of(2018, 5, 4)),
            )
        val barnGrunnlag =
            listOf(
                pdlBarn(fnrBarn1, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn2, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn3, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn4, fødsel(LocalDate.of(2018, 5, 4))),
            )

        val barn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(behandlingBarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.behandlingBarn.personIdent).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.behandlingBarn.personIdent).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.behandlingBarn.personIdent).isEqualTo(fnrBarn3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.behandlingBarn.fødselTermindato)
            .isEqualTo(LocalDate.of(2018, 5, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler et barn fra søknad til barn fra pdl som er født for tidlig`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer()
        val behandlingBarn =
            listOf(
                behandlingBarn(fnr = fnrBarn1),
                behandlingBarn(fnr = fnrBarn2),
                behandlingBarn(fnr = fnrBarn3),
                behandlingBarn(LocalDate.of(2018, 9, 4)),
            )
        val barnGrunnlag =
            listOf(
                pdlBarn(fnrBarn1, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn2, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn3, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn4, fødsel(2018, 5, 4)),
            )

        val barn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(behandlingBarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.behandlingBarn.personIdent).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.behandlingBarn.personIdent).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.behandlingBarn.personIdent).isEqualTo(fnrBarn3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.behandlingBarn.fødselTermindato)
            .isEqualTo(LocalDate.of(2018, 9, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler et barn fra søknad til barn fra pdl som er født for sent`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer()
        val behandlingBarn =
            listOf(
                behandlingBarn(fnr = fnrBarn1),
                behandlingBarn(fnr = fnrBarn2),
                behandlingBarn(fnr = fnrBarn3),
                behandlingBarn(LocalDate.of(2018, 5, 4)),
            )
        val barnGrunnlag =
            listOf(
                pdlBarn(fnrBarn1, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn2, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn3, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn4, fødsel(LocalDate.of(2018, 5, 31))),
            )

        val barn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(behandlingBarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.behandlingBarn.personIdent).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.behandlingBarn.personIdent).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.behandlingBarn.personIdent).isEqualTo(fnrBarn3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.behandlingBarn.fødselTermindato)
            .isEqualTo(LocalDate.of(2018, 5, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler tvillinger fra søknad til tvillinger fra pdl som er født etter søknad`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer()
        val behandlingBarn =
            listOf(
                behandlingBarn(fnr = fnrBarn1),
                behandlingBarn(fnr = fnrBarn2),
                behandlingBarn(LocalDate.of(2018, 5, 4)),
                behandlingBarn(LocalDate.of(2018, 5, 4)),
            )
        val barnGrunnlag =
            listOf(
                pdlBarn(fnrBarn1, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn2, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn3, fødsel(LocalDate.of(2018, 5, 31))),
                pdlBarn(fnrBarn4, fødsel(LocalDate.of(2018, 5, 31))),
            )

        val barn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(behandlingBarn, barnGrunnlag)

        assertThat(barn).hasSize(4)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.behandlingBarn.personIdent).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.behandlingBarn.personIdent).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.behandlingBarn.fødselTermindato)
            .isEqualTo(LocalDate.of(2018, 5, 4))
        assertThat(barn.first { it.fødselsnummer == fnrBarn4 }.behandlingBarn.fødselTermindato)
            .isEqualTo(LocalDate.of(2018, 5, 4))
    }

    @Test
    fun `kobleSøknadsbarnOgRegisterBarn kobler barn fra søknad til alternativ fra pdl som er født nærmest termindato`() {
        val fnrBarn1 = FnrGenerator.generer()
        val fnrBarn2 = FnrGenerator.generer()
        val fnrBarn3 = FnrGenerator.generer()
        val fnrBarn4 = FnrGenerator.generer()
        val behandlingBarn =
            listOf(
                behandlingBarn(fnr = fnrBarn1),
                behandlingBarn(fnr = fnrBarn2),
                behandlingBarn(LocalDate.of(2018, 5, 4)),
            )
        val barnGrunnlag =
            listOf(
                pdlBarn(fnrBarn1, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn2, fødsel(LocalDate.now())),
                pdlBarn(fnrBarn3, fødsel(LocalDate.of(2018, 5, 15))),
                pdlBarn(fnrBarn4, fødsel(LocalDate.of(2018, 5, 31))),
            )

        val barn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(behandlingBarn, barnGrunnlag)

        assertThat(barn).hasSize(3)
        assertThat(barn.first { it.fødselsnummer == fnrBarn1 }.behandlingBarn.personIdent).isEqualTo(fnrBarn1)
        assertThat(barn.first { it.fødselsnummer == fnrBarn2 }.behandlingBarn.personIdent).isEqualTo(fnrBarn2)
        assertThat(barn.first { it.fødselsnummer == fnrBarn3 }.behandlingBarn.fødselTermindato)
            .isEqualTo(LocalDate.of(2018, 5, 4))
        assertThat(barn.firstOrNull { it.fødselsnummer == fnrBarn4 }).isNull()
    }

    private fun behandlingBarn(
        terminDato: LocalDate? = null,
        fnr: String? = null,
    ) = BehandlingBarn(
        personIdent = fnr,
        fødselTermindato = terminDato,
        behandlingId = UUID.randomUUID(),
        navn = "navn",
        søknadBarnId = null,
    )

    private fun pdlBarn(
        fnr: String,
        fødsel: Fødsel,
    ) = BarnMedIdent(
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList(),
        listOf(fødsel),
        Navn("", "", "", Metadata(false)),
        fnr,
        null,
    )
}
