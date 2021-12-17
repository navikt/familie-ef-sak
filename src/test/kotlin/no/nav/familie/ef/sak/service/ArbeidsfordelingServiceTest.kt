package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class ArbeidsfordelingServiceTest {

    private lateinit var personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @BeforeEach
    internal fun setUp() {
        personopplysningerIntegrasjonerClient = mockk()
        every { personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(any()) } returns listOf()
        val cacheManager = ConcurrentMapCacheManager()
        arbeidsfordelingService = ArbeidsfordelingService(personopplysningerIntegrasjonerClient, cacheManager)
    }

    @Test
    internal fun `skal hente arbeidsfordeling`() {
        arbeidsfordelingService.hentNavEnhet(IDENT_FORELDER)
        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(IDENT_FORELDER) }
    }

    @Test
    internal fun `hentNavEnhet - skal cache når man kaller på den indirekte`() {
        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_FORELDER)
        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_FORELDER)
        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(IDENT_FORELDER) }

        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_BARN)
        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(IDENT_BARN) }
    }

    companion object {

        private const val IDENT_FORELDER = "1"
        private const val IDENT_BARN = "2"
    }
}