package no.nav.familie.ef.sak.infrastruktur.prosessering

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.felles.jsonMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
@Profile("!prod")
class FeilbarDemoTask : TaskStep<FeilbarDemoPayload> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: TaskType<FeilbarDemoPayload> = TYPE

    override fun utfor(kontekst: TaskKontekst<FeilbarDemoPayload>) {
        val payload = kontekst.payload

        if (Instant.now().isBefore(payload.simulertOppeFra)) {
            throw IllegalStateException(
                "Demo: den simulerte nedstrøms-avhengigheten er nede (demoId=${payload.demoId}, " +
                    "oppe fra ${payload.simulertOppeFra}). Tasken feiler med vilje - kjør den på nytt " +
                    "etter at avhengigheten er oppe igjen.",
            )
        }

        logger.info("Demo: task fullført (demoId=${payload.demoId}) - den simulerte avhengigheten er oppe igjen")
    }

    companion object {
        private const val TASK_TYPE_NAVN = "feilbarDemo"

        val TYPE: TaskType<FeilbarDemoPayload> =
            TaskType(
                navn = TASK_TYPE_NAVN,
                serialiser = { jsonMapper.writeValueAsString(it) },
                deserialiser = { jsonMapper.readValue<FeilbarDemoPayload>(it) },
            )

        fun opprettPayload(vinduSekunder: Long): FeilbarDemoPayload =
            FeilbarDemoPayload(
                demoId = UUID.randomUUID().toString(),
                simulertOppeFra = Instant.now().plusSeconds(vinduSekunder),
            )
    }
}

data class FeilbarDemoPayload(
    val demoId: String,
    val simulertOppeFra: Instant,
)
