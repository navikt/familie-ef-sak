package no.nav.familie.ef.sak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.repository.SøknadRepository
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Søker
import no.nav.familie.ef.sak.repository.domain.Søknad
import no.nav.familie.ef.sak.repository.domain.SøknadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*

internal class BehandlingstilgangTest {

    private val integrasjonerClient = mockk<FamilieIntegrasjonerClient>()

    private val søknadRepository = mockk<SøknadRepository>()

    private val behandlingstilgang = Behandlingstilgang(søknadRepository, integrasjonerClient)

    @BeforeEach
    fun setUp() {
        every { søknadRepository.findByBehandlingId(any()) }
                .returns(Søknad(UUID.randomUUID(),
                                UUID.randomUUID(),
                                SøknadType.OVERGANGSSTØNAD,
                                UUID.randomUUID(),
                                "1",
                                "1",
                                Sporbar(),
                                Søker("654654654", "Bob"),
                                setOf("321321321")))
    }

    @Test
    fun `isValid returnerer true hvis integrasjonerClient sjekkTilgangTilPersoner returnerer liste med bare true`() {
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) }
                .returns(listOf(Tilgang(true, null),
                                Tilgang(true, null),
                                Tilgang(true, null)))

        val valid = behandlingstilgang.isValid(UUID.randomUUID(), mockk())

        Assertions.assertThat(valid).isTrue()
    }

    @Test
    fun `isValid returnerer false hvis integrasjonerClient sjekkTilgangTilPersoner returnerer liste som inneholder false`() {
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) }
                .returns(listOf(Tilgang(true, null),
                                Tilgang(false, null),
                                Tilgang(true, null)))

        val valid = behandlingstilgang.isValid(UUID.randomUUID(), mockk())

        Assertions.assertThat(valid).isFalse()
    }
}