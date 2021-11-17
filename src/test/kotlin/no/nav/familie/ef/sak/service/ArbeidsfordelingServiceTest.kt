package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.testutil.pdlBarn
import no.nav.familie.ef.sak.testutil.pdlSøker
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cache.concurrent.ConcurrentMapCacheManager

internal class ArbeidsfordelingServiceTest {

    private lateinit var personService: PersonService
    private lateinit var personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @BeforeEach
    internal fun setUp() {
        personService = mockk()
        personopplysningerIntegrasjonerClient = mockk()
        every { personopplysningerIntegrasjonerClient.hentNavEnhet(any()) } returns listOf()
        val cacheManager = ConcurrentMapCacheManager()
        arbeidsfordelingService = ArbeidsfordelingService(personService, personopplysningerIntegrasjonerClient, cacheManager)
    }

    @Test
    internal fun `skal hente arbeidsfordeling til forelderen hvis graderingen er lavere på barnet`() {
        every { personService.hentPersonMedRelasjoner(IDENT_FORELDER) } returns
                søkerMedBarn(graderingForelder = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                             graderingBarn = AdressebeskyttelseGradering.FORTROLIG)

        arbeidsfordelingService.hentNavEnhet(IDENT_FORELDER)
        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavEnhet(IDENT_FORELDER) }
    }

    @Test
    internal fun `skal hente arbeidsfordeling til barnet hvis graderingen er lavere på forelderen`() {
        every { personService.hentPersonMedRelasjoner(IDENT_FORELDER) } returns
                søkerMedBarn(graderingForelder = AdressebeskyttelseGradering.FORTROLIG,
                             graderingBarn = AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        arbeidsfordelingService.hentNavEnhet(IDENT_FORELDER)
        verify(exactly = 1) { personopplysningerIntegrasjonerClient.hentNavEnhet(IDENT_BARN) }
    }

    @Test
    internal fun `hentNavEnhet - skal cache når man kaller på den indirekte`() {
        every { personService.hentPersonMedRelasjoner(any()) } returns søkerMedBarn()

        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_FORELDER)
        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_FORELDER)
        verify(exactly = 1) { personService.hentPersonMedRelasjoner(IDENT_FORELDER) }

        arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(IDENT_BARN)
        verify(exactly = 1) { personService.hentPersonMedRelasjoner(IDENT_BARN) }
    }

    private fun søkerMedBarn(graderingForelder: AdressebeskyttelseGradering = AdressebeskyttelseGradering.FORTROLIG,
                             graderingBarn: AdressebeskyttelseGradering = AdressebeskyttelseGradering.FORTROLIG): SøkerMedBarn =
            SøkerMedBarn(IDENT_FORELDER,
                         pdlSøker(graderingForelder),
                         mapOf(IDENT_BARN to pdlBarn(adressebeskyttelse = adressebeskyttelse(graderingBarn))))

    private fun pdlSøker(adressebeskyttelseGradering: AdressebeskyttelseGradering) =
            pdlSøker(adressebeskyttelse = adressebeskyttelse(adressebeskyttelseGradering))

    private fun adressebeskyttelse(adressebeskyttelseGradering: AdressebeskyttelseGradering) =
            listOf(Adressebeskyttelse(adressebeskyttelseGradering, Metadata(false)))

    companion object {

        private const val IDENT_FORELDER = "1"
        private const val IDENT_BARN = "2"
    }
}