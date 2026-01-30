package no.nav.familie.ef.sak.api.soknad

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.Ressurs.Status
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.exchange
import java.util.UUID

internal class SøknadControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandlingService: BehandlingService

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var søknadService: SøknadService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val respons = hentSøknadData(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `Skal hente søknadsinformasjon gitt behandlingId`() {
        val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())
        val fagsak =
            fagsakService.hentEllerOpprettFagsakMedBehandlinger(
                søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi,
                StønadType.OVERGANGSSTØNAD,
            )
        val behandlingÅrsak = BehandlingÅrsak.SØKNAD
        val behandling =
            behandlingService.opprettBehandling(
                BehandlingType.FØRSTEGANGSBEHANDLING,
                fagsak.id,
                behandlingsårsak = behandlingÅrsak,
            )
        søknadService.lagreSøknadForOvergangsstønad(søknad.søknad, behandling.id, fagsak.id, "1234")
        val søknadSkjema = søknadService.hentOvergangsstønad(behandling.id)!!
        val respons: ResponseEntity<Ressurs<SøknadDatoerDto>> = hentSøknadData(behandling.id)

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        assertThat(respons.body?.data?.søkerStønadFra).isEqualTo(søknadSkjema.søkerFra)
        assertThat(respons.body?.data?.søknadsdato).isEqualTo(søknadSkjema.datoMottatt)
    }

    private fun hentSøknadData(behandlingId: UUID): ResponseEntity<Ressurs<SøknadDatoerDto>> =
        testRestTemplate.exchange(
            localhost("/api/soknad/$behandlingId/datoer"),
            HttpMethod.GET,
            HttpEntity<Ressurs<SøknadDatoerDto>>(headers),
        )
}
