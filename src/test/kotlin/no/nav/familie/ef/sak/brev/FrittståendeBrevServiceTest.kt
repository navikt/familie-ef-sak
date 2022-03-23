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
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as KontrakterFrittståendeBrevDto


internal class FrittståendeBrevServiceTest {

    private val brevClient = mockk<BrevClient>()
    private val fagsakService = mockk<FagsakService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val iverksettClient = mockk<IverksettClient>()
    private val brevsignaturService = mockk<BrevsignaturService>()

    private val frittståendeBrevService =
            FrittståendeBrevService(brevClient,
                                    fagsakService,
                                    personopplysningerService,
                                    arbeidsfordelingService,
                                    iverksettClient,
                                    brevsignaturService

            )
    private val fagsak = fagsak(fagsakpersoner(identer = setOf("01010172272")))
    private val frittståendeBrevDto = FrittståendeBrevDto(
        "overskrift",
        listOf(
            FrittståendeBrevAvsnitt(
                "deloverskrift",
                "innhold"
            )
        ),
        fagsak.id, FrittståendeBrevKategori.INFORMASJONSBREV
    )


    private val brevtyperTestData = listOf(Pair(StønadType.OVERGANGSSTØNAD,
                                                FrittståendeBrevKategori.INFORMASJONSBREV) to FrittståendeBrevType.INFORMASJONSBREV,
            Pair(StønadType.OVERGANGSSTØNAD,
                 FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER) to FrittståendeBrevType.INNHENTING_AV_OPPLYSNINGER,
            Pair(StønadType.OVERGANGSSTØNAD,
                 FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT) to FrittståendeBrevType.VARSEL_OM_AKTIVITETSPLIKT,
            Pair(StønadType.SKOLEPENGER, FrittståendeBrevKategori.INFORMASJONSBREV) to FrittståendeBrevType.INFORMASJONSBREV,
            Pair(StønadType.SKOLEPENGER,
                 FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER) to FrittståendeBrevType.INNHENTING_AV_OPPLYSNINGER,
            Pair(StønadType.SKOLEPENGER,
                 FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT) to FrittståendeBrevType.VARSEL_OM_AKTIVITETSPLIKT,
            Pair(StønadType.BARNETILSYN, FrittståendeBrevKategori.INFORMASJONSBREV) to FrittståendeBrevType.INFORMASJONSBREV,
            Pair(StønadType.BARNETILSYN,
                 FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER) to FrittståendeBrevType.INNHENTING_AV_OPPLYSNINGER,
            Pair(StønadType.BARNETILSYN,
                 FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT) to FrittståendeBrevType.VARSEL_OM_AKTIVITETSPLIKT)

    @TestFactory
    fun `skal sende frittstående brev med riktig brevtype`() =
            brevtyperTestData.map { (input, forventetBrevtype) ->
                DynamicTest.dynamicTest("Skal sende brev for stønadtype ${input.first} og brevkategori " +
                                        "${input.second} til iverksett for journalføring med brevtype $forventetBrevtype") {
                    mockAvhengigheter()

                    val frittståendeBrevSlot = slot<KontrakterFrittståendeBrevDto>()
                    every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak.copy(stønadstype = input.first)
                    every { iverksettClient.sendFrittståendeBrev(capture(frittståendeBrevSlot)) } just Runs

                    frittståendeBrevService.sendFrittståendeBrev(frittståendeBrevDto.copy(brevType = input.second))

                    assertThat(frittståendeBrevSlot.captured.brevtype).isEqualTo(forventetBrevtype)
            }
        }

    private fun mockAvhengigheter() {
        BrukerContextUtil.mockBrukerContext("Saksbehandler")
        every { brevClient.genererBrev(any(), any(), any()) } returns "123".toByteArray()
        every { fagsakService.hentAktivIdent(any()) } returns fagsak.hentAktivIdent()
        every { fagsakService.fagsakMedOppdatertPersonIdent(any()) } returns fagsak
        every { fagsakService.hentFagsak(any()) } returns fagsak
        every { fagsakService.hentEksternId(any()) } returns Long.MAX_VALUE
        every { personopplysningerService.hentGjeldeneNavn(any()) } returns mapOf(fagsak.hentAktivIdent() to "Navn Navnesen")
        every { personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(any()) } returns ADRESSEBESKYTTELSEGRADERING.UGRADERT
        every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) } returns "123"
        every { iverksettClient.sendFrittståendeBrev(any()) } just Runs
        every { brevsignaturService.lagSignaturMedEnhet(any()) } returns SignaturDto("Navn Navnesen", "En enhet", false)
    }

    companion object {

        @AfterAll
        @JvmStatic
        fun tearDown() {
            BrukerContextUtil.clearBrukerContext()
        }
    }


}
