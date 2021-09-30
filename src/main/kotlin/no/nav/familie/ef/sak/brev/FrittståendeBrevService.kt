package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.domain.FRITEKST
import no.nav.familie.ef.sak.brev.domain.Vedtaksbrev
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as FrittståendeBrevDtoIverksetting
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class FrittståendeBrevService(private val brevClient: BrevClient,
                              private val brevRepository: VedtaksbrevRepository,
                              private val behandlingService: BehandlingService,
                              private val fagsakService: FagsakService,
                              private val personopplysningerService: PersonopplysningerService) {

    fun lagFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto): ByteArray {
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto)

        val vedtaksbrev = Vedtaksbrev(behandlingId = UUID.randomUUID(),
                                      saksbehandlerBrevrequest = objectMapper.writeValueAsString(request),
                                      brevmal = FRITEKST,
                                      saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn(true),
                                      besluttersignatur = null,
                                      beslutterPdf = null)

        return brevClient.genererBrev(vedtaksbrev = vedtaksbrev)
    }

    private fun lagFrittståendeBrevRequest(frittståendeBrevDto: FrittståendeBrevDto): FrittståendeBrevRequestDto {
        val ident = fagsakService.hentAktivIdent(frittståendeBrevDto.fagsakId)
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(ident))
        val request = FrittståendeBrevRequestDto(overskrift = frittståendeBrevDto.overskrift,
                                                 avsnitt = frittståendeBrevDto.avsnitt,
                                                 personIdent = ident,
                                                 navn = navn.getValue(ident),
                                                 brevdato = LocalDate.now())
        return request
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto)
        val brev = brevClient.genererBrev(request, SikkerhetContext.hentSaksbehandlerNavn(true))

    }


}
