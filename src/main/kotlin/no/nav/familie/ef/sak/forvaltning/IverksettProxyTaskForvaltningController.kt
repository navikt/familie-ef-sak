package no.nav.familie.ef.sak.forvaltning

import io.swagger.v3.oas.annotations.Operation
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.HarRolleForvalter
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@RestController
@RequestMapping("/api/forvaltning/iverksett/task/")
class IverksettProxyTaskForvaltningController(
    private val tilgangService: TilgangService,
    @Value("\${FAMILIE_EF_IVERKSETT_URL}")
    private val familieEfIverksettUri: String,
    @Qualifier("iverksettRestClient")
    private val restClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @HarRolleForvalter
    @PostMapping("kopier/taskid/{taskId}")
    @Operation(
        description =
            "Kopierer task i iverksett med id angitt taskId. Tasken i iverksett må ha status MANUELL_OPPFØLGING. Tasken som kopieres kan ikke re-kjøres da payload etter kopiering kun vil inneholde LocalDateTime. Denne må avvikshåndteres manuelt. " +
                "Tasken som kopieres vil ha samme metadata som tasken som kopieres",
        summary =
            "NB! Dette gjelder task i Iverksett, ikke i ef-sak. " +
                "Brukes for å opprette en task-kopi",
    )
    fun restartIverksettTask(
        @PathVariable taskId: Long,
    ): KopiertTaskResponse {
        val url = URI.create("$familieEfIverksettUri/api/forvaltning/task/restart/$taskId")
        val postForEntity =
            restClient
                .post()
                .uri(url)
                .body("")
                .retrieve()
                .body<KopiertTaskResponse>()!!
        logger.info("Kopiert task med id ${postForEntity.fraTaskId} -> ${postForEntity.tilNyTaskId}")
        return postForEntity
    }

    data class KopiertTaskResponse(
        val fraTaskId: Long,
        val tilNyTaskId: Long,
    )
}
