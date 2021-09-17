package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.felles.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.vurdering.medlemskap.pdlBarn
import no.nav.familie.ef.sak.vurdering.medlemskap.pdlSøker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ArbeidsfordelingServiceTest {

    private lateinit var personService: PersonService
    private lateinit var familieIntegrasjonerClient: FamilieIntegrasjonerClient
    private lateinit var arbeidsfordelingService: ArbeidsfordelingService

    @BeforeEach
    internal fun setUp() {
        personService = mockk()
        familieIntegrasjonerClient = mockk()
        every { familieIntegrasjonerClient.hentNavEnhet(any()) } returns listOf()
        arbeidsfordelingService = ArbeidsfordelingService(personService, familieIntegrasjonerClient)
    }

    @Test
    internal fun `skal hente arbeidsfordeling til forelderen hvis graderingen er lavere på barnet`() {
        every { personService.hentPersonMedRelasjoner(IDENT_FORELDER) } returns
                søkerMedBarn(graderingForelder = AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                             graderingBarn = AdressebeskyttelseGradering.FORTROLIG)

        arbeidsfordelingService.hentNavEnhet(IDENT_FORELDER)
        verify(exactly = 1) { familieIntegrasjonerClient.hentNavEnhet(IDENT_FORELDER) }
    }

    @Test
    internal fun `skal hente arbeidsfordeling til barnet hvis graderingen er lavere på forelderen`() {
        every { personService.hentPersonMedRelasjoner(IDENT_FORELDER) } returns
                søkerMedBarn(graderingForelder = AdressebeskyttelseGradering.FORTROLIG,
                             graderingBarn = AdressebeskyttelseGradering.STRENGT_FORTROLIG)

        arbeidsfordelingService.hentNavEnhet(IDENT_FORELDER)
        verify(exactly = 1) { familieIntegrasjonerClient.hentNavEnhet(IDENT_BARN) }
    }

    private fun søkerMedBarn(graderingForelder: AdressebeskyttelseGradering,
                             graderingBarn: AdressebeskyttelseGradering): SøkerMedBarn =
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