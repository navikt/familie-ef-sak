package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.dto.ManueltBrevDto
import no.nav.familie.ef.sak.brev.dto.ManueltBrevRequestDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class ManueltBrevService(private val brevClient: BrevClient,
                         private val brevRepository: VedtaksbrevRepository,
                         private val behandlingService: BehandlingService,
                         private val fagsakService: FagsakService,
                         private val personopplysningerService: PersonopplysningerService) {

    fun lagManueltBrev(manueltBrevDto: ManueltBrevDto): ByteArray {
        val ident = if (manueltBrevDto.fagsakId != null) {
            fagsakService.hentAktivIdent(UUID.fromString(manueltBrevDto.fagsakId))
        } else if (manueltBrevDto.behandlingId != null) {
            behandlingService.hentAktivIdent(
                    UUID.fromString(manueltBrevDto.behandlingId))
        } else {
            throw Feil("LagManueltBrev må enten ha fagsakId eller behandlingID. Begge var null.")
        }
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(ident))
        val request = ManueltBrevRequestDto(overskrift = manueltBrevDto.overskrift,
                                            avsnitt = manueltBrevDto.avsnitt,
                                            ident = ident,
                                            navn = navn[ident]!!,
                                            brevdato = LocalDate.now(),
                                            saksbehandlersignatur = SikkerhetContext.hentSaksbehandlerNavn(true)
            )

        return brevClient.lagManueltBrev(request)
    }

}
