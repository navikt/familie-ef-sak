package no.nav.familie.ef.sak.no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.samværsavtale
import no.nav.familie.ef.sak.repository.samværsuke
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleRepository
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.exchange
import java.util.UUID

internal class SamværsavtaleControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var samværsavtaleRepository: SamværsavtaleRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal opprette, hente ut og slette samværsavtaler`() {
        val (behandling, samværsavtaler) = arrangerData("1")
        arrangerData("2")

        val hentSamværsavtalerRespons = hentSamværsavtaler(behandling.id)
        val data = hentSamværsavtalerRespons.body?.data

        assertThat(hentSamværsavtalerRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(data?.size).isEqualTo(3)
        assertThat(data?.map { it.behandlingId }?.toSet()).containsExactly(behandling.id)
        assertThat(data?.map { it.behandlingBarnId }).containsExactlyInAnyOrderElementsOf(samværsavtaler.map { it.behandlingBarnId })

        val slettSamværsavtale = samværsavtaler.first()
        val slettSamværsavtaleRespons = slettSamværsavtale(behandling.id, slettSamværsavtale.behandlingBarnId)
        val slettData = slettSamværsavtaleRespons.body?.data

        assertThat(slettSamværsavtaleRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(slettData?.size).isEqualTo(2)
        assertThat(slettData?.map { it.behandlingId }?.toSet()).containsExactly(behandling.id)
        assertThat(slettData?.map { it.behandlingBarnId }?.size).isEqualTo(2)
        assertThat(slettData?.map { it.behandlingBarnId }).doesNotContain(slettSamværsavtale.behandlingBarnId)

        val oppdaterSamværsavtaleRequest =
            slettData!!.first().copy(uker = listOf(samværsuke(listOf(Samværsandel.MORGEN, Samværsandel.KVELD_NATT))))
        val oppdaterSamværsavtaleRespons = oppdaterSamværsavtale(oppdaterSamværsavtaleRequest)
        val oppdaterData = oppdaterSamværsavtaleRespons.body?.data

        val rehentSamværsavtaleRespons = hentSamværsavtaler(behandling.id)
        val rehentData = rehentSamværsavtaleRespons.body?.data

        assertThat(oppdaterSamværsavtaleRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(rehentSamværsavtaleRespons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(oppdaterData?.size).isEqualTo(2)
        assertThat(oppdaterData?.size).isEqualTo(rehentData?.size)
        assertThat(oppdaterData?.map { it.behandlingId }).isEqualTo(rehentData?.map { it.behandlingId })
        assertThat(oppdaterData?.map { it.behandlingBarnId }).isEqualTo(rehentData?.map { it.behandlingBarnId })

        val samværsavtaleMedOppdaterteUker = oppdaterData?.find { it.behandlingBarnId == oppdaterSamværsavtaleRequest.behandlingBarnId }
        assertThat(samværsavtaleMedOppdaterteUker?.uker?.size).isEqualTo(1)
        assertThat(samværsavtaleMedOppdaterteUker?.summerTilSamværsandelerVerdiPerDag()?.sum()).isEqualTo(35)
    }

    private fun arrangerData(
        personIdent: String,
    ): Pair<Behandling, List<Samværsavtale>> {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(personIdent))))
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val barn = mutableListOf<BehandlingBarn>()
        val avtaler = mutableListOf<Samværsavtale>()

        (1..3).forEach {
            val behandlingBarn = behandlingBarn(behandlingId = behandling.id, personIdent = it.toString())
            val avtale = samværsavtale(behandlingId = behandling.id, behandlingBarnid = behandlingBarn.id)

            barnRepository.insert(behandlingBarn)
            samværsavtaleRepository.insert(avtale)

            barn.add(behandlingBarn)
            avtaler.add(avtale)
        }

        return Pair(behandling, avtaler.toList())
    }

    private fun hentSamværsavtaler(behandlingId: UUID): ResponseEntity<Ressurs<List<SamværsavtaleDto>>> =
        restTemplate.exchange(
            localhost("/api/samvaersavtale/$behandlingId"),
            HttpMethod.GET,
            HttpEntity<Ressurs<List<SamværsavtaleDto>>>(headers),
        )

    private fun oppdaterSamværsavtale(avtaleDto: SamværsavtaleDto): ResponseEntity<Ressurs<List<SamværsavtaleDto>>> =
        restTemplate.exchange(
            localhost("/api/samvaersavtale"),
            HttpMethod.POST,
            HttpEntity(avtaleDto, headers),
        )

    private fun slettSamværsavtale(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ): ResponseEntity<Ressurs<List<SamværsavtaleDto>>> =
        restTemplate.exchange(
            localhost("/api/samvaersavtale/$behandlingId/$behandlingBarnId"),
            HttpMethod.DELETE,
            HttpEntity<Ressurs<List<SamværsavtaleDto>>>(headers),
        )
}
