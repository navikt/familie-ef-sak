package no.nav.familie.ef.sak.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.equality.shouldBeEqualToComparingFieldsExcept
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
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.kontrakter.ef.iverksett.AktivitetType
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.Periodetype
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
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

    @Test
    fun `utførGOmregning kaller iverksettUtenBrev med korrekt iverksettDto `() {
        val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
        val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = fagsakId, identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(behandling(id = behandlingId,
                                                                fagsak = fagsak,
                                                                resultat = BehandlingResultat.INNVILGET,
                                                                status = BehandlingStatus.FERDIGSTILT))
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år))
        vedtakRepository.insert(vedtak(behandling.id, år = år))

        omregningService.utførGOmregning(fagsakId, null, true)

        val nyBehandling =
                behandlingRepository.finnSisteBehandlingSomIkkeErBlankett(StønadType.OVERGANGSSTØNAD,
                                                                          fagsak.personIdenter.map { it.ident }.toSet())
        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNotNull
        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }
        val expectedIverksettDto = iverksett(fagsak, behandling, nyBehandling!!)
        iverksettDtoSlot.captured.shouldBeEqualToComparingFieldsExcept(expectedIverksettDto,
                                                                       BehandlingsdetaljerDto::behandlingId,
                                                                       BehandlingsdetaljerDto::eksternId,
                                                                       AndelTilkjentYtelseDto::kildeBehandlingId,
                                                                       VedtaksdetaljerOvergangsstønadDto::vedtakstidspunkt)
    }

    @Test
    fun `utførGOmregning kjørt med liveRun=false kaster exception og etterlater seg ingen spor i databasen`() {

        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(behandling(fagsak = fagsak,
                                                                resultat = BehandlingResultat.INNVILGET,
                                                                status = BehandlingStatus.FERDIGSTILT))
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år))
        vedtakRepository.insert(vedtak(behandling.id, år = år))

        assertThrows<DryRunException> { omregningService.utførGOmregning(fagsak.id, null, false) }

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }

    }

    fun iverksett(fagsak: Fagsak, behandling: Behandling, nyBehandling: Behandling): IverksettOvergangsstønadDto {
        val expectedIverksettDto =
                ObjectMapperProvider.objectMapper.readValue<IverksettOvergangsstønadDto>(readFile("expectedIverksettDto.json"))

        //TODO denne brekker ved neste G-beløp (2023-05-01). Beløp må omregnes i expected.
        val andel1 = AndelTilkjentYtelseDto(beløp = 9500,
                                            periodetype = Periodetype.MÅNED,
                                            inntekt = 0,
                                            inntektsreduksjon = 0,
                                            samordningsfradrag = 0,
                                            fraOgMed = LocalDate.of(år, 1, 1),
                                            tilOgMed = LocalDate.of(år, 4, 30),
                                            kildeBehandlingId = behandling.id)
        val andel2 = AndelTilkjentYtelseDto(beløp = 20952,
                                            periodetype = Periodetype.MÅNED,
                                            inntekt = 0,
                                            inntektsreduksjon = 0,
                                            samordningsfradrag = 0,
                                            fraOgMed = LocalDate.of(år, 5, 1),
                                            tilOgMed = LocalDate.of(år, 12, 31),
                                            kildeBehandlingId = nyBehandling.id)
        val tilkjentYtelseDto = expectedIverksettDto.vedtak.tilkjentYtelse?.copy(andelerTilkjentYtelse = listOf(andel1, andel2),
                                                                                 startdato = LocalDate.of(år, 1, 1))
        val vedtaksperioder = listOf(VedtaksperiodeOvergangsstønadDto(fraOgMed = nyesteGrunnbeløpGyldigFraOgMed,
                                                                      tilOgMed = LocalDate.of(år, 12, 31),
                                                                      aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                                                      periodeType = VedtaksperiodeType.HOVEDPERIODE))
        val vedtak = expectedIverksettDto.vedtak.copy(tilkjentYtelse = tilkjentYtelseDto, vedtaksperioder = vedtaksperioder)

        return expectedIverksettDto.copy(vedtak = vedtak,
                                         fagsak = expectedIverksettDto.fagsak.copy(eksternId = fagsak.eksternId.id))

    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/omregning/$filnavn")!!.readText()
    }
}
