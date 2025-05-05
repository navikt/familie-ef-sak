package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.brev.BrevsignaturService.Companion.NAV_ENHET_NAY
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.oppgave.OppgaveClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.saksbehandler
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class BrevsignaturServiceTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val oppgaveClient = mockk<OppgaveClient>()
    private val brevsignaturService = BrevsignaturService(personopplysningerService, oppgaveClient)

    @Test
    fun `skal sende frittstående brev med NAV Vikafossen signatur dersom person har strengt fortrolig adresse`() {
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(false))
        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.NAV_ENHET_VIKAFOSSEN)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isTrue()
    }

    @ValueSource(
        strings = ["NAV ARBEID OG YTELSER SKIEN", "NAV ARBEID OG YTELSER MØRE OG ROMSDAL", "NAV ARBEID OG YTELSER SØRLANDET"],
    )
    @ParameterizedTest
    fun `skal sende frittstående brev med vanlig nay signatur med geografisk lokasjon når søkeradressebeskyttelse er ugradert`(enhetsnavn: String) {
        val saksbehandlerNavn = "Ole Olsen"
        val forventetEnhetsnavn = enhetsnavnTilVisningstekst[enhetsnavn]
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo(saksbehandlerNavn) } returns saksbehandler(enhetsnavn)

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(false))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(forventetEnhetsnavn)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(saksbehandlerNavn)
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isFalse()

        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal sende frittstående brev med vanlig nay signatur uten geografisk lokasjon når enhetsnavn er uvanlig`() {
        val saksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo(saksbehandlerNavn) } returns saksbehandler(enhetsnavn = "NAV ARBEID OG YTELSER UVENTET ENHET")

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(false))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(NAV_ENHET_NAY)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(saksbehandlerNavn)
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isFalse()

        BrukerContextUtil.clearBrukerContext()
    }

    @ValueSource(
        strings = ["NAV ARBEID OG YTELSER SKIEN", "NAV ARBEID OG YTELSER MØRE OG ROMSDAL", "NAV ARBEID OG YTELSER SØRLANDET"],
    )
    @ParameterizedTest
    fun `skal sende frittstående brev med vanlig nay signatur med geografisk lokasjon selv om vedtak er uten beslutter`(enhetsnavn: String) {
        val saksbehandlerNavn = "Ole Olsen"
        val forventetEnhetsnavn = enhetsnavnTilVisningstekst[enhetsnavn]
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo(saksbehandlerNavn) } returns saksbehandler(enhetsnavn)

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(true))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(forventetEnhetsnavn)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(saksbehandlerNavn)
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isTrue()

        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal sende vedtaksbrev med NAV Vikafossen signatur dersom person har strengt fortrolig adresse`() {
        val saksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG

        val signaturMedEnhet = brevsignaturService.lagBeslutterSignatur("123", VedtakErUtenBeslutter(false))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.NAV_ENHET_VIKAFOSSEN)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isTrue()

        BrukerContextUtil.clearBrukerContext()
    }

    @ValueSource(
        strings = ["NAV ARBEID OG YTELSER SKIEN", "NAV ARBEID OG YTELSER MØRE OG ROMSDAL", "NAV ARBEID OG YTELSER SØRLANDET"],
    )
    @ParameterizedTest
    fun `skal sende vedtaksbrev med vanlig nay signatur med geografisk lokasjon når søkeradressebeskyttelse er ugradert`(enhetsnavn: String) {
        val fortventetSaksbehandlerNavn = "Ole Olsen"
        val forventetEnhetsnavn = enhetsnavnTilVisningstekst[enhetsnavn]
        BrukerContextUtil.mockBrukerContext(fortventetSaksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo(fortventetSaksbehandlerNavn) } returns saksbehandler(enhetsnavn)

        val signaturMedEnhet = brevsignaturService.lagBeslutterSignatur("123", VedtakErUtenBeslutter(false))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(forventetEnhetsnavn)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(fortventetSaksbehandlerNavn)
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isFalse()

        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal ikke sette navn på beslutter dersom vedtak er uten beslutter`() {
        val saksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo(saksbehandlerNavn) } returns saksbehandler()

        val signaturMedEnhet = brevsignaturService.lagBeslutterSignatur("123", VedtakErUtenBeslutter(true))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo("")
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo("")
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isTrue()

        BrukerContextUtil.clearBrukerContext()
    }

    @ValueSource(
        strings = ["NAV ARBEID OG YTELSER SKIEN", "NAV ARBEID OG YTELSER MØRE OG ROMSDAL", "NAV ARBEID OG YTELSER SØRLANDET"],
    )
    @ParameterizedTest
    fun `skal sette riktig saksbehandlerenhet med geografisk tilhørighet i brevsignatur`(enhetsnavn: String) {
        val saksbehandlerNavn = "Ole Olsen"
        val forventetEnhetsnavn = enhetsnavnTilVisningstekst[enhetsnavn]
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo("A") } returns saksbehandler(enhetsnavn)

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(true), "Ole Olsen", "A")

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(forventetEnhetsnavn)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo("Ole Olsen")
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isTrue()

        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal sette saksbehandlerenhet uten geografisk tilhørighet i brevsignatur`() {
        val saksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { oppgaveClient.hentSaksbehandlerInfo("A") } returns saksbehandler("NAV ARBEID OG YTELSER UVENTET ENHET")

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(true), "Ole Olsen", "A")

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo("Nav arbeid og ytelser")
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo("Ole Olsen")
        Assertions.assertThat(signaturMedEnhet.skjulBeslutter).isTrue()

        BrukerContextUtil.clearBrukerContext()
    }

    companion object {
        val enhetsnavnTilVisningstekst =
            mapOf(
                "NAV ARBEID OG YTELSER SKIEN" to "Nav arbeid og ytelser Skien",
                "NAV ARBEID OG YTELSER MØRE OG ROMSDAL" to "Nav arbeid og ytelser Møre og Romsdal",
                "NAV ARBEID OG YTELSER SØRLANDET" to "Nav arbeid og ytelser Sørlandet",
            )
    }
}
