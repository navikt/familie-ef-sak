package no.nav.familie.ef.sak.behandling.henlegg

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.BehandlingDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.OppholdstillatelseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.SivilstandDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.VergemålDto
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.util.UUID

class HenleggBehandlingControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository
    val taskService = mockk<TaskService>(relaxed = true)
    val personopplysningerService = mockk<PersonopplysningerService>(relaxed = true)

    private val henleggBehandlingController: HenleggBehandlingController =
        HenleggBehandlingController(
            behandlingService = mockk(relaxed = true),
            fagsakService = mockk(relaxed = true),
            henleggService = mockk(relaxed = true),
            tilgangService = mockk(relaxed = true),
            featureToggleService = mockk(),
            taskService = taskService,
            personopplysningerService = personopplysningerService,
        )

    @BeforeEach fun setUp() {
        mockBrukerContext("", azp_name = "prod-gcp:teamfamilie:familie-ef-personhendelse")
        headers.setBearerAuth(lokalTestToken)
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test internal fun `Skal kaste feil hvis feilregistrert og send brev er true`() {
        val exception =
            assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.FEILREGISTRERT, true))
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis type er ulik trukket tilbake")
    }

    @Test internal fun `Skal lage send brev task hvis send brev er true og henlagårsak er trukket`() {
        henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
        verify(exactly = 1) { taskService.save(any()) }
    }

    @Test internal fun `Skal ikke lage send brev task hvis skalSendeHenleggelsesBrev er false`() {
        henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, false))
        verify(exactly = 0) { taskService.save(any()) }
    }

    @Test internal fun `Skal kaste feil hvis bruker har fullmakt`() {
        mocckHentPersonopplysningerMedFullmakt()
        val exception =
            assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
    }

    @Test internal fun `Skal ikke kaste feil hvis bruker har fullmakt som har utgått`() {
        mocckHentPersonopplysningerMedFullmaktEnDagSiden()
        henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
        verify(exactly = 1) { taskService.save(any()) }
    }

    @Test internal fun `Skal kaste feil hvis bruker har Verge`() {
        mockkHentPersonopplysningerMedVergemål()
        val exception =
            assertThrows<Feil> {
                henleggBehandlingController.henleggBehandling(UUID.randomUUID(), HenlagtDto(HenlagtÅrsak.TRUKKET_TILBAKE, true))
            }
        assertThat(exception.message).isEqualTo("Skal ikke sende brev hvis person er tilknyttet vergemål eller fullmakt")
    }

    @Test
    internal fun `Skal henlegge behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(respons.body?.data!!.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
    }

    @Test
    internal fun `Skal henlegge FØRSTEGANGSBEHANDLING`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678901"))))
        val behandling = behandlingRepository.insert(behandling(fagsak, type = BehandlingType.FØRSTEGANGSBEHANDLING))
        val respons = henlegg(behandling.id, HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT))

        assertThat(respons.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(respons.body?.data!!.resultat).isEqualTo(BehandlingResultat.HENLAGT)
        assertThat(respons.body?.data!!.henlagtÅrsak).isEqualTo(HenlagtÅrsak.FEILREGISTRERT)
    }

    private fun henleggBehandling(id: UUID): ResponseEntity<Ressurs<BehandlingDto>> =
        restTemplate.exchange(
            localhost("/api/behandling/$id/henlegg"),
            HttpMethod.POST,
            HttpEntity<Ressurs<BehandlingDto>>(headers),
        )

    private fun henlegg(
        id: UUID,
        henlagt: HenlagtDto,
    ): ResponseEntity<Ressurs<BehandlingDto>> =
        restTemplate.exchange<Ressurs<BehandlingDto>>(
            localhost("/api/behandling/$id/henlegg"),
            HttpMethod.POST,
            HttpEntity(henlagt, headers),
        )

    private fun mocckHentPersonopplysningerMedFullmaktEnDagSiden() {
        every { personopplysningerService.hentPersonopplysningerFraRegister(any()) } returns
            dto(
                fullmakt =
                    listOf(
                        FullmaktDto(
                            gyldigFraOgMed = LocalDate.now().minusDays(2),
                            gyldigTilOgMed = LocalDate.now().minusDays(1),
                            navn = "123",
                            motpartsPersonident = "123",
                            områder = emptyList(),
                        ),
                    ),
            )
    }

    private fun mocckHentPersonopplysningerMedFullmakt() {
        every { personopplysningerService.hentPersonopplysningerFraRegister(any()) } returns
            dto(
                fullmakt =
                    listOf(
                        FullmaktDto(
                            gyldigFraOgMed = LocalDate.now(),
                            gyldigTilOgMed = null,
                            navn = "123",
                            motpartsPersonident = "123",
                            områder = emptyList(),
                        ),
                    ),
            )
    }

    private fun mockkHentPersonopplysningerMedVergemål() {
        every { personopplysningerService.hentPersonopplysningerFraRegister(any()) } returns
            dto(
                vergemål =
                    listOf(
                        VergemålDto(
                            embete = null,
                            type = null,
                            motpartsPersonident = null,
                            navn = null,
                            omfang = null,
                        ),
                    ),
            )
    }

    private fun dto(
        status: Folkeregisterpersonstatus? = null,
        fødselsdato: LocalDate? = null,
        dødsdato: LocalDate? = null,
        statsborgerskap: List<StatsborgerskapDto> = emptyList(),
        sivilstand: List<SivilstandDto> = emptyList(),
        adresse: List<AdresseDto> = emptyList(),
        fullmakt: List<FullmaktDto> = emptyList(),
        barn: List<BarnDto> = emptyList(),
        innflyttingTilNorge: List<InnflyttingDto> = emptyList(),
        utflyttingFraNorge: List<UtflyttingDto> = emptyList(),
        oppholdstillatelse: List<OppholdstillatelseDto> = emptyList(),
        vergemål: List<VergemålDto> = emptyList(),
    ) = PersonopplysningerDto(
        personIdent = "",
        navn = NavnDto("", "", "", ""),
        kjønn = Kjønn.MANN,
        adressebeskyttelse = null,
        folkeregisterpersonstatus = status,
        fødselsdato = fødselsdato,
        dødsdato = dødsdato,
        statsborgerskap = statsborgerskap,
        sivilstand = sivilstand,
        adresse = adresse,
        fullmakt = fullmakt,
        egenAnsatt = false,
        barn = barn,
        innflyttingTilNorge = innflyttingTilNorge,
        utflyttingFraNorge = utflyttingFraNorge,
        oppholdstillatelse = oppholdstillatelse,
        vergemål = vergemål,
    )
}
