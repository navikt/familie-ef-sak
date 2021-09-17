package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping(path = ["/api/blankett"])
@ProtectedWithClaims(issuer = "azuread")
@Unprotected
class BlankettController(private val tilgangService: TilgangService,
                         private val blankettService: BlankettService,
                         private val behandlingService: BehandlingService,
                         private val oppgaveService: OppgaveService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @GetMapping("{behandlingId}")
    fun hentBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return blankettService.hentBlankettPdf(behandlingId)?.let {
            Ressurs.success(it.pdf.bytes)
        } ?: lagBlankettPdf(behandlingId)
    }

    @PostMapping("/oppgave/{oppgaveId}")
    fun startBlankettBehandling(@PathVariable oppgaveId: Long): Ressurs<UUID> {
        oppgaveService.hentEfOppgave(oppgaveId)?.let {
            return Ressurs.success(it.behandlingId)
        }
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val journalpostId = oppgave.journalpostId
        require(journalpostId != null) { "For å plukke oppgaven må det eksistere en journalpostId" }

        val behandling = blankettService.opprettBlankettBehandling(journalpostId, oppgaveId)

        return Ressurs.success(behandling.id)
    }

    private fun lagBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerOpprettelseAvBlankett(behandling)
        val blankett = blankettService.lagBlankett(behandlingId)
        return Ressurs.success(blankett)
    }

    private fun validerOpprettelseAvBlankett(behandling: Behandling) {
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            kastApiFeil("Behandling er låst for videre redigering for behandling : ${behandling}", HttpStatus.BAD_REQUEST)
        }
        if (!typeBlankett(behandling)) {
            kastApiFeil("Behandling er ikke av typen blankett for behandling : ${behandling}", HttpStatus.BAD_REQUEST)
        }
    }

    private fun typeBlankett(behandling: Behandling): Boolean {
        return behandling.type == BehandlingType.BLANKETT
    }

    private fun kastApiFeil(feilmelding: String, httpStatus: HttpStatus) {
        logger.error(feilmelding)
        throw ApiFeil(feilmelding, httpStatus)
    }
}
