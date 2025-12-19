package no.nav.familie.ef.sak.minside

import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.log.IdUtils
import no.nav.familie.log.mdc.MDCConstants
import no.nav.tms.microfrontend.MicrofrontendMessageBuilder
import no.nav.tms.microfrontend.Sensitivitet
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class MinSideKafkaProducerService(
    private val kafkaTemplate: KafkaTemplate<String, String>,
) {
    @Value("\${MIN_SIDE_TOPIC}")
    lateinit var topic: String
    private val logger = Logg.getLogger(this::class)

    fun aktiver(personIdent: String) {
        val melding =
            MicrofrontendMessageBuilder
                .enable {
                    ident = personIdent
                    initiatedBy = "teamfamilie"
                    microfrontendId = "familie-ef-mikrofrontend-minside"
                    sensitivitet = Sensitivitet.HIGH
                }.text()
        val callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
        logger.info("Sender aktivere minside melding for callId=$callId")
        kafkaTemplate.send(topic, callId, melding)
    }

    fun deaktiver(personIdent: String) {
        val melding =
            MicrofrontendMessageBuilder
                .disable {
                    ident = personIdent
                    initiatedBy = "teamfamilie"
                    microfrontendId = "familie-ef-mikrofrontend-minside"
                }.text()
        val callId = MDC.get(MDCConstants.MDC_CALL_ID) ?: IdUtils.generateId()
        logger.info("Sender deaktivere minside melding for callId=$callId")
        kafkaTemplate.send(topic, callId, melding)
    }
}
