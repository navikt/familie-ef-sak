package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.RevurderingService
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class EksternBehandlingService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val revurderingService: RevurderingService
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun harLøpendeStønad(personidenter: Set<String>): Boolean {
        val behandlingIDer = hentAlleBehandlingIDer(personidenter)
        val sisteStønadsdato = behandlingIDer
            .map(tilkjentYtelseService::hentForBehandling)
            .mapNotNull { it.andelerTilkjentYtelse.maxOfOrNull(AndelTilkjentYtelse::stønadTom) }
            .maxOfOrNull { it } ?: LocalDate.MIN
        return sisteStønadsdato >= LocalDate.now()
    }

    private fun hentAlleBehandlingIDer(personidenter: Set<String>): Set<UUID> {
        return StønadType.values().mapNotNull { fagsakService.finnFagsak(personidenter, it) }
            .mapNotNull { behandlingService.finnSisteIverksatteBehandling(it.id) }
            .map { it.id }
            .toSet()
    }

    @Transactional
    fun opprettRevurderingKlage(eksternFagsakId: Long): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        val finnesÅpenBehandling = behandlingService.finnesÅpenBehandling(fagsak.id)
        if (finnesÅpenBehandling) {
            return OpprettRevurderingResponse(IkkeOpprettet(årsak = IkkeOpprettetÅrsak.ÅPEN_BEHANDLING))
        }
        return try {
            val revurdering = RevurderingDto(
                fagsakId = fagsak.id,
                behandlingsårsak = BehandlingÅrsak.KLAGE,
                kravMottatt = LocalDate.now()
            )
            val behandling = revurderingService.opprettRevurderingManuelt(revurdering)
            OpprettRevurderingResponse(Opprettet(behandling.eksternId.id.toString()))
        } catch (e: Exception) {
            logger.error("Feilet opprettelse av revurdering for fagsak=${fagsak.id}, se secure logg for detaljer")
            secureLogger.error("Feilet opprettelse av revurdering for fagsak=${fagsak.id}", e)
            OpprettRevurderingResponse(IkkeOpprettet(IkkeOpprettetÅrsak.FEIL, e.message))
        }
    }
}
