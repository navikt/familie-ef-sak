package no.nav.familie.ef.sak.infotrygd.skygge

import efterlatte.prosessering.TaskKontekst
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.TaskType
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaGcpClient
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.secureLogger
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeRequest
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
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
                    normaliser = InfotrygdPeriodeResponse::trim,
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
                    normaliser = InfotrygdPeriodeResponse::trim,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_SAKER -> {
                sammenlign(
                    operasjon = kontekst.payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = { infotrygdReplikaGcpClient.hentSaker(jsonMapper.readValue<InfotrygdSøkRequest>(request)) },
                    normaliser = InfotrygdSakResponse::trim,
                )
            }

            SkyggeInfotrygdOperasjon.HENT_INNSLAG_HOS_INFOTRYGD -> {
                sammenlign(
                    operasjon = kontekst.payload.operasjon,
                    request = request,
                    forventetRespons = forventetRespons,
                    faktiskRespons = {
                        infotrygdReplikaGcpClient.hentInfotrygdFinnes(jsonMapper.readValue<InfotrygdSøkRequest>(request))
                    },
                    normaliser = InfotrygdFinnesResponse::trim,
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

private fun InfotrygdPeriodeResponse.trim(): InfotrygdPeriodeResponse =
    copy(
        overgangsstønad = overgangsstønad.normalisertePerioder(),
        barnetilsyn = barnetilsyn.normalisertePerioder(),
        skolepenger = skolepenger.normalisertePerioder(),
    )

private fun List<InfotrygdPeriode>.normalisertePerioder(): List<InfotrygdPeriode> = map { it.copy(barnIdenter = it.barnIdenter.sorted()) }.sortedBy { it.toString() }

private fun InfotrygdSakResponse.trim(): InfotrygdSakResponse = copy(saker = saker.map { it.trim() }.sortedBy { it.toString() })

/**
 * Enkelte String-felter fra Infotrygd er fastbredde CHAR-kolonner i DB2. On-prem og GCP-replikaen kan derfor
 * returnere disse med ulik whitespace-padding selv om verdien reelt sett er lik (eller tom). Trimmes bort her
 * for at slike rene formateringsforskjeller ikke skal trigge falske avvik i skyggekjøringen.
 */
private fun InfotrygdSak.trim(): InfotrygdSak =
    copy(
        saksnr = saksnr?.trim(),
        saksblokk = saksblokk?.trim(),
        kapittelnr = kapittelnr?.trim(),
        årsakskode = årsakskode?.trim(),
        behandlendeEnhet = behandlendeEnhet?.trim(),
        registrertAvEnhet = registrertAvEnhet?.trim(),
        tkNr = tkNr?.trim(),
        region = region?.trim(),
    )

private fun InfotrygdFinnesResponse.trim(): InfotrygdFinnesResponse =
    copy(
        vedtak = vedtak.sortedBy { it.toString() },
        saker = saker.sortedBy { it.toString() },
    )
