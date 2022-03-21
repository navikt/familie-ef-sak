package no.nav.familie.ef.sak.brev

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.UUID

internal class BrevsignaturServiceTest {

    val personopplysningerService = mockk<PersonopplysningerService>()
    val fagsakService = mockk<FagsakService>()

    val brevsignaturService = BrevsignaturService(personopplysningerService, fagsakService)

    @BeforeEach
    fun setUp(){
        every { fagsakService.hentFagsak(any()) } returns fagsak(identer = setOf(PersonIdent("123")))
    }

    @Test
    fun `skal sende frittstående brev med NAV Vikafossen signatur `() {
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(UUID.randomUUID())
        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_VIKAFOSSEN)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(BrevsignaturService.NAV_ANONYM_NAVN)
    }

    @Test
    fun `skal sende frittstående brev med vanlig nay signatur når søkeradressebeskyttelse er ugradert `() {
        val fortventetSaksbehandlerNavn = "Ole Olsen"
        BrukerContextUtil.mockBrukerContext(fortventetSaksbehandlerNavn)
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT

        val signaturMedEnhet = brevsignaturService.lagSignaturMedEnhet(UUID.randomUUID())

        Assertions.assertThat(signaturMedEnhet.enhet).isEqualTo(BrevsignaturService.ENHET_NAY)
        Assertions.assertThat(signaturMedEnhet.navn).isEqualTo(fortventetSaksbehandlerNavn)

        BrukerContextUtil.clearBrukerContext()
    }
}