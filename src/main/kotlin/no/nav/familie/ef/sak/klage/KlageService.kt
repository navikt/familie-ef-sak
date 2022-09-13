package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerInfotrygd
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.ef.StønadType
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
    private val infotrygdService: InfotrygdService
) {

    fun opprettKlage(behandlingId: UUID, opprettKlageDto: OpprettKlageDto) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                aktivIdent,
                Stønadstype.fraEfStønadstype(behandling.stønadstype),
                behandling.eksternId.toString(),
                behandling.eksternFagsakId.toString(),
                Fagsystem.EF,
                opprettKlageDto.mottattDato
            )
        )
    }

    fun harÅpenKlage(fagsakPersonId: UUID): ÅpneKlagerDto {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        return ÅpneKlagerDto(infotrygd = hentÅpneKlagerFraInfotrygd(fagsakPerson))
    }

    private fun hentÅpneKlagerFraInfotrygd(fagsakPerson: FagsakPerson): ÅpneKlagerInfotrygd {
        return infotrygdService.hentÅpneKlagesaker(fagsakPerson.hentAktivIdent()).groupBy { it.stønadType }
            .map { it.key to it.value.isNotEmpty() }.toMap()
            .let {
                ÅpneKlagerInfotrygd(
                    overgangsstønad = it[StønadType.OVERGANGSSTØNAD] ?: false,
                    barnetilsyn = it[StønadType.BARNETILSYN] ?: false,
                    skolepenger = it[StønadType.SKOLEPENGER] ?: false
                )
            }
    }
}
