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
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * Typet task-nøkkel for skyggekjøringen. Erstatter task-typenavnet den gamle
 * `@TaskStepBeskrivelse`-annotasjonen (`no.nav.familie.prosessering`) tidligere bar.
 */
val SKYGGEKJØR_INFOTRYGD_TASK_TYPE: TaskType<SkyggeInfotrygdPayload> =
    TaskType(
        navn = "skyggekjørInfotrygd",
        serialiser = { jsonMapper.writeValueAsString(it) },
        deserialiser = { jsonMapper.readValue<SkyggeInfotrygdPayload>(it) },
    )

/**
 * Skyggekjører kall som allerede er gjort mot familie-ef-infotrygd (on-prem) på nytt mot familie-ef-infotrygd-replika (GCP),
 * for å verifisere at migreringen til GCP gir identiske svar.
 *
 * Tasken oppretter IKKE selv kallet mot on-prem - den mottar requesten og fasitsvaret (on-prem-responsen) som ble hentet
 * i [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient], gjør det samme kallet mot GCP-replikaen, og sammenligner
 * responsene. Ved avvik logges det en error (med detaljer i secureLogger siden responsene kan inneholde personopplysninger)
 * og tasken feiler slik at avviket blir synlig og kan følges opp manuelt.
 *
 * **Første pilot av `efterlatte-prosessering` i familie-ef-sak** (se
 * `.github/prosessering/05-poc-veikart.md` i efterlatte-prosessering-repoet). Denne
 * tasken ble valgt fordi den kun logger og sammenligner - ingen andre sideeffekter -
 * og er derfor en trygg kandidat for å prøve det nye biblioteket ved siden av den
 * gamle `no.nav.familie.prosessering`-motoren, som resten av appen fortsatt bruker.
 * Tasken opprettes uten noen forretningstransaksjon å henge outbox-garantien på, se
 * `opprettIEgenTransaksjon` i [no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient].
 *
 * Merk: retry-antall og backoff er foreløpig satt engine-globalt (for *alle*
 * task-typer på denne motoren), ikke per task-type slik den gamle
 * `@TaskStepBeskrivelse(maxAntallFeil, triggerTidVedFeilISekunder, ...)` gjorde.
 * Motoren i denne appen kjører bare denne ene task-typen foreløpig, så det er
 * uproblematisk nå - se åpen tråd i `04-outbox-api.md`.
 */
@Component
class SkyggekjørInfotrygdTask(
    private val infotrygdReplikaGcpClient: InfotrygdReplikaGcpClient,
) : TaskStep<SkyggeInfotrygdPayload> {
    private val logger = LoggerFactory.getLogger(javaClass)

    override val type = SKYGGEKJØR_INFOTRYGD_TASK_TYPE

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
}

/** Kastes ved avvik i [SkyggekjørInfotrygdTask]. Uten stacktrace - avviket er allerede fullt beskrevet i loggmeldingen. */
class SkyggekjøringAvvikException(
    melding: String,
) : RuntimeException(melding, null, false, false)

data class SkyggeInfotrygdPayload(
    val operasjon: SkyggeInfotrygdOperasjon,
    val request: String,
    val forventetRespons: String,
    val personIdenter: Set<String>,
    val timestamp: LocalDateTime = LocalDateTime.now(),
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
