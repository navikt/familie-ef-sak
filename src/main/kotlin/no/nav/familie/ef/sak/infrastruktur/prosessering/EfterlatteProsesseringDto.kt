package no.nav.familie.ef.sak.infrastruktur.prosessering

import efterlatte.prosessering.Status
import efterlatte.prosessering.Task
import java.time.Instant

data class EfterlatteProsesseringTaskDto(
    val id: Long,
    val type: String,
    val status: Status,
    val antallFeil: Int,
    val stoppårsak: String?,
    val triggerTid: Instant,
    val opprettetTid: Instant,
    val plukketTid: Instant?,
    val payload: String?,
    val metadata: Map<String, String>,
)

data class EfterlatteProsesseringOversiktDto(
    val tasks: List<EfterlatteProsesseringTaskDto>,
    val antallPerStatus: Map<Status, Int>,
    val typer: List<String>,
)

data class OpprettFeilbarDemoTaskDto(
    val vinduSekunder: Long = 20,
)

data class FeilbarDemoTaskOpprettetDto(
    val taskId: Long,
    val simulertOppeFra: Instant,
)

fun Task.tilDto(): EfterlatteProsesseringTaskDto =
    EfterlatteProsesseringTaskDto(
        id = id,
        type = type,
        status = status,
        antallFeil = antallFeil,
        stoppårsak = stoppaarsak?.name,
        triggerTid = triggerTid,
        opprettetTid = opprettetTid,
        plukketTid = plukketTid,
        payload = payload,
        metadata = metadata,
    )
