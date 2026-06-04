package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.task.BehandlingsstatistikkTask
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/forvaltning/dvh")
@ProtectedWithClaims(issuer = "azuread")
class DvhForvaltningController(
    private val taskService: TaskService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
) {
    @PostMapping(path = ["/sak/ferdigstill"])
    fun sendFerdigstiltBehandlingTilDvh(
        @RequestBody dvhForvaltning: DvhForvaltningDto,
    ) {
        tilgangService.validerHarForvalterrolle()
        val behandling = behandlingService.hentBehandling(dvhForvaltning.behandlingId)
        if (behandling.status != BehandlingStatus.FERDIGSTILT) {
            throw Feil("Behandlingen må være ferdigstilt for at man skal kunne sende ferdig-hendelse til DVH")
        }
        taskService.save(
            BehandlingsstatistikkTask.opprettFerdigTask(
                behandlingId = dvhForvaltning.behandlingId,
                hendelseTidspunkt = dvhForvaltning.hendelsestidspunkt,
                gjeldendeSaksbehandler = dvhForvaltning.gjeldendeSaksbehandler,
            ),
        )
    }

    data class DvhForvaltningDto(
        val behandlingId: UUID,
        val hendelsestidspunkt: LocalDateTime,
        val gjeldendeSaksbehandler: String,
    )
}
