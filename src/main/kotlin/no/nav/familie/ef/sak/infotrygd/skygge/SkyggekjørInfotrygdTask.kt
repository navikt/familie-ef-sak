package no.nav.familie.ef.sak.infotrygd.skygge

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaGcpClient
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Base64

/**
 * Skyggekjører kall som allerede er gjort mot familie-ef-infotrygd (on-prem) på nytt mot familie-ef-infotrygd-replika (GCP),
 * for å verifisere at migreringen til GCP gir identiske svar.
 *
 * Pilot for det nye, transaksjonelle outbox-baserte task-rammeverket no.nav.efterlatte:prosessering-* (se
 * [SkyggekjøringTaskLagrer] for hvordan tasken opprettes, og [no.nav.familie.ef.sak.infrastruktur.config.EfterlatteProsesseringConfig]
 * for oppsettet). Andre tasks i familie-ef-sak kjører fortsatt på det etablerte no.nav.familie.prosessering.
 */
@Service
class SkyggekjørInfotrygdTask(
    private val infotrygdReplikaGcpClient: InfotrygdReplikaGcpClient,
) : TaskStep<SkyggeInfotrygdPayload> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type: TaskType<SkyggeInfotrygdPayload> = TYPE

    override fun utfor(kontekst: TaskKontekst<SkyggeInfotrygdPayload>) {
        val request = kontekst.task.metadata.hentDekodetMetadata(METADATA_REQUEST)
        val forventetRespons = kontekst.task.metadata.hentDekodetMetadata(METADATA_FORVENTET_RESPONS)

        when (kontekst.payload.operasjon) {
            SkyggeInfotrygdOperasjon.HENT_PERIODER -> {
                sammenlign(
                    operasjon = kontekst.payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentPerioder(jsonMapper.readValue<InfotrygdPeriodeRequest>(request)) },
                    normaliser = InfotrygdPeriodeResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_SAMMENSLÅTTE_PERIODER -> {
                sammenlign(
                    operasjon = kontekst.payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = {
                        infotrygdReplikaGcpClient.hentSammenslåttePerioder(jsonMapper.readValue<InfotrygdPeriodeRequest>(request))
                    },
                    normaliser = InfotrygdPeriodeResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_SAKER -> {
                sammenlign(
                    operasjon = kontekst.payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentSaker(jsonMapper.readValue<InfotrygdSøkRequest>(request)) },
                    normaliser = InfotrygdSakResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_INNSLAG_HOS_INFOTRYGD -> {
                sammenlign(
                    operasjon = kontekst.payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = {
                        infotrygdReplikaGcpClient.hentInslagHosInfotrygd(jsonMapper.readValue<InfotrygdSøkRequest>(request))
                    },
                    normaliser = InfotrygdFinnesResponse::normalisert,
                )
            }
        }
    }

    private inline fun <reified T> sammenlign(
        operasjon: SkyggeInfotrygdOperasjon,
        request: String,
        forventetRespons: String,
        faktiskRespons: () -> T,
        normaliser: (T) -> T,
    ) {
        val forventet = normaliser(jsonMapper.readValue<T>(forventetRespons))
        val faktisk = normaliser(faktiskRespons())

        if (forventet != faktisk) {
            secureLogger.error(
                "Skyggekjøring av $operasjon feilet - avvik mellom familie-ef-infotrygd (on-prem) og " +
                    "familie-ef-infotrygd-replika (GCP).\nrequest=$request\nonPrem=$forventet\ngcp=$faktisk",
            )
            logger.error(
                "Skyggekjøring av $operasjon feilet - responsen fra familie-ef-infotrygd-replika (GCP) er " +
                    "ulik responsen fra familie-ef-infotrygd (on-prem). Se secureLogger for detaljer.",
            )
            throw TaskExceptionUtenStackTrace(
                "Skyggekjøring av $operasjon feilet - avvik mellom on-prem og GCP-replika for infotrygd. Se securelogger for detaljer.",
            )
        }
    }

    companion object {
        private const val TASK_TYPE_NAVN = "skyggekjørInfotrygd"
        private const val METADATA_REQUEST = "request"
        private const val METADATA_FORVENTET_RESPONS = "forventetRespons"

        val TYPE: TaskType<SkyggeInfotrygdPayload> =
            TaskType(
                navn = TASK_TYPE_NAVN,
                serialiser = { jsonMapper.writeValueAsString(it) },
                deserialiser = { jsonMapper.readValue<SkyggeInfotrygdPayload>(it) },
            )

        fun opprettPayload(
            operasjon: SkyggeInfotrygdOperasjon,
            personIdenter: Set<String>,
        ): SkyggeInfotrygdPayload =
            SkyggeInfotrygdPayload(
                operasjon = operasjon,
                personIdenter = personIdenter.sorted(),
            )

        fun opprettMetadata(
            request: Any,
            forventetRespons: Any,
        ): Map<String, String> =
            mapOf(
                METADATA_REQUEST to jsonMapper.writeValueAsString(request).kodeBase64(),
                METADATA_FORVENTET_RESPONS to jsonMapper.writeValueAsString(forventetRespons).kodeBase64(),
            )

        private fun String.kodeBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))

        private fun Map<String, String>.hentDekodetMetadata(navn: String): String = String(Base64.getDecoder().decode(this.getValue(navn)), Charsets.UTF_8)
    }
}

data class SkyggeInfotrygdPayload(
    val operasjon: SkyggeInfotrygdOperasjon,
    val personIdenter: List<String>,
)

enum class SkyggeInfotrygdOperasjon {
    HENT_PERIODER,
    HENT_SAMMENSLÅTTE_PERIODER,
    HENT_SAKER,
    HENT_INNSLAG_HOS_INFOTRYGD,
}

/**
 * Perioder/saker/treff kan i praksis komme i ulik rekkefølge fra on-prem og GCP-replikaen uten at det er et reelt avvik,
 * så listene sorteres på en stabil, innholdsbasert nøkkel før sammenligning.
 */
private fun InfotrygdPeriodeResponse.normalisert(): InfotrygdPeriodeResponse =
    copy(
        overgangsstønad = overgangsstønad.sortedBy { it.toString() },
        barnetilsyn = barnetilsyn.sortedBy { it.toString() },
        skolepenger = skolepenger.sortedBy { it.toString() },
    )

private fun InfotrygdSakResponse.normalisert(): InfotrygdSakResponse = copy(saker = saker.sortedBy { it.toString() })

private fun InfotrygdFinnesResponse.normalisert(): InfotrygdFinnesResponse =
    copy(
        vedtak = vedtak.sortedBy { it.toString() },
        saker = saker.sortedBy { it.toString() },
    )
