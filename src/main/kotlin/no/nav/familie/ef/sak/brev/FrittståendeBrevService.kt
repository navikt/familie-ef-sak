package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as FrittståendeBrevDtoIverksetting

@Service
class FrittståendeBrevService(private val brevClient: BrevClient,
                              private val fagsakService: FagsakService,
                              private val personopplysningerService: PersonopplysningerService,
                              private val arbeidsfordelingService: ArbeidsfordelingService,
                              private val iverksettClient: IverksettClient,
                              private val brevsignaturService: BrevsignaturService) {

    fun forhåndsvisFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto): ByteArray {
        return lagFrittståendeBrevMedSignatur(frittståendeBrevDto)
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        val ident = fagsakService.hentAktivIdent(frittståendeBrevDto.fagsakId)
        val brev = lagFrittståendeBrevMedSignatur(frittståendeBrevDto)
        val eksternFagsakId = fagsakService.hentEksternId(frittståendeBrevDto.fagsakId)
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(
            ident)
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(true)
        val stønadstype = fagsakService.hentFagsak(frittståendeBrevDto.fagsakId).stønadstype
        val brevType = utledFrittståendeBrevType(frittståendeBrevDto, stønadstype)
        iverksettClient.sendFrittståendeBrev(FrittståendeBrevDtoIverksetting(personIdent = ident,
                                                                             eksternFagsakId = eksternFagsakId,
                                                                             brevtype = brevType,
                                                                             fil = brev,
                                                                             journalførendeEnhet = journalførendeEnhet,
                                                                             saksbehandlerIdent = saksbehandlerIdent))
    }

    private fun lagFrittståendeBrevRequest(frittståendeBrevDto: FrittståendeBrevDto, ident: String): FrittståendeBrevRequestDto {
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(ident))
        return FrittståendeBrevRequestDto(overskrift = frittståendeBrevDto.overskrift,
                                          avsnitt = frittståendeBrevDto.avsnitt,
                                          personIdent = ident,
                                          navn = navn.getValue(ident))
    }

    private fun lagFrittståendeBrevMedSignatur(
        frittståendeBrevDto: FrittståendeBrevDto
    ): ByteArray {
        val fagsak = fagsakService.hentFagsak(frittståendeBrevDto.fagsakId)
        val aktivIdent = fagsak.hentAktivIdent()
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto, aktivIdent)
        val signatur = brevsignaturService.lagSignaturMedEnhet(frittståendeBrevDto.fagsakId)
        val brev = brevClient.genererBrev(request, signatur.navn, signatur.enhet)
        return brev
    }


    private fun utledFrittståendeBrevType(frittståendeBrevDto: FrittståendeBrevDto,
                                          stønadstype: StønadType) =
            when (frittståendeBrevDto.brevType) {
                FrittståendeBrevKategori.INFORMASJONSBREV, FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT ->
                    utledBrevtypeInfobrev(stønadstype)
                FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER -> utledBrevtypeMangelbrev(stønadstype)
                FrittståendeBrevKategori.VARSEL_OM_SANKSJON -> utledBrevtypeSanksjonsbrev(stønadstype)
            }

    private fun utledBrevtypeInfobrev(stønadstype: StønadType) =
            when (stønadstype) {
                StønadType.OVERGANGSSTØNAD -> FrittståendeBrevType.INFOBREV_OVERGANGSSTØNAD
                StønadType.BARNETILSYN -> FrittståendeBrevType.INFOBREV_BARNETILSYN
                StønadType.SKOLEPENGER -> FrittståendeBrevType.INFOBREV_SKOLEPENGER
            }

    private fun utledBrevtypeMangelbrev(stønadstype: StønadType) =
            when (stønadstype) {
                StønadType.OVERGANGSSTØNAD -> FrittståendeBrevType.MANGELBREV_OVERGANGSSTØNAD
                StønadType.BARNETILSYN -> FrittståendeBrevType.MANGELBREV_BARNETILSYN
                StønadType.SKOLEPENGER -> FrittståendeBrevType.MANGELBREV_SKOLEPENGER
            }

    private fun utledBrevtypeSanksjonsbrev(stønadstype: StønadType) =
            when (stønadstype) {
                StønadType.OVERGANGSSTØNAD -> FrittståendeBrevType.SANKSJONSBREV_OVERGANGSTØNAD
                StønadType.BARNETILSYN -> FrittståendeBrevType.SANKSJONSBREV_BARNETILSYN
                StønadType.SKOLEPENGER -> FrittståendeBrevType.SANKSJONSBREV_SKOLEPENGER
            }
}



