package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.ApiFeil
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.TilgangService
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

    @PostMapping("{behandlingId}")
    fun lagBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        if (behandling.status.behandlingErLåstForVidereRedigering()) {
            kastApiFeil("Behandling er låst for videre redigering for behandling : ${behandling}", HttpStatus.BAD_REQUEST)
        }
        if (!typeBlankett(behandling)) {
            kastApiFeil("Behandling er ikke av typen blankett for behandling : ${behandling}", HttpStatus.BAD_REQUEST)
        }
        val blankett = blankettService.lagBlankett(behandlingId)
        return Ressurs.success(blankett)
    }

    @GetMapping("{behandlingId}")
    fun hentBlankettPdf(@PathVariable behandlingId: UUID): Ressurs<ByteArray> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        val blankett = blankettService.hentBlankettPdf(behandlingId)
        if (blankett.isEmpty && behandling.status.behandlingErLåstForVidereRedigering()) {
            kastApiFeil("Kunne ikke finne blankett for behandling : ${behandling}", HttpStatus.NOT_FOUND)
        }
        if (blankett.isEmpty) {
            return lagBlankettPdf(behandlingId)
        }
        return Ressurs.success(blankett.get().pdf.bytes)
    }

    @PostMapping("/oppgave/{oppgaveId}")
    fun opprettBlankettBehandling(@PathVariable oppgaveId: Long): Ressurs<UUID> {
        oppgaveService.hentEfOppgave(oppgaveId)?.let {
            kastApiFeil("Det finnes allerede en behandling for denne oppgaven - kan ikke opprettes på nytt",
                        HttpStatus.BAD_REQUEST)
        }
        val oppgave = oppgaveService.hentOppgave(oppgaveId)
        val journalpostId = oppgave.journalpostId
        require(journalpostId != null) { "For å plukke oppgaven må det eksistere en journalpostId" }

        val behandling = blankettService.opprettBlankettBehandling(journalpostId, oppgaveId)

        return Ressurs.success(behandling.id)
    }

    private fun typeBlankett(behandling: Behandling): Boolean {
        return behandling.type == BehandlingType.BLANKETT
    }

    private fun kastApiFeil(feilmelding: String, httpStatus: HttpStatus) {
        logger.error(feilmelding)
        throw ApiFeil(feilmelding, httpStatus)
    }
}
