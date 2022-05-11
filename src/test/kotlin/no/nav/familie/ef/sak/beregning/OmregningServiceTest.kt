package no.nav.familie.ef.sak.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class OmregningServiceTest : OppslagSpringRunnerTest() {


    @Autowired lateinit var omregningService: OmregningService
    @Autowired lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var vedtakRepository: VedtakRepository
    @Autowired lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired lateinit var taskRepository: TaskRepository
    @Autowired lateinit var iverksettClient: IverksettClient

    val personService = mockk<PersonService>()


    @BeforeEach
    fun setup() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent("321", false)))
    }

    @Test
    fun `utførGOmregning kaller iverksettUtenBrev med korrekt iverksettDto `() {

        val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
        val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = fagsakId, identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(behandling(id = behandlingId,
                                                                fagsak = fagsak,
                                                                resultat = BehandlingResultat.INNVILGET,
                                                                status = BehandlingStatus.FERDIGSTILT))
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321"))
        vedtakRepository.insert(vedtak(behandling.id))

        omregningService.utførGOmregning(fagsakId)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNotNull
        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }
        val expectedIverksettDto =
                objectMapper.readValue<IverksettOvergangsstønadDto>(readFile("expectedIverksettDto.json"))
        // Ignorerer behandlingId siden denne endrer seg.
        assertThat(iverksettDtoSlot.captured)
                .usingRecursiveComparison()
                .ignoringFields("behandling.behandlingId",
                                "vedtak.tilkjentYtelse.andelerTilkjentYtelse.kildeBehandlingId",
                                "vedtak.vedtakstidspunkt")
                .isEqualTo(expectedIverksettDto)
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/omregning/$filnavn")!!.readText()
    }
}