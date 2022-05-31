package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForBarn
import no.nav.familie.kontrakter.ef.iverksett.OppgaverForBarnDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.security.token.support.core.api.Unprotected
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/barn-opprett-oppgaver")
@Unprotected
@Validated
class OpprettOppgaverForBarnSomIkkeBlittOpprettetTidligereController(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val grunnlagsdataService: GrunnlagsdataService,
    private val iverksettClient: IverksettClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val barn1År = setOf(
        "02f13409-e531-4a51-8e0a-09e8a1348585" to "2021-11-01",
        "06139f78-d3f7-45b7-bf80-128c038fd114" to "2021-10-29",
        "19950024-23f2-4bae-aa11-75b9114e68a8" to "2021-10-26",
        "1abbd7a1-cd31-4135-87f5-95bbece3c9b5" to "2021-10-24",
        "1cb57e75-cdde-42a5-9eac-235594a6bfbc" to "2021-10-15",
        "22593f04-03e4-4bcc-ac25-1e4b856c6259" to "2021-11-10",
        "24966c4e-0f87-4fa5-9aa5-a0d5eb961549" to "2021-09-30",
        "26945a6a-61f6-4647-b4d6-1036da0ebc1e" to "2021-11-18",
        "2765c192-7c03-43c8-8d5d-4be9fb23ffb6" to "2021-10-11",
        "2a246efb-4002-47ba-b886-b0b11e63e93b" to "2021-11-07",
        "30e619ee-cd9d-4f29-92eb-5fb136fe172b" to "2021-10-27",
        "3165c025-a5d6-4e40-b3fa-f4c3c014ccfc" to "2021-11-18",
        "3165c025-a5d6-4e40-b3fa-f4c3c014ccfc" to "2021-11-18",
        "3322fae4-8726-488a-b680-99ee38e0fe23" to "2021-11-03",
        "360cd483-a709-4d4e-87a4-1c606081e795" to "2021-11-07",
        "3b13d86e-e76a-477d-b3c5-abddb178b91f" to "2021-10-06",
        "3e2c1abd-cbad-4154-8cd4-461da965cbed" to "2021-10-26",
        "3e47d666-d28b-404d-b1e5-53370e613956" to "2021-10-09",
        "3f3959d9-be2e-484f-a6a3-ccdee344ce79" to "2021-11-18",
        "55973308-1262-4140-ac00-50edee46222d" to "2021-11-11",
        "609806a6-ce07-408e-8801-0aba1e3a79a5" to "2021-11-05",
        "6550b5eb-ed7c-430f-8e10-95d6f20e72de" to "2021-10-31",
        "67f7003c-8efb-47c5-aaba-c5caa298fa86" to "2021-11-18",
        "69848559-29d1-4967-9e71-a4f818758db6" to "2021-10-01",
        "821269c1-437e-4ade-9943-109529a43030" to "2021-10-23",
        "859e2212-4fff-4775-ba37-a84cc3f41d02" to "2021-11-10",
        "86204b7a-fac1-43c0-96b8-cfe10c896a8d" to "2021-10-02",
        "8cd9e1f1-744c-434a-9078-858613b31b48" to "2021-11-10",
        "8ea5ec13-2552-4348-aded-1346c90c8080" to "2021-11-08",
        "954b946a-3eb5-4dd6-9c0f-aa959f25429a" to "2021-11-04",
        "98953e88-914e-4cf5-a856-27e0eaea3e61" to "2021-10-07",
        "a07ff1b0-7ebe-4720-b7d5-61a588e8d765" to "2021-10-02",
        "a6e021e2-ce78-4cfc-bd8e-ebb162282f67" to "2021-11-18",
        "a6e8489b-1ebe-45cf-b148-dcde37fcbdfe" to "2021-11-09",
        "b341fe4a-8db8-482a-a875-56dcc4f047c0" to "2021-10-02",
        "b741b9a1-9094-469c-a59e-60362e808529" to "2021-10-12",
        "bc617aba-8f2c-4475-bd8a-c9a567e6ba75" to "2021-11-15",
        "bd4f01d0-f9df-4788-9266-52bdd69ca07f" to "2021-11-13",
        "c0368c28-4507-462d-94e8-acc6b5ed10ff" to "2021-10-24",
        "c56d3636-b6db-4f0b-99a7-1bca5d42af20" to "2021-11-15",
        "d2814c84-83ff-432a-af35-e908969bb7c8" to "2021-11-04",
        "d34c71c7-3de1-4a29-87c9-314b06c9c8fa" to "2021-10-18",
        "dff6099c-030e-4f00-8784-850e7f1cfc81" to "2021-11-08",
        "e19e19b3-cf32-4435-81a7-632513a80ff4" to "2021-10-14",
        "e317e7b3-2db6-42c9-b399-f1a1a31e7f59" to "2021-10-22",
        "e36ef362-67af-488d-92c7-39c47cc9d56e" to "2021-10-23",
    ).map { UUID.fromString(it.first) to LocalDate.parse(it.second) }

    @PostMapping("1ar")
    fun opprettOppgaver(@RequestParam dryRun: Boolean) {
        val beskrivelse = OppgaveBeskrivelse.beskrivelseBarnBlirSeksMnd()
        val oppgaver = barn1År.mapNotNull { (behandlingId, fødselsdato) ->
            if (behandlingRepository.findByIdOrNull(behandlingId) == null) {
                logger.info("Finner ikke behandling for $behandlingId")
                return@mapNotNull null
            }

            val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
            val barnFraGrunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId).grunnlagsdata.barn
            val barn = barnFraGrunnlagsdata.find { it.fødsel.gjeldende().fødselsdato == fødselsdato }
            if (barn == null) {
                logger.info("Finner ikke barn med fødselsdato $fødselsdato for behandling=$behandlingId")
                return@mapNotNull null
            }
            OppgaveForBarn(
                behandlingId,
                saksbehandling.eksternFagsakId,
                saksbehandling.ident,
                StønadType.OVERGANGSSTØNAD,
                beskrivelse
            )
        }
        if (dryRun) {
            logger.info("Oppretter ikke oppgaver")
        } else {
            iverksettClient.sendOppgaverForBarn(OppgaverForBarnDto(oppgaver))
        }
    }
}
