package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.domene.PersonIdentDto
import no.nav.familie.ef.sak.fagsak.dto.Søkeresultat
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions
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

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Gitt person med fagsak når søk på personensident kallas skal det returneres 200 OK med Søkeresultat`() {
         fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson("01010199999"))))

        val response = søkPerson("01010199999")
        Assertions.assertThat(response.statusCode).isEqualTo(
                HttpStatus.OK)
        Assertions.assertThat(response.body?.data?.personIdent).isEqualTo("01010199999")
        Assertions.assertThat(response.body?.data?.fagsaker?.first()?.stønadstype).isEqualTo(Stønadstype.OVERGANGSSTØNAD)
    }

    @Test
    internal fun `Gitt person uten fagsak når søk på personensident kallas skal det returneres RessursFeilet`() {
        val response = søkPerson("01010166666")
        Assertions.assertThat(response.body.status).isEqualTo(Ressurs.Status.FEILET)
        Assertions.assertThat(response.body.frontendFeilmelding).isEqualTo("Finner ikke fagsak for søkte personen")
    }

    private fun søkPerson(personIdent: String): ResponseEntity<Ressurs<Søkeresultat>> {
        return restTemplate.exchange(localhost("/api/sok/"),
                                     HttpMethod.POST,
                                     HttpEntity(PersonIdentDto(personIdent = personIdent), headers))
    }
}