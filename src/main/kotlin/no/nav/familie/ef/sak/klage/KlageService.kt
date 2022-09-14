package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerInfotrygd
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

    fun opprettKlage(behandlingId: UUID, opprettKlageDto: OpprettKlageDto) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        val enhetId = arbeidsfordelingService.hentNavEnhet(aktivIdent)?.enhetId
        brukerfeilHvis(enhetId == null) {
            "Finner ikke behandlende enhet for personen"
        }
        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                aktivIdent,
                Stønadstype.fraEfStønadstype(behandling.stønadstype),
                behandling.eksternId.toString(),
                behandling.eksternFagsakId.toString(),
                Fagsystem.EF,
                opprettKlageDto.mottattDato,
                enhetId
            )
        )
    }

    fun hentÅpneKlagerInfotrygd(fagsakPersonId: UUID): ÅpneKlagerInfotrygd {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        return hentÅpneKlagerFraInfotrygd(fagsakPerson)
    }

    private fun hentÅpneKlagerFraInfotrygd(fagsakPerson: FagsakPerson): ÅpneKlagerInfotrygd {
        return infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()).map { it.stønadType }
            .let { ÅpneKlagerInfotrygd(stønadstyper = it.toSet()) }
    }
}
