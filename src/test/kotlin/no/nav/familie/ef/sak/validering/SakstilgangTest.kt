package no.nav.familie.ef.sak.validering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.Tilgang
import no.nav.familie.ef.sak.repository.SakRepository
import no.nav.familie.ef.sak.repository.domain.Sak
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.Søker
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.util.*

internal class SakstilgangTest {

    private val integrasjonerClient = mockk<FamilieIntegrasjonerClient>()

    private val sakRepository = mockk<SakRepository>()

    private val sakstilgang = Sakstilgang(sakRepository, integrasjonerClient)

    @BeforeEach
    fun setUp() {
        every { sakRepository.findByIdOrNull(any()) }
                .returns(Sak(UUID.randomUUID(),
                             byteArrayOf(0),
                             "1",
                             "1",
                             Sporbar(),
                             Søker("654654654", "Bob"),
                             emptySet()))
    }

    @Test
    fun `isValid returnerer true hvis integrasjonerClient sjekkTilgangTilPersoner returnerer liste med bare true`() {
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) }
                .returns(listOf(Tilgang(true, null),
                                Tilgang(true, null),
                                Tilgang(true, null)))

        val valid = sakstilgang.isValid(UUID.randomUUID(), mockk())

        Assertions.assertThat(valid).isTrue()
    }

    @Test
    fun `isValid returnerer false hvis integrasjonerClient sjekkTilgangTilPersoner returnerer liste som inneholder false`() {
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) }
                .returns(listOf(Tilgang(true, null),
                                Tilgang(false, null),
                                Tilgang(true, null)))

        val valid = sakstilgang.isValid(UUID.randomUUID(), mockk())

        Assertions.assertThat(valid).isFalse()
    }
}