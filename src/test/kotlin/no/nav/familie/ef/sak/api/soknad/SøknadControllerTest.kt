package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api.soknad

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.api.dto.SøknadDatoerDto
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
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
import java.util.*

internal class SøknadControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakRepository: FagsakRepository
    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired lateinit var behandlingService: BehandlingService
    @Autowired lateinit var fagsakService: FagsakService

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal returnere 200 OK med status IKKE_TILGANG dersom man ikke har tilgang til brukeren`() {
        val fagsak = fagsakRepository.insert(fagsak(identer = setOf(FagsakPerson("ikkeTilgang"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, aktiv = false))
        val respons = hentSøknadData(behandling.id)

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        Assertions.assertThat(respons.body?.status).isEqualTo(Ressurs.Status.IKKE_TILGANG)
        Assertions.assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `Skal hente søknadsinformasjon gitt behandlingId`(){
        val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())
        val fagsak = fagsakService.hentEllerOpprettFagsakMedBehandlinger(søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi,
                                                                         Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.FØRSTEGANGSBEHANDLING, fagsak.id)
        behandlingService.lagreSøknadForOvergangsstønad(søknad.søknad, behandling.id, fagsak.id, "1234")
        val søknadSkjema = behandlingService.hentOvergangsstønad(behandling.id)
        val respons: ResponseEntity<Ressurs<SøknadDatoerDto>> = hentSøknadData(behandling.id)

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(respons.body?.status).isEqualTo(Ressurs.Status.SUKSESS)
        Assertions.assertThat(respons.body?.data?.søkerStønadFra).isEqualTo(søknadSkjema.søkerFra)
        Assertions.assertThat(respons.body?.data?.søknadsdato).isEqualTo(søknadSkjema.datoMottatt)

    }

    private fun hentSøknadData(behandlingId: UUID): ResponseEntity<Ressurs<SøknadDatoerDto>> {

        return restTemplate.exchange(localhost("/api/soknad/$behandlingId/datoer"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<SøknadDatoerDto>>(headers))
    }

}