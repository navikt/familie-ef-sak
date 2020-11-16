package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

internal class PersonopplysningerControllerTest : OppslagSpringRunnerTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val respons: ResponseEntity<Ressurs<PersonopplysningerDto>> = hentPersonopplysninger()

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        Assertions.assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        Assertions.assertThat(respons.body?.data).isNull()
    }

    private fun hentPersonopplysninger(): ResponseEntity<Ressurs<PersonopplysningerDto>> {
        val personopplysningerRequest = PersonIdentDto("ikkeTilgang")

        return restTemplate.exchange(localhost("/api/personopplysninger"),
                                     HttpMethod.POST,
                                     HttpEntity(personopplysningerRequest, headers))
    }

}