package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
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
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(frittståendeBrevDto.fagsakId)
        val ident = fagsak.hentAktivIdent()
        val brev = lagFrittståendeBrevMedSignatur(frittståendeBrevDto)
        val eksternFagsakId = fagsak.eksternId.id
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(ident)
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(true)
        val stønadstype = fagsak.stønadstype
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

    private fun lagFrittståendeBrevMedSignatur(frittståendeBrevDto: FrittståendeBrevDto): ByteArray {
        val fagsak = fagsakService.hentFagsak(frittståendeBrevDto.fagsakId)
        return lagFrittståendeBrevMedSignatur(frittståendeBrevDto, fagsak)
    }

    private fun lagFrittståendeBrevMedSignatur(frittståendeBrevDto: FrittståendeBrevDto, fagsak: Fagsak): ByteArray {
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto, fagsak.hentAktivIdent())
        val signatur = brevsignaturService.lagSignaturMedEnhet(frittståendeBrevDto.fagsakId)
        return brevClient.genererBrev(request, signatur.navn, signatur.enhet)
    }


    private fun utledFrittståendeBrevType(frittståendeBrevDto: FrittståendeBrevDto, stønadstype: Stønadstype) =
            when (frittståendeBrevDto.brevType) {
                FrittståendeBrevKategori.INFORMASJONSBREV, FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT ->
                    utledBrevtypeInfobrev(stønadstype)
                FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER -> utledBrevtypeMangelbrev(stønadstype)
                FrittståendeBrevKategori.VARSEL_OM_SANKSJON -> utledBrevtypeSanksjonsbrev(stønadstype)
            }

    private fun utledBrevtypeInfobrev(stønadstype: Stønadstype) =
            when (stønadstype) {
                Stønadstype.OVERGANGSSTØNAD -> FrittståendeBrevType.INFOBREV_OVERGANGSSTØNAD
                Stønadstype.BARNETILSYN -> FrittståendeBrevType.INFOBREV_BARNETILSYN
                Stønadstype.SKOLEPENGER -> FrittståendeBrevType.INFOBREV_SKOLEPENGER
            }

    private fun utledBrevtypeMangelbrev(stønadstype: Stønadstype) =
            when (stønadstype) {
                Stønadstype.OVERGANGSSTØNAD -> FrittståendeBrevType.MANGELBREV_OVERGANGSSTØNAD
                Stønadstype.BARNETILSYN -> FrittståendeBrevType.MANGELBREV_BARNETILSYN
                Stønadstype.SKOLEPENGER -> FrittståendeBrevType.MANGELBREV_SKOLEPENGER
            }

    private fun utledBrevtypeSanksjonsbrev(stønadstype: Stønadstype) =
            when (stønadstype) {
                Stønadstype.OVERGANGSSTØNAD -> FrittståendeBrevType.SANKSJONSBREV_OVERGANGSTØNAD
                Stønadstype.BARNETILSYN -> FrittståendeBrevType.SANKSJONSBREV_BARNETILSYN
                Stønadstype.SKOLEPENGER -> FrittståendeBrevType.SANKSJONSBREV_SKOLEPENGER
            }
}



