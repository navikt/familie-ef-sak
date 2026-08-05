package no.nav.familie.ef.sak.infrastruktur.prosessering

import efterlatte.prosessering.Status
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import efterlatte.prosessering.spring.TaskService as EfterlatteTaskService

@RestController
@RequestMapping(path = ["/api/efterlatte-prosessering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Profile("!prod")
class EfterlatteProsesseringController(
    private val efterlatteProsesseringAdminRepository: EfterlatteProsesseringAdminRepository,
    private val efterlatteProsesseringTaskService: EfterlatteTaskService,
    private val tilgangService: TilgangService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("task")
    fun hentTasks(
        @RequestParam(required = false) status: Status?,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false, defaultValue = "100") antall: Int,
    ): Ressurs<EfterlatteProsesseringOversiktDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvis(antall !in 1..MAKS_ANTALL) { "Antall må være mellom 1 og $MAKS_ANTALL" }

        val tasks = efterlatteProsesseringAdminRepository.hentTasks(status = status, type = type, antall = antall)

        return Ressurs.success(
            EfterlatteProsesseringOversiktDto(
                tasks = tasks.map { it.tilDto() },
                antallPerStatus = efterlatteProsesseringAdminRepository.hentAntallPerStatus(),
                typer = efterlatteProsesseringAdminRepository.hentTyper(),
            ),
        )
    }

    @GetMapping("task/{id}")
    fun hentTask(
        @PathVariable id: Long,
    ): Ressurs<EfterlatteProsesseringTaskDto> {
        tilgangService.validerHarSaksbehandlerrolle()

        val task = efterlatteProsesseringAdminRepository.hentTask(id)
        brukerfeilHvis(task == null) { "Finner ingen task med id $id" }

        return Ressurs.success(task.tilDto())
    }

    @PostMapping("task/{id}/kjor-paa-nytt")
    fun kjørPåNytt(
        @PathVariable id: Long,
    ): Ressurs<EfterlatteProsesseringTaskDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvisIkke(efterlatteProsesseringAdminRepository.rekjør(id)) {
            "Task $id kan ikke kjøres på nytt. Kun tasks med status STOPPET eller AVBRUTT kan rekjøres."
        }
        logger.info("${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} kjørte prosessering-task $id på nytt")

        return Ressurs.success(hentTaskEtterEndring(id))
    }

    @PostMapping("task/{id}/avbryt")
    fun avbryt(
        @PathVariable id: Long,
    ): Ressurs<EfterlatteProsesseringTaskDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvisIkke(efterlatteProsesseringAdminRepository.avbryt(id)) {
            "Task $id kan ikke avbrytes. Kun tasks med status KLAR eller STOPPET kan avbrytes."
        }
        logger.info("${SikkerhetContext.hentSaksbehandlerEllerSystembruker()} avbrøt prosessering-task $id")

        return Ressurs.success(hentTaskEtterEndring(id))
    }

    /**
     * Legger en [FeilbarDemoTask] i kø. Tasken feiler helt til det simulerte nedetidsvinduet har gått,
     * og fullfører når en operatør kjører den på nytt etterpå - se [FeilbarDemoTask] for hvorfor.
     */
    @PostMapping("demo/feilbar", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun opprettFeilbarDemoTask(
        @RequestBody request: OpprettFeilbarDemoTaskDto,
    ): Ressurs<FeilbarDemoTaskOpprettetDto> {
        tilgangService.validerHarSaksbehandlerrolle()
        brukerfeilHvis(request.vinduSekunder !in 1..MAKS_VINDU_SEKUNDER) {
            "Nedetidsvinduet må være mellom 1 og $MAKS_VINDU_SEKUNDER sekunder"
        }

        val payload = FeilbarDemoTask.opprettPayload(request.vinduSekunder)
        val taskId =
            efterlatteProsesseringTaskService.opprettIEgenTransaksjon(
                type = FeilbarDemoTask.TYPE,
                payload = payload,
            )
        logger.info("La feilbar demo-task ${taskId.verdi} i kø, simulert nede til ${payload.simulertOppeFra}")

        return Ressurs.success(
            FeilbarDemoTaskOpprettetDto(
                taskId = taskId.verdi,
                simulertOppeFra = payload.simulertOppeFra,
            ),
        )
    }

    private fun hentTaskEtterEndring(id: Long): EfterlatteProsesseringTaskDto {
        val task = efterlatteProsesseringAdminRepository.hentTask(id)
        brukerfeilHvis(task == null) { "Finner ingen task med id $id" }

        return task.tilDto()
    }

    companion object {
        private const val MAKS_ANTALL = 1000
        private const val MAKS_VINDU_SEKUNDER = 600L
    }
}
