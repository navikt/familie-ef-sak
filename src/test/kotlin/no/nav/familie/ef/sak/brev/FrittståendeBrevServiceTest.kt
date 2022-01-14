package no.nav.familie.ef.sak.brev

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as KontrakterFrittståendeBrevDto


internal class FrittståendeBrevServiceTest {

    val brevClient = mockk<BrevClient>()
    val fagsakService = mockk<FagsakService>()
    val personopplysningerService = mockk<PersonopplysningerService>()
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val iverksettClient = mockk<IverksettClient>()
    val brevsignaturService = mockk<BrevsignaturService>()

    val frittståendeBrevService =
            FrittståendeBrevService(brevClient,
                                    fagsakService,
                                    personopplysningerService,
                                    arbeidsfordelingService,
                                    iverksettClient,
                                    brevsignaturService

            )

    val frittståendeBrevDto = FrittståendeBrevDto("overskrift",
                                                  listOf(FrittståendeBrevAvsnitt("deloverskrift",
                                                                                 "innhold")),
                                                  UUID.randomUUID(), FrittståendeBrevKategori.INFORMASJONSBREV)


    private val brevtyperTestData = listOf(Pair(Stønadstype.OVERGANGSSTØNAD,
                 FrittståendeBrevKategori.INFORMASJONSBREV) to FrittståendeBrevType.INFOBREV_OVERGANGSSTØNAD,
            Pair(Stønadstype.OVERGANGSSTØNAD,
                 FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER) to FrittståendeBrevType.MANGELBREV_OVERGANGSSTØNAD,
            Pair(Stønadstype.OVERGANGSSTØNAD,
                 FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT) to FrittståendeBrevType.INFOBREV_OVERGANGSSTØNAD,
            Pair(Stønadstype.SKOLEPENGER, FrittståendeBrevKategori.INFORMASJONSBREV) to FrittståendeBrevType.INFOBREV_SKOLEPENGER,
            Pair(Stønadstype.SKOLEPENGER,
                 FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER) to FrittståendeBrevType.MANGELBREV_SKOLEPENGER,
            Pair(Stønadstype.SKOLEPENGER,
                 FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT) to FrittståendeBrevType.INFOBREV_SKOLEPENGER,
            Pair(Stønadstype.BARNETILSYN, FrittståendeBrevKategori.INFORMASJONSBREV) to FrittståendeBrevType.INFOBREV_BARNETILSYN,
            Pair(Stønadstype.BARNETILSYN,
                 FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER) to FrittståendeBrevType.MANGELBREV_BARNETILSYN,
            Pair(Stønadstype.BARNETILSYN,
                 FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT) to FrittståendeBrevType.INFOBREV_BARNETILSYN)

    @TestFactory
    fun `skal sende frittstående brev med riktig brevtype`() =
            brevtyperTestData.map { (input, forventetBrevtype) ->
                DynamicTest.dynamicTest("Skal sende brev for stønadtype ${input.first} og brevkategori ${input.second} til iverksett for journalføring med brevtype $forventetBrevtype") {
                    mockAvhengigheter()

                val frittståendeBrevSlot = slot<KontrakterFrittståendeBrevDto>()
                every { fagsakService.hentFagsak(any()) } returns Fagsak(stønadstype = input.first)
                every { iverksettClient.sendFrittståendeBrev(capture(frittståendeBrevSlot)) } just Runs

                frittståendeBrevService.sendFrittståendeBrev(frittståendeBrevDto.copy(brevType = input.second))

                assertThat(frittståendeBrevSlot.captured.brevtype).isEqualTo(forventetBrevtype)
            }
        }

    @Test
    fun `skal sende frittsrående brev med NAV Vikafossen signatur`() {
        val enhet = slot<String>()
        val signatur = slot<String>()

        mockAvhengigheter()

        every {
            personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any())
        } returns ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG

        every { brevClient.genererBrev(any(), capture(signatur), capture(enhet)) } returns "123".toByteArray()

        frittståendeBrevService.sendFrittståendeBrev(frittståendeBrevDto)

        assertThat(enhet.captured).isEqualTo("NAV Vikafossen")
        assertThat(signatur.captured).isEqualTo("NAV anonym")
    }

    private fun mockAvhengigheter() {
        BrukerContextUtil.mockBrukerContext("Saksbehandler")
        every { brevClient.genererBrev(any(), any(), "NAV Arbeid og ytelser") } returns "123".toByteArray()
        every { fagsakService.hentAktivIdent(any()) } returns "12345678910"
        every { fagsakService.hentFagsak(any()) } returns fagsak()
        every { fagsakService.hentEksternId(any()) } returns Long.MAX_VALUE
        every { personopplysningerService.hentGjeldeneNavn(any()) } returns mapOf("12345678910" to "Navn Navnesen")
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "123"
        every { iverksettClient.sendFrittståendeBrev(any()) } just Runs
    }

    companion object {

        @AfterAll
        @JvmStatic
        fun tearDown() {
            BrukerContextUtil.clearBrukerContext()
        }
    }


}