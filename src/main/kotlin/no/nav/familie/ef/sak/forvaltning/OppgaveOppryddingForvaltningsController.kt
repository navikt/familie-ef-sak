package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype

@RestController
@RequestMapping("/api/oppgave/forvaltning/opprydding")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveOppryddingForvaltningsController(
    private val oppgaveService: OppgaveService,
    private val tilgangService: TilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    final val mappenavnProd = "41 Revurdering"
    final val mappenavnDev = "41 - Revurdering"
    val mappeNavn = listOf(mappenavnDev, mappenavnProd)

    data class RunType(val liveRun: Boolean)

    @PostMapping("start-opprydding")
    fun ryddOppgaver(
        @RequestBody runType: RunType,
    ) {
        logger.info("Starter opprydding av oppgaver")
        tilgangService.validerHarForvalterrolle()

        val mappeId = hentFremleggMappeId()
        logger.info("Funnet mappe: $mappeId")

        val oppgaveFrist = LocalDate.of(2024, 5, 18)
        val oppgaver =
            oppgaveService.hentOppgaver(
                FinnOppgaveRequest(
                    tema = Tema.ENF,
                    fristFomDato = oppgaveFrist,
                    fristTomDato = oppgaveFrist,
                    mappeId = mappeId.toLong(),
                    limit = 1000,
                ),
            )

        logger.info("Antall oppgaver funnet: ${oppgaver.antallTreffTotalt}. Antall oppgaver hentet ut: ${oppgaver.oppgaver.size}")

        if (runType.liveRun) {
            oppgaver.oppgaver.forEach { oppgave ->
                val oppgaveId = oppgave.id
                if (oppgaveId == null) {
                    logger.error("Kan ikke ferdigstille oppgave - mangler ID")
                    secureLogger.info("Kan ikke ferdigstille oppgave pga manglende ID: $oppgave")
                } else if (oppgave.oppgavetype != Oppgavetype.Fremlegg.name) {
                    logger.error("Kan ikke ferdigstille oppgave - feil type")
                    secureLogger.info("Kan ikke ferdigstille oppgave pga feil oppgavetype: $oppgave")
                } else {
                    secureLogger.info("Ferdigstiller oppgave $oppgaveId")
                    oppgaveService.ferdigstillOppgave(oppgaveId)
                }
            }
        }
    }

    private fun hentFremleggMappeId() = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { mappeNavn.contains(it.navn) }.id
}
