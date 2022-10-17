package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevAvsnitt
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.UUID

internal class BrevMellomlagerControllerTest : OppslagSpringRunnerTest() {
    val fagsak = fagsak(identer = setOf(PersonIdent("12345678901")))
    val frittståendeBrev = frittståendeBrevDto(fagsak)

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `Skal mellomlagre og hente ut frittstående brev`() {
        val responsFørLagring = hentMellomlagretBrev(fagsak.id)
        assertThat(responsFørLagring.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(responsFørLagring.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(responsFørLagring.body?.data).isNull()
        mellomlagre(frittståendeBrev)
        val respons = hentMellomlagretBrev(fagsak.id)

        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body?.data).isEqualTo(frittståendeBrev)
    }

    @Test
    internal fun `Skal mellomlagre og slette frittstående brev`() {
        mellomlagre(frittståendeBrev)
        val respons = hentMellomlagretBrev(fagsak.id)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body?.data).isEqualTo(frittståendeBrev)
        slettMellomlagretBrev(fagsak.id)
        val responsEtterSletting = hentMellomlagretBrev(fagsak.id)
        assertThat(responsEtterSletting.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(responsEtterSletting.body?.data).isNull()
    }

    private fun frittståendeBrevDto(fagsak: Fagsak) = FrittståendeBrevDto(
        overskrift = "Tralalala",
        avsnitt = listOf(
            FrittståendeBrevAvsnitt(
                deloverskrift = "Hei",
                innhold = "Hopp",
                skalSkjulesIBrevbygger = false
            )
        ),
        fagsakId = fagsak.id,
        brevType = FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT,
        mottakere = BrevmottakereDto(
            personer = listOf(BrevmottakerPerson("12345678901", "Hei", MottakerRolle.VERGE)),
            organisasjoner = emptyList()
        )
    )

    private fun mellomlagre(frittståendeBrev: FrittståendeBrevDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(
            localhost("/api/brev/mellomlager/frittstaende"),
            HttpMethod.POST,
            HttpEntity(frittståendeBrev, headers)
        )
    }

    private fun hentMellomlagretBrev(id: UUID): ResponseEntity<Ressurs<FrittståendeBrevDto?>> {
        return restTemplate.exchange(
            localhost("/api/brev/mellomlager/frittstaende/$id"),
            HttpMethod.GET,
            HttpEntity<Ressurs<FrittståendeBrevDto?>>(headers)
        )
    }

    private fun slettMellomlagretBrev(id: UUID): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(
            localhost("/api/brev/mellomlager/frittstaende/$id"),
            HttpMethod.DELETE,
            HttpEntity<Ressurs<UUID>>(headers)
        )
    }
}
