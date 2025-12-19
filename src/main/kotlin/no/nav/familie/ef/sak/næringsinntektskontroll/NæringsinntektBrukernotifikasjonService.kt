package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NæringsinntektBrukernotifikasjonService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    @Value("\${BRUKERNOTIFIKASJON_VARSEL_TOPIC}")
    val topic: String,
    @Value("\${NAIS_APP_NAME}")
    val applicationName: String,
    @Value("\${NAIS_NAMESPACE}")
    val namespace: String,
    @Value("\${NAIS_CLUSTER_NAME}")
    val cluster: String,
) {
    private val logger = Logg.getLogger(this::class)

    fun sendBeskjedTilBruker(
        personIdent: String,
        behandlingId: UUID,
        melding: String,
    ) {
        val generertVarselId = UUID.randomUUID().toString()
        val opprettVarsel =
            VarselActionBuilder.opprett {
                type = Varseltype.Beskjed
                varselId = generertVarselId
                sensitivitet = Sensitivitet.High
                ident = personIdent
                tekst =
                    Tekst(
                        spraakkode = "nb",
                        tekst = melding,
                        default = true,
                    )
                produsent = Produsent(cluster, namespace, applicationName)
            }

        logger.info("Sender til Kafka topic: {}: {}", topic, opprettVarsel)
        runCatching {
            val producerRecord = ProducerRecord(topic, generertVarselId, opprettVarsel)
            kafkaTemplate.send(producerRecord).get()
        }.onFailure {
            val errorMessage = "Kunne ikke sende brukernotifikasjon til topic: $topic. Se secure logs for mer informasjon."
            logger.error(errorMessage)
            logger.error("Kunne ikke sende brukernotifikasjon til topic: {}", topic, it)
            throw RuntimeException(errorMessage)
        }
    }
}
