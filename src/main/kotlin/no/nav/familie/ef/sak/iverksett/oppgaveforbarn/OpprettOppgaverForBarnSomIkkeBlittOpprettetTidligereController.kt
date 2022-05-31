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
        "06c1aa63-f47d-47fa-bed5-05b58839bbe7" to "2021-05-08",
        "0fc1d94f-e805-40ad-82c3-2ff01f478da3" to "2021-05-10",
        "13968d19-35c6-4385-b7ad-3fc6d9a9b490" to "2021-05-13",
        "20c3b195-40de-4ff4-b083-b4cb2e962c49" to "2021-05-11",
        "235fc192-b9dd-4d4e-9ad2-8eeb0d660099" to "2021-05-12",
        "27795013-a7ce-4cfc-b953-5ca4725bc1fa" to "2021-05-14",
        "288d7015-0918-42a3-9252-b045d5e0e194" to "2021-05-18",
        "2f5649c6-f03d-4035-9d00-c7c331d8a3bb" to "2021-05-05",
        "33e969a2-f732-4b39-80af-fc744f8a35db" to "2021-05-06",
        "4fafc3d1-452a-4a06-b26d-bf372a472499" to "2021-05-14",
        "5563df12-35e4-485d-8aba-3877ee9ed4b2" to "2021-04-19",
        "5acc3d3d-abe3-4a66-8495-0a4b83c6a5b1" to "2021-05-02",
        "6030743c-e368-4822-a727-b59e8f38a7b7" to "2021-05-15",
        "793e67eb-6054-4bdc-b939-ba10d40be4e4" to "2021-05-10",
        "8859f937-9b4d-407c-b33a-f9f0fa0fe403" to "2021-05-06",
        "88dff2d1-2df4-49d7-afbb-7846aeae0579" to "2021-04-05",
        "8b56ead6-fb2e-4312-8baf-55bff9ee685a" to "2021-05-10",
        "8df68811-e91a-4725-99e7-ad266c3254a5" to "2021-04-03",
        "a6c6739b-fbc9-438d-867c-4f1f001ed920" to "2021-04-09",
        "aab185fb-71b8-4a7b-b8e4-fc014b7d2784" to "2021-05-04",
        "ab522e54-bd07-46a6-987b-fe3697bb755c" to "2021-04-12",
        "b10ce65b-03e6-4d00-82aa-0b636e846593" to "2021-05-10",
        "b77f6fa0-6c12-40f2-9b3f-6eadf128b54f" to "2021-05-08",
        "be02bd1e-2441-495e-b40d-c2f0e6fa0e92" to "2021-04-09",
        "c3d02e86-85b5-4a0d-bdea-563361a2e9b4" to "2021-05-16",
        "c74b309b-54eb-492b-8298-ac8776c4df4c" to "2021-04-16",
        "cad2a19f-bc71-4dca-8b66-cf8a3beefc53" to "2021-05-11",
        "cd6d991e-10f9-4968-be4f-fcf047db706a" to "2021-04-19",
        "d0504c11-7c1f-44dd-86ff-488a7ab43b49" to "2021-05-07",
        "d45ea3fb-a6b1-4973-992f-aa9edeaea9b2" to "2021-04-26",
        "d503237a-5e1b-4431-8ed3-8b2546fdd9d5" to "2021-05-08",
        "d714b350-e24a-45a9-b01d-088f1b0bb7be" to "2021-05-07",
        "da1bcad1-4dec-49c3-9e40-82d89c92404e" to "2021-05-08",
        "e0662204-01bc-467b-9059-0f21e974606d" to "2021-05-17",
        "e26b1266-d16e-4d99-af2f-30f031734dcb" to "2021-05-17",
        "edee6060-5ca3-4c58-9296-f25ab3ad9e86" to "2021-05-05",
        "eebb695e-9f6e-4543-bc20-e18685b7e811" to "2021-05-16",

        "38a3b6fd-9504-4247-a754-d4bcaed61149" to "2021-05-09",
        "e8ba3326-8197-4ec6-bdb6-d79ac150ad44" to "2021-05-14",
    ).map { UUID.fromString(it.first) to LocalDate.parse(it.second) }

    @PostMapping("1ar")
    fun opprettOppgaver(@RequestParam dryRun: Boolean) {
        val beskrivelse = OppgaveBeskrivelse.beskrivelseBarnFyllerEttÅr()
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
