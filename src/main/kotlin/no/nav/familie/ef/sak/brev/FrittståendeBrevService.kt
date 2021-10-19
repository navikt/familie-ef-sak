package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.brev.dto.VedtaksbrevDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as FrittståendeBrevDtoIverksetting

@Service
class FrittståendeBrevService(private val brevClient: BrevClient,
                              private val fagsakService: FagsakService,
                              private val personopplysningerService: PersonopplysningerService,
                              private val arbeidsfordelingService: ArbeidsfordelingService,
                              private val iverksettClient: IverksettClient) {

    fun lagFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto): ByteArray {
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto)

        val vedtaksbrev = VedtaksbrevDto(saksbehandlerBrevrequest = objectMapper.writeValueAsString(request),
                                         brevmal = FRITEKST,
                                         saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn(true),
                                         besluttersignatur = null)

        return brevClient.genererBrev(vedtaksbrev = vedtaksbrev)
    }

    private fun lagFrittståendeBrevRequest(frittståendeBrevDto: FrittståendeBrevDto): FrittståendeBrevRequestDto {
        val ident = fagsakService.hentAktivIdent(frittståendeBrevDto.fagsakId)
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(ident))
        return FrittståendeBrevRequestDto(overskrift = frittståendeBrevDto.overskrift,
                                          avsnitt = frittståendeBrevDto.avsnitt,
                                          personIdent = ident,
                                          navn = navn.getValue(ident),
                                          brevdato = LocalDate.now())
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto)
        val brev = brevClient.genererBrev(request, SikkerhetContext.hentSaksbehandlerNavn(true))
        val ident = fagsakService.hentAktivIdent(frittståendeBrevDto.fagsakId)
        val eksternFagsakId = fagsakService.hentEksternId(frittståendeBrevDto.fagsakId)
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(
                ident)
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(true)
        iverksettClient.sendFrittståendeBrev(FrittståendeBrevDtoIverksetting(personIdent = ident,
                                                                             eksternFagsakId = eksternFagsakId,
                                                                             stønadType = frittståendeBrevDto.stønadType,
                                                                             brevtype = frittståendeBrevDto.brevType,
                                                                             fil = brev,
                                                                             journalførendeEnhet = journalførendeEnhet,
                                                                             saksbehandlerIdent = saksbehandlerIdent))
    }

}
