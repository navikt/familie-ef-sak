package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.tms.varsel.action.Produsent
import no.nav.tms.varsel.action.Sensitivitet
import no.nav.tms.varsel.action.Tekst
import no.nav.tms.varsel.action.Varseltype
import no.nav.tms.varsel.builder.VarselActionBuilder
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class NæringsinntektBrukernotifikasjonService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value("\${BRUKERNOTIFIKASJON_BESKJED_TOPIC}")
    lateinit var topic: String

    @Value("\$NAIS_APP_NAME")
    lateinit var applicationName: String
    @Value("\$NAIS_NAMESPACE")
    lateinit var namespace: String
    @Value("\$NAIS_CLUSTER")
    lateinit var cluster: String

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

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

        secureLogger.info("Sender til Kafka topic: {}: {}", topic, opprettVarsel)
        runCatching {
            val producerRecord = ProducerRecord(topic, generertVarselId, opprettVarsel)
            kafkaTemplate.send(producerRecord).get()
        }.onFailure {
            val errorMessage = "Kunne ikke sende brukernotifikasjon til topic: $topic. Se secure logs for mer informasjon."
            logger.error(errorMessage)
            secureLogger.error("Kunne ikke sende brukernotifikasjon til topic: {}", topic, it)
            throw RuntimeException(errorMessage)
        }
    }
}
