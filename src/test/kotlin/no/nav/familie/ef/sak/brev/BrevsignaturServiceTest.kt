package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class BrevsignaturServiceTest {
    val personopplysningerService = mockk<PersonopplysningerService>()
    val vedtakService = mockk<VedtakService>()
    val brevsignaturService = BrevsignaturService(personopplysningerService, vedtakService)

    @Test
    fun `skal sende frittstående brev med NAV Vikafossen signatur dersom person har strengt fortrolig adresse`() {
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(true))
        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_VIKAFOSSEN)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
    }

    @Test
    fun `skal sende frittstående brev med vanlig nay signatur når søkeradressebeskyttelse er ugradert`() {
        val fortventetSaksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(fortventetSaksbehandlerNavn)
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(false))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(fortventetSaksbehandlerNavn)

        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal sende frittstående brev med vanlig nay signatur selv om vedtak er uten beslutter`() {
        val fortventetSaksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(fortventetSaksbehandlerNavn)
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT

        val signaturMedEnhet = brevsignaturService.lagSaksbehandlerSignatur("123", VedtakErUtenBeslutter(true))

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(fortventetSaksbehandlerNavn)

        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal sende vedtaksbrev med NAV Vikafossen signatur dersom person har strengt fortrolig adresse`() {
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        val signaturMedEnhet = brevsignaturService.lagBeslutterSignatur("123", VedtakErUtenBeslutter(false))
        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_VIKAFOSSEN)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
    }

    @Test
    fun `skal sende vedtaksbrev brev med vanlig nay signatur når søkeradressebeskyttelse er ugradert`() {
        val fortventetSaksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(fortventetSaksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        val signaturMedEnhet = brevsignaturService.lagBeslutterSignatur("123", VedtakErUtenBeslutter(false))
        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(fortventetSaksbehandlerNavn)
    }

    @Test
    fun `skal ikke sette navn på beslutter dersom vedtak er uten beslutter`() {
        val saksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(saksbehandlerNavn)

        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        val signaturMedEnhet = brevsignaturService.lagBeslutterSignatur("123", VedtakErUtenBeslutter(false))
        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo("")
    }


}
