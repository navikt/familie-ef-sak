package no.nav.familie.ef.sak.api.gui

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.integration.FamilieIntegrasjonerClient
import no.nav.familie.ef.sak.integration.dto.familie.Tilgang
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.Testsøknad.søknad
import no.nav.familie.ef.sak.repository.SøknadRepository
import no.nav.familie.ef.sak.repository.VedleggRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.VedleggService
import no.nav.familie.ef.sak.validering.Sakstilgang
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.*

internal class VedleggControllerTest {

    private val vedleggId = UUID.fromString("6005812f-0713-4cf2-a223-e9dd0c83e9ed")

    private lateinit var vedleggRepository: VedleggRepository
    private lateinit var søknadRepository: SøknadRepository
    private lateinit var integrasjonerClient: FamilieIntegrasjonerClient
    private lateinit var vedleggController: VedleggController

    @BeforeEach
    internal fun setUp() {
        vedleggRepository = mockk()
        søknadRepository = mockk()
        integrasjonerClient = mockk()
        vedleggController = VedleggController(VedleggService(vedleggRepository, Sakstilgang(søknadRepository, integrasjonerClient)))
    }

    @Test
    internal fun `kaster feil når man ikke finner vedlegget`() {
        every { vedleggRepository.findByIdOrNull(any()) } returns null

        assertThat(Assertions.catchThrowable { vedleggController.hentVedlegg(vedleggId) })
                .isInstanceOf(Feil::class.java)
                .extracting { (it as Feil).message }
                .isEqualTo("Ugyldig Primærnøkkel: 6005812f-0713-4cf2-a223-e9dd0c83e9ed")
    }

    @Test
    internal fun `finner vedlegg på id`() {
        every { vedleggRepository.findByIdOrNull(any()) } returns vedlegg()
        every { søknadRepository.findByIdOrNull(any()) } returns sak()
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(true))

        val vedleggResponse = vedleggController.hentVedlegg(UUID.randomUUID())
        assertThat(vedleggResponse.statusCodeValue).isEqualTo(200)
    }

    @Test
    internal fun `kaster feil når man ikke har tilgang på vedlegget`() {
        every { vedleggRepository.findByIdOrNull(any()) } returns vedlegg()
        every { søknadRepository.findByIdOrNull(any()) } returns sak()
        every { integrasjonerClient.sjekkTilgangTilPersoner(any()) } returns listOf(Tilgang(false))

        assertThat(Assertions.catchThrowable { vedleggController.hentVedlegg(UUID.randomUUID()) })
                .isInstanceOf(Feil::class.java)
                .matches { (it as Feil).frontendFeilmelding == "Har ikke tilgang til saken" }
    }

    private fun sak() = Søknad(søknad = objectMapper.writeValueAsBytes(søknad),
                               type = SøknadType.OVERGANGSSTØNAD,
                               saksnummerInfotrygd = "saksnummer",
                               søker = Søker("12345612345", "Navn"),
                               barn = setOf(Barn(fødselsdato = LocalDate.now(),
                                              harSammeAdresse = true,
                                              fødselsnummer = null,
                                              navn = "Navn")),
                               journalpostId = "journalId")

    private fun vedlegg(): Vedlegg {
        return Vedlegg(vedleggId, UUID.randomUUID(), Sporbar(), byteArrayOf(12), "navn")
    }

}