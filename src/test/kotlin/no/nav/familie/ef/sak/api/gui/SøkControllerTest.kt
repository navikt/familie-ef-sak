package no.nav.familie.ef.sak.api.gui

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.dto.Søkeresultat
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdFinnesResponse
import no.nav.familie.kontrakter.ef.infotrygd.Saktreff
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class SøkControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var fagsakPersonRepository: FagsakPersonRepository
    @Autowired private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @AfterEach
    internal fun tearDown() {
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
    }

    @Test
    internal fun `Gitt person med fagsak når søk på personensident kallas skal det returneres 200 OK med Søkeresultat`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("01010199999"))))

        val response = søkPerson("01010199999")
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.data?.fagsakPersonId).isEqualTo(fagsak.fagsakPersonId)
        assertThat(response.body?.data?.personIdent).isEqualTo("01010199999")
        assertThat(response.body?.data?.fagsaker?.first()?.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
    }

    @Test
    internal fun `Gitt person uten fagsak når søk på personensident kallas skal det returneres RessursFeilet`() {
        val response = søkPerson("01010166666")
        assertThat(response.body.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        assertThat(response.body.frontendFeilmelding).isEqualTo("Finner ikke fagsak for søkte personen")
    }

    @Test
    internal fun `Skal opprette fagsakPerson når personen finne i infotrygd`() {
        val personIdent = "01010199999"
        every { infotrygdReplikaClient.hentInslagHosInfotrygd(any()) } returns
                InfotrygdFinnesResponse(emptyList(), listOf(Saktreff(personIdent, StønadType.OVERGANGSSTØNAD)))
        val response = søkPerson(personIdent)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        val data = response.body!!.data!!
        assertThat(data.personIdent).isEqualTo(personIdent)
        assertThat(data.fagsaker).hasSize(0)
        assertThat(fagsakRepository.findBySøkerIdent(setOf(personIdent))).hasSize(0)
        assertThat(fagsakPersonRepository.findByIdent(setOf(personIdent))).isNotNull
    }

    @Test
    internal fun `Skal feile hvis personIdenten har feil lengde`() {
        val response = søkPerson("010101999990")
        assertThat(response.body.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        assertThat(response.body.frontendFeilmelding).isEqualTo("Ugyldig personident. Det må være 11 sifre")
    }

    @Test
    internal fun `Skal feile hvis personIdenten inneholder noe annet enn tall`() {
        val response = søkPerson("010et1ord02")
        assertThat(response.body.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        assertThat(response.body.frontendFeilmelding).isEqualTo("Ugyldig personident. Det kan kun inneholde tall")
    }

    private fun søkPerson(personIdent: String): ResponseEntity<Ressurs<Søkeresultat>> {
        return restTemplate.exchange(localhost("/api/sok/"),
                                     HttpMethod.POST,
                                     HttpEntity(PersonIdentDto(personIdent = personIdent), headers))
    }
}