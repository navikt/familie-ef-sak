package no.nav.familie.ef.sak.infotrygd.skygge

import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaGcpClient
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSøkRequest
import no.nav.familie.kontrakter.felles.jsonMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Properties

/**
 * Skyggekjører kall som allerede er gjort mot familie-ef-infotrygd (on-prem) på nytt mot familie-ef-infotrygd-replika (GCP),
 * for å verifisere at migreringen til GCP gir identiske svar.
 *
 * Tasken oppretter IKKE selv kallet mot on-prem - den mottar requesten og fasitsvaret (on-prem-responsen) som ble hentet
 * i [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient], gjør det samme kallet mot GCP-replikaen, og sammenligner
 * responsene. Ved avvik logges det en error (med detaljer i secureLogger siden responsene kan inneholde personopplysninger)
 * og tasken feiler slik at avviket blir synlig og kan følges opp manuelt.
 */
@Service
@TaskStepBeskrivelse(
    taskStepType = SkyggekjørInfotrygdTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 60L,
    beskrivelse = "Skyggekjører kall mot familie-ef-infotrygd-replika (GCP) og sammenligner med fasitsvar fra familie-ef-infotrygd (on-prem)",
)
class SkyggekjørInfotrygdTask(
    private val infotrygdReplikaGcpClient: InfotrygdReplikaGcpClient,
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        val payload = jsonMapper.readValue<SkyggeInfotrygdPayload>(task.payload)
        val request = task.metadata.hentDekodetProperty(METADATA_REQUEST)
        val forventetRespons = task.metadata.hentDekodetProperty(METADATA_FORVENTET_RESPONS)
        when (payload.operasjon) {
            SkyggeInfotrygdOperasjon.HENT_PERIODER -> {
                sammenlign(
                    operasjon = payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentPerioder(jsonMapper.readValue<InfotrygdPeriodeRequest>(request)) },
                    normaliser = InfotrygdPeriodeResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_SAMMENSLÅTTE_PERIODER -> {
                sammenlign(
                    operasjon = payload.operasjon,
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
                    operasjon = payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentSaker(jsonMapper.readValue<InfotrygdSøkRequest>(request)) },
                    normaliser = InfotrygdSakResponse::normalisert,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_INNSLAG_HOS_INFOTRYGD -> {
                sammenlign(
                    operasjon = payload.operasjon,
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
        const val TYPE = "skyggekjørInfotrygd"
        private const val METADATA_REQUEST = "request"
        private const val METADATA_FORVENTET_RESPONS = "forventetRespons"

        fun opprettTask(
            operasjon: SkyggeInfotrygdOperasjon,
            request: Any,
            forventetRespons: Any,
            personIdenter: Set<String>,
        ): Task {
            val sortertePersonIdenter = personIdenter.sorted()
            val payload =
                SkyggeInfotrygdPayload(
                    operasjon = operasjon,
                    personIdenter = sortertePersonIdenter,
                )
            return Task(
                type = TYPE,
                payload = jsonMapper.writeValueAsString(payload),
                properties =
                    Properties().apply {
                        this["operasjon"] = operasjon.name
                        this["personIdenter"] = sortertePersonIdenter.joinToString(",")
                        this[METADATA_REQUEST] = jsonMapper.writeValueAsString(request).kodeBase64()
                        this[METADATA_FORVENTET_RESPONS] = jsonMapper.writeValueAsString(forventetRespons).kodeBase64()
                    },
            )
        }

        private fun String.kodeBase64(): String = Base64.getEncoder().encodeToString(this.toByteArray(Charsets.UTF_8))

        private fun Properties.hentDekodetProperty(navn: String): String = String(Base64.getDecoder().decode(this.getProperty(navn)), Charsets.UTF_8)
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
