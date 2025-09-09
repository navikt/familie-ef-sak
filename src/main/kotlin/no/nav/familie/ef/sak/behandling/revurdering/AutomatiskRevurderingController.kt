package no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping(path = ["/api/automatisk-revurdering"])
@ProtectedWithClaims(issuer = "azuread")
class AutomatiskRevurderingController(
    private val taskService: TaskService,
) {
    @PostMapping
    fun forsøkAutomatiskRevurdering(
        @RequestBody personIdenter: List<String>,
    ) {
        val task = OpprettAutomatiskRevurderingTask.opprettTask(PayloadOpprettAutomatiskRevurderingTask(personIdenter, YearMonth.now()))
        taskService.save(task)
    }

    @PostMapping("/forvaltning")
    fun forsøkAutomatiskRevurderingForvaltning(
        @RequestBody personIdenter: List<String>,
    ) {
        val task = OpprettAutomatiskRevurderingFraForvaltningTask.opprettTask(PayloadOpprettAutomatiskRevurderingFraForvaltningTask(personIdenter, YearMonth.now()))
        taskService.save(task)
    }
}
