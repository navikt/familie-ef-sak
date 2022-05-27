package no.nav.familie.ef.sak.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
    val år = nyesteGrunnbeløpGyldigFraOgMed.year

    @BeforeEach
    fun setup() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent("321", false)))
        clearMocks(iverksettClient, answers = false)
    }

    /**
     * Denne brekker hver gang det kommer nytt G-beløp.
     * Beløp må oppdateres i omregnes i expectedIverksettDto.json.
     */
    @Test
    fun `utførGOmregning kaller iverksettUtenBrev med korrekt iverksettDto `() {
        val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
        val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = fagsakId, identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                id = behandlingId,
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år))
        vedtakRepository.insert(vedtak(behandling.id, år = år))

        omregningService.utførGOmregning(fagsakId, true)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNotNull
        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }
        val expectedIverksettDto = iverksettMedOppdaterteIder(fagsak, behandling)
        assertThat(iverksettDtoSlot.captured).usingRecursiveComparison()
            .ignoringFields("vedtak.vedtakstidspunkt")
            .isEqualTo(expectedIverksettDto)
    }

    @Test
    fun `utførGOmregning kjørt med liveRun=false kaster exception og etterlater seg ingen spor i databasen`() {

        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år))
        vedtakRepository.insert(vedtak(behandling.id, år = år))

        assertThrows<DryRunException> { omregningService.utførGOmregning(fagsak.id, false) }

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med samordningsfradrag returner og etterlater seg ingen spor i databasen i dry run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        vedtakRepository.insert(vedtak(behandling.id, år = år, inntekter = InntektWrapper(listOf(inntektsperiode))))

        omregningService.utførGOmregning(fagsak.id, false)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med samordningsfradrag returner og etterlater seg ingen spor i databasen i live run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        vedtakRepository.insert(vedtak(behandling.id, år = år, inntekter = InntektWrapper(listOf(inntektsperiode))))

        omregningService.utførGOmregning(fagsak.id, true)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med sanksjon returner og etterlater seg ingen spor i databasen i dry run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        val vedtaksperiode = vedtaksperiode(år = år, vedtaksperiodeType = VedtaksperiodeType.SANKSJON)
        vedtakRepository.insert(
            vedtak(
                behandlingId = behandling.id,
                år = år,
                inntekter = InntektWrapper(listOf(inntektsperiode)),
                perioder = PeriodeWrapper(listOf(vedtaksperiode))
            )
        )

        omregningService.utførGOmregning(fagsak.id, false)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med sanksjon returner og etterlater seg ingen spor i databasen i live run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        val vedtaksperiode = vedtaksperiode(år = år, vedtaksperiodeType = VedtaksperiodeType.SANKSJON)
        vedtakRepository.insert(
            vedtak(
                behandlingId = behandling.id,
                år = år,
                inntekter = InntektWrapper(listOf(inntektsperiode)),
                perioder = PeriodeWrapper(listOf(vedtaksperiode))
            )
        )

        omregningService.utførGOmregning(fagsak.id, true)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    fun iverksettMedOppdaterteIder(fagsak: Fagsak, behandling: Behandling): IverksettOvergangsstønadDto {

        val nyBehandling =
            behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(
                StønadType.OVERGANGSSTØNAD,
                fagsak.personIdenter.map {
                    it.ident
                }.toSet()
            ) ?: error("Impossibru! :p")

        val expectedIverksettDto: IverksettOvergangsstønadDto =
            ObjectMapperProvider.objectMapper.readValue(readFile("expectedIverksettDto.json"))

        val andelerTilkjentYtelse = expectedIverksettDto.vedtak.tilkjentYtelse?.andelerTilkjentYtelse?.map {
            if (it.fraOgMed >= nyesteGrunnbeløpGyldigFraOgMed) {
                it.copy(kildeBehandlingId = nyBehandling.id)
            } else {
                it.copy(kildeBehandlingId = behandling.id)
            }
        } ?: emptyList()
        val tilkjentYtelseDto =
            expectedIverksettDto.vedtak.tilkjentYtelse?.copy(andelerTilkjentYtelse = andelerTilkjentYtelse)
        val vedtak = expectedIverksettDto.vedtak.copy(tilkjentYtelse = tilkjentYtelseDto)
        val behandlingsdetaljerDto = expectedIverksettDto.behandling.copy(
            behandlingId = nyBehandling.id,
            eksternId = nyBehandling.eksternId.id
        )
        return expectedIverksettDto.copy(
            vedtak = vedtak,
            behandling = behandlingsdetaljerDto,
            fagsak = expectedIverksettDto.fagsak.copy(eksternId = fagsak.eksternId.id)
        )
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/omregning/$filnavn")!!.readText()
    }
}
