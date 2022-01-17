package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.slot
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BrevsignaturServiceTest {
    @Test
    fun `skal sende frittstående brev med NAV Vikafossen signatur`() {
        val enhet = slot<String>()
        val signatur = slot<String>()

        mockAvhengigheter()

        every {
            personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any())
        } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG

        every { brevsignaturService.lagSignaturMedEnhet(any()) } returns SignaturDto("Navn Navnesen", "En enhet")

        every { brevClient.genererBrev(any(), capture(signatur), capture(enhet)) } returns "123".toByteArray()

        frittståendeBrevService.sendFrittståendeBrev(frittståendeBrevDto)

        val signaturNavn = "NAV Vikafossen"
        Assertions.assertThat(enhet.captured).isEqualTo(signaturNavn)
        Assertions.assertThat(signatur.captured).isEqualTo("NAV anonym")
    }
}