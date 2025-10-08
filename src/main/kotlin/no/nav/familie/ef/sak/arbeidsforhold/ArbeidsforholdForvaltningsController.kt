package no.nav.familie.ef.sak.arbeidsforhold

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/api/arbeidsforhold"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class ArbeidsforholdController(
    private val taskService: TaskService,
) {
    @PostMapping("LoggArbeidsforholdForPerson")
    @Operation(
        description =
            "Lager en task som logger arbeidsforhold for person - gitt personIdent",
        summary =
            "Logg arbeidsforhold for person",
    )
    fun loggArbeidsforholdForPerson(
        @RequestBody personIdent: String,
    ) {
        val loggArbeidsforholdForPerson = LoggArbeidsforholdForPersonTask.opprettTask(personIdent)
        taskService.save(loggArbeidsforholdForPerson)
    }
}
