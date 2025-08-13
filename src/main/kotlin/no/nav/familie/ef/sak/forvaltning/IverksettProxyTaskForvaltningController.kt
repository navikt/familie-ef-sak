package no.nav.familie.ef.sak.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.http.client.AbstractRestClient
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestOperations
import java.net.URI

@RestController
@RequestMapping("/api/forvaltning/iverksett/")
@ProtectedWithClaims(issuer = "azuread")
class IverksettProxyTaskForvaltningController(
    private val tilgangService: TilgangService,
    @Value("\${FAMILIE_EF_IVERKSETT_URL}")
    private val familieEfIverksettUri: String,
    @Qualifier("azure")
    private val restOperations: RestOperations,
) : AbstractRestClient(restOperations, "familie.ef.iverksett.forvaltning") {
    @PostMapping("taskid/{taskId}")
    @Operation(
        description =
            "Kopierer task i iverksett med id angitt taskId. Tasken i iverksett må ha status MANUELL_OPPFØLGING.",
        summary =
            "NB! Dette gjelder task i Iverksett, ikke i ef-sak. " +
                "Brukes for å opprette en task-kopi uten plukket-antall",
    )
    fun restartIverksettTask(
        @PathVariable taskId: Long,
    ) : String {
        tilgangService.validerHarForvalterrolle()
        val url = URI.create("$familieEfIverksettUri/api/forvaltning/task/restart/$taskId")
        return postForEntity<Ressurs<String>>(uri = url, payload = "", httpHeaders = textHeaders()).data!!
    }

    private fun textHeaders(): HttpHeaders {
        val httpHeaders = HttpHeaders()
        httpHeaders.add("Content-Type", "text/plain;charset=UTF-8")
        httpHeaders.acceptCharset = listOf(Charsets.UTF_8)
        return httpHeaders
    }
}
