package no.nav.familie.ef.sak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.dto.PersonIdentDto
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.Tilgang
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PersontilgangTest {

    private val integrasjonerClient = mockk<FamilieIntegrasjonerClient>()

    private val persontilgang  = Persontilgang(integrasjonerClient)

    @Test
    fun `isValid returnerer true hvis integrasjonerClient sjekkTilgangTilPersoner returnerer true`() {
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) }
                .returns(listOf(Tilgang (true, null)))

        val valid = persontilgang.isValid(PersonIdentDto("654654654"), mockk())

        assertThat(valid).isTrue()
    }

    @Test
    fun `isValid returnerer false hvis integrasjonerClient sjekkTilgangTilPersoner returnerer false`() {
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) }
                .returns(listOf(Tilgang (false, null)))

        val valid = persontilgang.isValid(PersonIdentDto("654654654"), mockk())

        assertThat(valid).isFalse()
    }

}