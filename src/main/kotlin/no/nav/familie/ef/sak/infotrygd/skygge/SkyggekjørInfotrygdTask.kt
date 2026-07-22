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
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Skyggekjører kall som allerede er gjort mot familie-ef-infotrygd (on-prem) på nytt mot familie-ef-infotrygd-replika (GCP),
 * for å verifisere at migreringen til GCP gir identiske svar.
 *
 * Tasken oppretter IKKE selv kallet mot on-prem - den mottar requesten og fasitsvaret (on-prem-responsen) som ble hentet
 * i [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient], gjør det samme kallet mot GCP-replikaen, og sammenligner
 * responsene. Ved avvik logges det en error (med detaljer i secureLogger siden responsene kan inneholde personopplysninger)
 * og tasken feiler slik at avviket blir synlig og kan følges opp manuelt.
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
        val payload = kontekst.payload
        when (payload.operasjon) {
            SkyggeInfotrygdOperasjon.HENT_PERIODER -> {
                sammenlign(
                    payload = payload,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentPerioder(jsonMapper.readValue<InfotrygdPeriodeRequest>(payload.request)) },
                    normaliser = InfotrygdPeriodeResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_SAMMENSLÅTTE_PERIODER -> {
                sammenlign(
                    payload = payload,
                    faktiskRespons = {
                        infotrygdReplikaGcpClient.hentSammenslåttePerioder(jsonMapper.readValue<InfotrygdPeriodeRequest>(payload.request))
                    },
                    normaliser = InfotrygdPeriodeResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_SAKER -> {
                sammenlign(
                    payload = payload,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentSaker(jsonMapper.readValue<InfotrygdSøkRequest>(payload.request)) },
                    normaliser = InfotrygdSakResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_INNSLAG_HOS_INFOTRYGD -> {
                sammenlign(
                    payload = payload,
                    faktiskRespons = {
                        infotrygdReplikaGcpClient.hentInslagHosInfotrygd(jsonMapper.readValue<InfotrygdSøkRequest>(payload.request))
                    },
                    normaliser = InfotrygdFinnesResponse::normalisert,
                )
            }
        }
    }

    private inline fun <reified T> sammenlign(
        payload: SkyggeInfotrygdPayload,
        faktiskRespons: () -> T,
        normaliser: (T) -> T,
    ) {
        val forventet = normaliser(jsonMapper.readValue<T>(payload.forventetRespons))
        val faktisk = normaliser(faktiskRespons())

        if (forventet != faktisk) {
            secureLogger.error(
                "Skyggekjøring av ${payload.operasjon} feilet - avvik mellom familie-ef-infotrygd (on-prem) og " +
                    "familie-ef-infotrygd-replika (GCP).\nrequest=${payload.request}\nonPrem=$forventet\ngcp=$faktisk",
            )
            logger.error(
                "Skyggekjøring av ${payload.operasjon} feilet - responsen fra familie-ef-infotrygd-replika (GCP) er " +
                    "ulik responsen fra familie-ef-infotrygd (on-prem). Se secureLogger for detaljer.",
            )
            throw SkyggekjøringAvvikException(
                "Skyggekjøring av ${payload.operasjon} feilet - avvik mellom on-prem og GCP-replika for infotrygd. Se securelogger for detaljer.",
            )
        }
    }

    companion object {
        const val TYPE_NAVN = "skyggekjørInfotrygd"

        val TYPE: TaskType<SkyggeInfotrygdPayload> =
            TaskType(
                navn = TYPE_NAVN,
                serialiser = { jsonMapper.writeValueAsString(it) },
                deserialiser = { jsonMapper.readValue<SkyggeInfotrygdPayload>(it) },
            )

        fun opprettPayload(
            operasjon: SkyggeInfotrygdOperasjon,
            request: Any,
            forventetRespons: Any,
        ): SkyggeInfotrygdPayload =
            SkyggeInfotrygdPayload(
                operasjon = operasjon,
                request = jsonMapper.writeValueAsString(request),
                forventetRespons = jsonMapper.writeValueAsString(forventetRespons),
            )
    }
}

/**
 * Feil ved avvik mellom on-prem og GCP-replika er et forventet forretningsutfall (ikke en programmeringsfeil),
 * så stacktracen fylles bevisst ikke ut - detaljene ligger allerede i secureLogger/logger over.
 */
private class SkyggekjøringAvvikException(
    message: String,
) : RuntimeException(message) {
    override fun fillInStackTrace(): Throwable = this
}

data class SkyggeInfotrygdPayload(
    val operasjon: SkyggeInfotrygdOperasjon,
    val request: String,
    val forventetRespons: String,
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
