package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.revurdering.RevurderingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettet
import no.nav.familie.kontrakter.felles.klage.IkkeOpprettetÅrsak
import no.nav.familie.kontrakter.felles.klage.KanIkkeOppretteRevurderingÅrsak
import no.nav.familie.kontrakter.felles.klage.KanOppretteRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.OpprettRevurderingResponse
import no.nav.familie.kontrakter.felles.klage.Opprettet
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Service
class EksternBehandlingService(
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val revurderingService: RevurderingService,
    private val oppgaveService: OppgaveService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun harLøpendeStønad(personidenter: Set<String>): Boolean {
        val behandlingIDer = hentAlleBehandlingIDer(personidenter)
        val sisteStønadsdato =
            behandlingIDer
                .map(tilkjentYtelseService::hentForBehandling)
                .mapNotNull { it.andelerTilkjentYtelse.maxOfOrNull(AndelTilkjentYtelse::stønadTom) }
                .maxOfOrNull { it } ?: LocalDate.MIN
        return sisteStønadsdato >= LocalDate.now()
    }

    fun harLøpendeBarnetilsyn(personIdent: String): Boolean {
        val fagsakPerson = fagsakPersonService.finnPerson(setOf(personIdent)) ?: return false
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPerson.id)
        val fagsak = fagsaker.barnetilsyn
        return fagsak?.let { fagsakService.erLøpende(fagsak) } ?: false
    }

    private fun hentAlleBehandlingIDer(personidenter: Set<String>): Set<UUID> =
        StønadType
            .values()
            .mapNotNull { fagsakService.finnFagsak(personidenter, it) }
            .mapNotNull { behandlingService.finnSisteIverksatteBehandling(it.id) }
            .map { it.id }
            .toSet()

    @Transactional(readOnly = true)
    fun kanOppretteRevurdering(eksternFagsakId: Long): KanOppretteRevurderingResponse {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)
        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> {
                KanOppretteRevurderingResponse(true, null)
            }

            is KanIkkeOppretteRevurdering -> {
                KanOppretteRevurderingResponse(false, resultat.årsak.kanIkkeOppretteRevurderingÅrsak)
            }
        }
    }

    fun tilhørendeBehandleSakOppgaveErPåbegynt(
        personIdent: String,
        stønadType: StønadType,
        innsendtSøknadTidspunkt: LocalDateTime,
    ): Boolean {
        val fagsak = fagsakService.finnFagsak(setOf(personIdent), stønadType)

        return if (fagsak == null) {
            false
        } else {
            val behandlingerOpprettetEtterSøknadstidspunkt =
                behandlingService.hentBehandlinger(fagsak.id).filter { it.sporbar.opprettetTid > innsendtSøknadTidspunkt }
            val efOppgaver = hentEFOppgaver(behandlingerOpprettetEtterSøknadstidspunkt.map { it.id })
            val oppgaver = hentOppgaver(efOppgaver.map { it.gsakOppgaveId })

            oppgaver.any { it.tilordnetRessurs != null }
        }
    }

    @Transactional
    fun opprettRevurderingKlage(eksternFagsakId: Long): OpprettRevurderingResponse {
        val fagsak = fagsakService.hentFagsakPåEksternId(eksternFagsakId)

        val resultat = utledKanOppretteRevurdering(fagsak)
        return when (resultat) {
            is KanOppretteRevurdering -> {
                opprettRevurdering(fagsak)
            }

            is KanIkkeOppretteRevurdering -> {
                OpprettRevurderingResponse(IkkeOpprettet(resultat.årsak.ikkeOpprettetÅrsak))
            }
        }
    }

    private fun opprettRevurdering(fagsak: Fagsak) =
        try {
            val revurdering =
                RevurderingDto(
                    fagsakId = fagsak.id,
                    behandlingsårsak = BehandlingÅrsak.KLAGE,
                    kravMottatt = LocalDate.now(),
                    vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
                )
            val behandling = revurderingService.opprettRevurderingManuelt(revurdering)
            OpprettRevurderingResponse(Opprettet(behandling.eksternId.toString()))
        } catch (e: Exception) {
            logger.error("Feilet opprettelse av revurdering for fagsak=${fagsak.id}, se secure logg for detaljer")
            secureLogger.error("Feilet opprettelse av revurdering for fagsak=${fagsak.id}", e)
            OpprettRevurderingResponse(IkkeOpprettet(IkkeOpprettetÅrsak.FEIL, e.message))
        }

    private fun utledKanOppretteRevurdering(fagsak: Fagsak): KanOppretteRevurderingResultat {
        val finnesÅpenBehandling = behandlingService.finnesÅpenBehandling(fagsak.id)
        if (finnesÅpenBehandling) {
            return KanIkkeOppretteRevurdering(Årsak.ÅPEN_BEHANDLING)
        }

        if (behandlingService.finnSisteIverksatteBehandling(fagsak.id) == null) {
            return KanIkkeOppretteRevurdering(Årsak.INGEN_BEHANDLING)
        }
        return KanOppretteRevurdering()
    }

    private fun hentEFOppgaver(behandlingIder: List<UUID>) = behandlingIder.mapNotNull { oppgaveService.finnSisteBehandleSakOppgaveForBehandling(it) }

    private fun hentOppgaver(oppgaveIder: List<Long>) = oppgaveIder.map { oppgaveService.hentOppgave(it) }
}

private sealed interface KanOppretteRevurderingResultat

private class KanOppretteRevurdering : KanOppretteRevurderingResultat

private data class KanIkkeOppretteRevurdering(
    val årsak: Årsak,
) : KanOppretteRevurderingResultat

private enum class Årsak(
    val ikkeOpprettetÅrsak: IkkeOpprettetÅrsak,
    val kanIkkeOppretteRevurderingÅrsak: KanIkkeOppretteRevurderingÅrsak,
) {
    ÅPEN_BEHANDLING(IkkeOpprettetÅrsak.ÅPEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.ÅPEN_BEHANDLING),
    INGEN_BEHANDLING(IkkeOpprettetÅrsak.INGEN_BEHANDLING, KanIkkeOppretteRevurderingÅrsak.INGEN_BEHANDLING),
}
