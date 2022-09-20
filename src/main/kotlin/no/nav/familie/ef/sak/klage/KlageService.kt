package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.klage.dto.KlagebehandlingerDto
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerInfotrygdDto
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KlageService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val klageClient: KlageClient,
    private val infotrygdService: InfotrygdService,
    private val arbeidsfordelingService: ArbeidsfordelingService
) {

    fun hentBehandlinger(fagsakPersonId: UUID): KlagebehandlingerDto {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)
        val eksternFagsakIder = fagsaker.let {
            listOfNotNull(
                it.overgangsstønad?.eksternId?.id,
                it.barnetilsyn?.eksternId?.id,
                it.skolepenger?.eksternId?.id
            )
        }
        if (eksternFagsakIder.isEmpty()) {
            return KlagebehandlingerDto(emptyList(), emptyList(), emptyList())
        }
        val klagebehandlingerPåEksternId = klageClient.hentKlagebehandlinger(eksternFagsakIder.toSet())
        return KlagebehandlingerDto(
            overgangsstønad = klagebehandlingerPåEksternId[fagsaker.overgangsstønad?.eksternId?.id] ?: emptyList(),
            barnetilsyn = klagebehandlingerPåEksternId[fagsaker.barnetilsyn?.eksternId?.id] ?: emptyList(),
            skolepenger = klagebehandlingerPåEksternId[fagsaker.skolepenger?.eksternId?.id] ?: emptyList()
        )
    }

    fun opprettKlage(behandlingId: UUID, opprettKlageDto: OpprettKlageDto) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        val enhetId = arbeidsfordelingService.hentNavEnhet(aktivIdent)?.enhetId
        brukerfeilHvis(enhetId == null) {
            "Finner ikke behandlende enhet for personen"
        }
        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivIdent,
                stønadstype = Stønadstype.fraEfStønadstype(behandling.stønadstype),
                eksternBehandlingId = behandling.eksternId.toString(),
                eksternFagsakId = behandling.eksternFagsakId.toString(),
                fagsystem = Fagsystem.EF,
                klageMottatt = opprettKlageDto.mottattDato,
                behandlendeEnhet = enhetId
            )
        )
    }

    fun hentÅpneKlagerInfotrygd(fagsakPersonId: UUID): ÅpneKlagerInfotrygdDto {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        return hentÅpneKlagerFraInfotrygd(fagsakPerson)
    }

    private fun hentÅpneKlagerFraInfotrygd(fagsakPerson: FagsakPerson): ÅpneKlagerInfotrygdDto {
        return infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()).map { it.stønadType }
            .let { ÅpneKlagerInfotrygdDto(stønadstyper = it.toSet()) }
    }
}
