package no.nav.familie.ef.sak.samværsavtale

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.hovedregelMetadata
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.samværsavtale
import no.nav.familie.ef.sak.repository.samværsuke
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.BARNEHAGE_SKOLE
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.ETTERMIDDAG
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.KVELD_NATT
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel.MORGEN
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.dto.JournalførBeregnetSamværRequest
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDto
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.kontrakter.felles.dokarkiv.ArkiverDokumentResponse
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpStatus
import java.util.UUID

internal class SamværsavtaleServiceTest {
    private val samværsavtaleRepository: SamværsavtaleRepository = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val fagsakService: FagsakService = mockk()
    private val tilordnetRessursService: TilordnetRessursService = mockk()
    private val barnService: BarnService = mockk()
    private val personopplysningerService: PersonopplysningerService = mockk()
    private val journalpostClient: JournalpostClient = mockk()
    private val brevClient: BrevClient = mockk()
    private val arbeidsfordelingService: ArbeidsfordelingService = mockk()
    private val brevsignaturService: BrevsignaturService = mockk()

    private val samværsavtaleService: SamværsavtaleService =
        SamværsavtaleService(
            samværsavtaleRepository = samværsavtaleRepository,
            behandlingService = behandlingService,
            fagsakService = fagsakService,
            tilordnetRessursService = tilordnetRessursService,
            barnService = barnService,
            personopplysningerService = personopplysningerService,
            journalpostClient = journalpostClient,
            brevClient = brevClient,
            arbeidsfordelingService = arbeidsfordelingService,
            brevsignaturService = brevsignaturService,
        )

    @Nested
    inner class OpprettEllerErstattSamværsavtale {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "FERDIGSTILT", "SATT_PÅ_VENT"],
            mode = EnumSource.Mode.INCLUDE,
        )
        internal fun `skal ikke kunne redigere samværsavtale dersom tilhørende behandling ikke er redigerbar`(behandlingStatus: BehandlingStatus) {
            val behandlingId = UUID.randomUUID()
            val samværsavtale = samværsavtale(behandlingId = behandlingId).tilDto()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = behandlingStatus)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(behandlingId = behandlingId))

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)
                }

            assertThat(feil.message).isEqualTo("Behandlingen er låst for videre redigering")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal ikke kunne redigere samværsavtale dersom behandlingen tilhører en annen saksbehandler`() {
            val behandlingId = UUID.randomUUID()
            val samværsavtale = samværsavtale(behandlingId = behandlingId).tilDto()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = UTREDES)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(behandlingId = behandlingId))
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns false

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)
                }

            assertThat(feil.message).isEqualTo("Behandlingen eies av en annen saksbehandler")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal ikke kunne opprette en samværsavtale uten noen uker`() {
            val behandlingId = UUID.randomUUID()
            val samværsavtale = samværsavtale(behandlingId = behandlingId).tilDto()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = UTREDES)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(behandlingId = behandlingId))
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns true

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)
                }

            assertThat(feil.message).isEqualTo("Kan ikke opprette en samværsavtale uten noen uker. BehandlingId=$behandlingId")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal ikke kunne opprette en samværsavtale med duplikate samværsandeler innenfor en og samme dag`() {
            val behandlingId = UUID.randomUUID()
            val samværsuke = samværsuke(andeler = listOf(MORGEN, MORGEN))
            val samværsavtale =
                samværsavtale(
                    behandlingId = behandlingId,
                    uker = listOf(samværsuke),
                ).tilDto()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = UTREDES)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(behandlingId = behandlingId))
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns true

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)
                }

            assertThat(feil.message).isEqualTo("Kan ikke ha duplikate samværsandeler innenfor en og samme dag. BehandlingId=$behandlingId")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal ikke kunne opprette en samværsavtale på et barn som ikke eksisterer på behandlingen`() {
            val behandlingId = UUID.randomUUID()
            val samværsuke = samværsuke(andeler = listOf(MORGEN, KVELD_NATT))
            val samværsavtale =
                samværsavtale(
                    behandlingId = behandlingId,
                    uker = listOf(samværsuke),
                ).tilDto()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = UTREDES)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(behandlingId = behandlingId))
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns true

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)
                }

            assertThat(feil.message).isEqualTo("Kan ikke opprette en samværsavtale for et barn som ikke eksisterer på behandlingen. BehandlingId=$behandlingId")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal opprette en ny samværsavtale dersom det ikke eksisterer en fra før`() {
            val behandlingId = UUID.randomUUID()
            val behandlingBarnId = UUID.randomUUID()
            val samværsuke = samværsuke(andeler = listOf(MORGEN, KVELD_NATT))
            val samværsavtale =
                samværsavtale(
                    behandlingId = behandlingId,
                    behandlingBarnid = behandlingBarnId,
                    uker = listOf(samværsuke),
                ).tilDto()
            val lagretSamværsavtale = slot<Samværsavtale>()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = UTREDES)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(id = behandlingBarnId, behandlingId = behandlingId))
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns true
            every {
                samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)
            } returns null
            every { samværsavtaleRepository.insert(capture(lagretSamværsavtale)) } answers { firstArg() }

            samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)

            verify(exactly = 1) { samværsavtaleRepository.insert(any()) }
            verify(exactly = 0) { samværsavtaleRepository.update(any()) }
            assertThat(lagretSamværsavtale.captured.behandlingId).isEqualTo(behandlingId)
            assertThat(lagretSamværsavtale.captured.behandlingBarnId).isEqualTo(behandlingBarnId)
            assertThat(lagretSamværsavtale.captured.uker.uker.size).isEqualTo(1)
            assertThat(
                lagretSamværsavtale.captured
                    .tilDto()
                    .mapTilSamværsandelerPerDag()
                    .size,
            ).isEqualTo(7)
            assertThat(
                lagretSamværsavtale.captured
                    .tilDto()
                    .summerTilSamværsandelerVerdiPerDag()
                    .sum(),
            ).isEqualTo(35)
        }

        @Test
        internal fun `skal erstatte samværsavtale dersom det eksisterer en fra før`() {
            val behandlingId = UUID.randomUUID()
            val behandlingBarnId = UUID.randomUUID()
            val samværsuke = samværsuke(andeler = listOf(BARNEHAGE_SKOLE, ETTERMIDDAG))
            val samværsavtale =
                samværsavtale(
                    behandlingId = behandlingId,
                    behandlingBarnid = behandlingBarnId,
                    uker = listOf(samværsuke),
                ).tilDto()
            val lagretSamværsavtaleId = UUID.randomUUID()
            val lagretSamværsavtale = slot<Samværsavtale>()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = UTREDES)
            every { barnService.finnBarnPåBehandling(behandlingId) } returns
                    listOf(behandlingBarn(id = behandlingBarnId, behandlingId = behandlingId))
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns true
            every {
                samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)
            } returns
                    samværsavtale(
                        id = lagretSamværsavtaleId,
                        behandlingId = behandlingId,
                        behandlingBarnid = behandlingBarnId,
                    )
            every { samværsavtaleRepository.update(capture(lagretSamværsavtale)) } answers { firstArg() }

            samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale)

            verify(exactly = 0) { samværsavtaleRepository.insert(any()) }
            verify(exactly = 1) { samværsavtaleRepository.update(any()) }
            assertThat(lagretSamværsavtale.captured.behandlingId).isEqualTo(behandlingId)
            assertThat(lagretSamværsavtale.captured.behandlingBarnId).isEqualTo(behandlingBarnId)
            assertThat(lagretSamværsavtale.captured.uker.uker.size).isEqualTo(1)
            assertThat(
                lagretSamværsavtale.captured
                    .tilDto()
                    .mapTilSamværsandelerPerDag()
                    .size,
            ).isEqualTo(7)
            assertThat(
                lagretSamværsavtale.captured
                    .tilDto()
                    .summerTilSamværsandelerVerdiPerDag()
                    .sum(),
            ).isEqualTo(21)
        }
    }

    @Nested
    inner class HentSamværsavtale {
        @Test
        internal fun `skal returnere null dersom samværsavtale ikke finnes fra før`() {
            val behandlingId = UUID.randomUUID()
            every { samværsavtaleRepository.findByBehandlingId(behandlingId) } returns emptyList()
            val samværsavtaler = samværsavtaleService.hentSamværsavtalerForBehandling(behandlingId)
            assertThat(samværsavtaler.isEmpty()).isTrue()
        }

        @Test
        internal fun `skal returnere samværsavtale dersom samværsavtale finnes fra før`() {
            val behandlingId = UUID.randomUUID()
            every { samværsavtaleRepository.findByBehandlingId(behandlingId) } returns
                    listOf(samværsavtale(), samværsavtale(), samværsavtale())
            val samværsavtaler = samværsavtaleService.hentSamværsavtalerForBehandling(behandlingId)
            assertThat(samværsavtaler.size).isEqualTo(3)
        }
    }

    @Nested
    inner class SlettSamværsavtale {
        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "FERDIGSTILT", "SATT_PÅ_VENT"],
            mode = EnumSource.Mode.INCLUDE,
        )
        internal fun `skal ikke kunne slette samværsavtale dersom tilhørende behandling ikke er redigerbar`(behandlingStatus: BehandlingStatus) {
            val behandlingId = UUID.randomUUID()
            val behandlingBarnId = UUID.randomUUID()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId, status = behandlingStatus)

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.slettSamværsavtale(behandlingId, behandlingBarnId)
                }

            assertThat(feil.message).isEqualTo("Behandlingen er låst for videre redigering")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal ikke kunne slette samværsavtale dersom behandlingen tilhører en annen saksbehandler`() {
            val behandlingId = UUID.randomUUID()
            val behandlingBarnId = UUID.randomUUID()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns false

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.slettSamværsavtale(behandlingId, behandlingBarnId)
                }

            assertThat(feil.message).isEqualTo("Behandlingen eies av en annen saksbehandler")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal slette eksisterende samværsavtale`() {
            val behandlingId = UUID.randomUUID()
            val behandlingBarnId = UUID.randomUUID()
            val behandlingIdSlot = slot<UUID>()
            val behandlingBarnSlot = slot<UUID>()

            every { behandlingService.hentBehandling(behandlingId) } returns
                    behandling(id = behandlingId)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)
            } returns true
            every {
                samværsavtaleRepository.deleteByBehandlingIdAndBehandlingBarnId(capture(behandlingIdSlot), capture(behandlingBarnSlot))
            } just Runs

            samværsavtaleService.slettSamværsavtale(behandlingId, behandlingBarnId)

            verify(exactly = 1) { samværsavtaleRepository.deleteByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId) }
            assertThat(behandlingIdSlot.captured).isEqualTo(behandlingId)
            assertThat(behandlingBarnSlot.captured).isEqualTo(behandlingBarnId)
        }
    }

    @Nested
    inner class Gjenbruk {
        @Test
        internal fun `skal kopiere over riktige samværsavtaler fra forrige behandling dersom de eksisterer`() {
            val eksisterendeBehandlingId = UUID.randomUUID()
            val nyBehandlingId = UUID.randomUUID()

            val nyeSamværsavtaler = slot<List<Samværsavtale>>()

            val barnPåBeggeBehandlingerMedSamværsavtale =
                behandlingBarn(
                    behandlingId = eksisterendeBehandlingId,
                    personIdent = "1",
                )
            val barnPåBeggeBehandlingerUtenSamværsavtale =
                behandlingBarn(
                    behandlingId = eksisterendeBehandlingId,
                    personIdent = "2",
                )

            val barnPåEksisterendeBehandling =
                behandlingBarn(
                    behandlingId = eksisterendeBehandlingId,
                    personIdent = "3",
                )
            val barnPåNyBehandling =
                behandlingBarn(
                    behandlingId = nyBehandlingId,
                    personIdent = "4",
                )

            val metadata: HovedregelMetadata =
                hovedregelMetadata(
                    barn =
                    listOf(
                        barnPåBeggeBehandlingerMedSamværsavtale.copy(behandlingId = nyBehandlingId),
                        barnPåNyBehandling,
                        barnPåBeggeBehandlingerUtenSamværsavtale.copy(behandlingId = nyBehandlingId),
                    ),
                )

            val samværsavtaleBarnPåBeggeBehandlinger =
                samværsavtale(
                    behandlingId = eksisterendeBehandlingId,
                    behandlingBarnid = barnPåBeggeBehandlingerMedSamværsavtale.id,
                    uker = listOf(samværsuke(listOf(BARNEHAGE_SKOLE))),
                )
            val samværsavtaleBarnPåEksisterendeBehandling =
                samværsavtale(
                    behandlingId = eksisterendeBehandlingId,
                    behandlingBarnid = barnPåEksisterendeBehandling.id,
                    uker = listOf(samværsuke(listOf(KVELD_NATT))),
                )

            every { samværsavtaleRepository.findByBehandlingId(eksisterendeBehandlingId) } returns
                    listOf(samværsavtaleBarnPåBeggeBehandlinger, samværsavtaleBarnPåEksisterendeBehandling)
            every { barnService.finnBarnPåBehandling(eksisterendeBehandlingId) } returns
                    listOf(barnPåBeggeBehandlingerMedSamværsavtale, barnPåEksisterendeBehandling, barnPåBeggeBehandlingerUtenSamværsavtale)
            every { samværsavtaleRepository.insertAll(capture(nyeSamværsavtaler)) } answers { firstArg() }

            samværsavtaleService.gjenbrukSamværsavtaler(nyBehandlingId, eksisterendeBehandlingId, metadata)

            verify(exactly = 1) { samværsavtaleRepository.insertAll(any()) }
            assertThat(nyeSamværsavtaler.captured.size).isEqualTo(1)
            assertThat(nyeSamværsavtaler.captured.first().behandlingId).isEqualTo(nyBehandlingId)
            assertThat(nyeSamværsavtaler.captured.first().behandlingBarnId).isEqualTo(barnPåBeggeBehandlingerMedSamværsavtale.id)
            assertThat(
                nyeSamværsavtaler.captured
                    .first()
                    .tilDto()
                    .mapTilSamværsandelerPerDag()
                    .size,
            ).isEqualTo(7)
            assertThat(
                nyeSamværsavtaler.captured
                    .first()
                    .tilDto()
                    .summerTilSamværsandelerVerdiPerDag()
                    .sum(),
            ).isEqualTo(14)
        }

        @ParameterizedTest
        @EnumSource(
            value = BehandlingStatus::class,
            names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "FERDIGSTILT", "SATT_PÅ_VENT"],
            mode = EnumSource.Mode.INCLUDE,
        )
        internal fun `skal ikke kunne gjenbruke samværsavtale på behandling som ikke er redigerbar`(status: BehandlingStatus) {
            val behandlingSomSkalOppdateres = behandling(status = status)
            val behandlingForGjenbruk = behandling()
            val barnPåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = VilkårType.ALENEOMSORG, barnId = barnPåBehandlingSomSkalOppdateres.id)

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barnPåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)
                }

            assertThat(feil.message).isEqualTo("Behandlingen er låst og samværsavtale kan ikke gjenbrukes på behandling med id=${behandlingSomSkalOppdateres.id}")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)

        }


        @Test
        internal fun `skal ikke kunne gjenbruke samværsavtale på behandling som tilhører en annen saksbehandler`() {
            val behandlingSomSkalOppdateres = behandling()
            val behandlingForGjenbruk = behandling()
            val barnPåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = VilkårType.ALENEOMSORG, barnId = barnPåBehandlingSomSkalOppdateres.id)

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateres.id)
            } returns false

            val feil: ApiFeil =
                assertThrows {
                    samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barnPåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)
                }

            assertThat(feil.message).isEqualTo("Behandling med id=${behandlingSomSkalOppdateres.id} eies av noen andre og samværsavtale kan derfor ikke oppdateres av deg")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        }

        @Test
        internal fun `skal ikke kunne gjenbruke samværsavtale på behandling som ikke har noen tidligere gjenbrukbare behandlinger`() {
            val fagsakForBehandlingSomSkalOppdateres = fagsak()
            val behandlingSomSkalOppdateres = behandling(fagsak = fagsakForBehandlingSomSkalOppdateres)
            val behandlingForGjenbruk = behandling()
            val barnPåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = VilkårType.ALENEOMSORG, barnId = barnPåBehandlingSomSkalOppdateres.id)

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateres.id)
            } returns true
            every { fagsakService.hentFagsakForBehandling(behandlingSomSkalOppdateres.id) } returns fagsakForBehandlingSomSkalOppdateres
            every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakForBehandlingSomSkalOppdateres.fagsakPersonId) } returns emptyList()

            val feil: Feil =
                assertThrows {
                    samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barnPåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)
                }

            assertThat(feil.message).isEqualTo("Fant ingen tidligere behandlinger som kan benyttes til gjenbruk av samværsavtale for behandling med id=${behandlingSomSkalOppdateres.id}")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Test
        internal fun `skal ikke kunne gjenbruke samværsavtale hvis behandling for gjenbruk ikke er gjenbrukbar`() {
            val fagsakForBehandlingSomSkalOppdateres = fagsak()
            val behandlingSomSkalOppdateres = behandling(fagsak = fagsakForBehandlingSomSkalOppdateres)
            val behandlingForGjenbruk = behandling()
            val barnPåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = VilkårType.ALENEOMSORG, barnId = barnPåBehandlingSomSkalOppdateres.id)

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateres.id)
            } returns true
            every { fagsakService.hentFagsakForBehandling(behandlingSomSkalOppdateres.id) } returns fagsakForBehandlingSomSkalOppdateres
            every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakForBehandlingSomSkalOppdateres.fagsakPersonId) } returns listOf(behandling())

            val feil: Feil =
                assertThrows {
                    samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barnPåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)
                }

            assertThat(feil.message).isEqualTo("Behandling med id=${behandlingForGjenbruk.id} kan ikke benyttes til gjenbruk av samværsavtale for behandling med id=${behandlingSomSkalOppdateres.id}")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @ParameterizedTest
        @EnumSource(
            value = VilkårType::class,
            names = ["ALENEOMSORG"],
            mode = EnumSource.Mode.EXCLUDE,
        )
        internal fun `skal ikke kunne gjenbruke samværsavtale for et vilkår som ikke er av type ALENEOMSORG`(vilkårType: VilkårType) {
            val fagsakForBehandlingSomSkalOppdateres = fagsak()
            val behandlingSomSkalOppdateres = behandling(fagsak = fagsakForBehandlingSomSkalOppdateres)
            val behandlingForGjenbruk = behandling()
            val barnPåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = vilkårType, barnId = barnPåBehandlingSomSkalOppdateres.id)

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateres.id)
            } returns true
            every { fagsakService.hentFagsakForBehandling(behandlingSomSkalOppdateres.id) } returns fagsakForBehandlingSomSkalOppdateres
            every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakForBehandlingSomSkalOppdateres.fagsakPersonId) } returns listOf(behandlingForGjenbruk)

            val feil: Feil =
                assertThrows {
                    samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barnPåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)
                }

            assertThat(feil.message).isEqualTo("Kan ikke gjenbruke samværsavtale for et vilkår som ikke er av type ALENEOMSROG. behandlingId=${vilkårsVurderingSomSkalOppdateres.behandlingId}")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Test
        internal fun `skal ikke kunne gjenbruke samværsavtale for et vilkår uten tilhørende barn`() {
            val fagsakForBehandlingSomSkalOppdateres = fagsak()
            val behandlingSomSkalOppdateres = behandling(fagsak = fagsakForBehandlingSomSkalOppdateres)
            val behandlingForGjenbruk = behandling()
            val barnPåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = VilkårType.ALENEOMSORG, barnId = null)

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateres.id)
            } returns true
            every { fagsakService.hentFagsakForBehandling(behandlingSomSkalOppdateres.id) } returns fagsakForBehandlingSomSkalOppdateres
            every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakForBehandlingSomSkalOppdateres.fagsakPersonId) } returns listOf(behandlingForGjenbruk)

            val feil: Feil =
                assertThrows {
                    samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barnPåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)
                }

            assertThat(feil.message).isEqualTo("Kan ikke gjenbruke samværsavtale for et vilkår som ikke har et barn. behandlingId=${vilkårsVurderingSomSkalOppdateres.behandlingId}")
            assertThat(feil.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Test
        internal fun `skal gjenbruke riktig samværsavtale fra forrige gjenbrukbare behandling`() {
            val fagsakForBehandlingSomSkalOppdateres = fagsak()
            val behandlingSomSkalOppdateres = behandling(fagsak = fagsakForBehandlingSomSkalOppdateres)
            val behandlingForGjenbruk = behandling()
            val barn1PåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "1")
            val barn2PåBehandlingSomSkalOppdateres = behandlingBarn(behandlingId = behandlingSomSkalOppdateres.id, personIdent = "2")
            val barn1PåBehandlingForGjenbruk = behandlingBarn(behandlingId = behandlingForGjenbruk.id, personIdent = "3")
            val barn2PåBehandlingForGjenbruk = behandlingBarn(behandlingId = behandlingForGjenbruk.id, personIdent = "1")
            val vilkårsVurderingSomSkalOppdateres = vilkårsvurdering(behandlingId = behandlingSomSkalOppdateres.id, type = VilkårType.ALENEOMSORG, barnId = barn1PåBehandlingSomSkalOppdateres.id)
            val lagretSamværsavtale = slot<Samværsavtale>()

            every { behandlingService.hentSaksbehandling(behandlingSomSkalOppdateres.id) } returns saksbehandling(behandling = behandlingSomSkalOppdateres)
            every {
                tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateres.id)
            } returns true
            every { fagsakService.hentFagsakForBehandling(behandlingSomSkalOppdateres.id) } returns fagsakForBehandlingSomSkalOppdateres
            every { behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakForBehandlingSomSkalOppdateres.fagsakPersonId) } returns listOf(behandlingForGjenbruk)
            every { barnService.finnBarnPåBehandling(behandlingForGjenbruk.id) } returns listOf(barn1PåBehandlingForGjenbruk, barn2PåBehandlingForGjenbruk)
            every { samværsavtaleRepository.findByBehandlingId(behandlingForGjenbruk.id) } returns listOf(samværsavtale(), samværsavtale(), samværsavtale(behandlingBarnid = barn2PåBehandlingForGjenbruk.id, uker = listOf(samværsuke(andeler = listOf(KVELD_NATT)), samværsuke(andeler = listOf(MORGEN, BARNEHAGE_SKOLE)))))
            every { behandlingService.hentBehandling(behandlingSomSkalOppdateres.id) } returns behandlingSomSkalOppdateres
            every { barnService.finnBarnPåBehandling(behandlingSomSkalOppdateres.id) } returns listOf(barn1PåBehandlingSomSkalOppdateres, barn2PåBehandlingSomSkalOppdateres)
            every { samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandlingSomSkalOppdateres.id, barn1PåBehandlingSomSkalOppdateres.id)} returns samværsavtale()
            every {samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(any(), any())} returns null
            every { samværsavtaleRepository.insert(capture(lagretSamværsavtale)) } answers { firstArg() }

            samværsavtaleService.gjenbrukSamværsavtale(behandlingSomSkalOppdateres.id, behandlingForGjenbruk.id, listOf(barn1PåBehandlingSomSkalOppdateres, barn2PåBehandlingSomSkalOppdateres), vilkårsVurderingSomSkalOppdateres)

            verify(exactly = 1) { samværsavtaleRepository.insert(any()) }
            verify(exactly = 0) { samværsavtaleRepository.update(any()) }
            assertThat(lagretSamværsavtale.captured.behandlingId).isEqualTo(behandlingSomSkalOppdateres.id)
            assertThat(lagretSamværsavtale.captured.behandlingBarnId).isEqualTo(barn1PåBehandlingSomSkalOppdateres.id)
            assertThat(lagretSamværsavtale.captured.uker.uker.size).isEqualTo(2)
            assertThat(
                lagretSamværsavtale.captured
                    .tilDto()
                    .mapTilSamværsandelerPerDag()
                    .size,
            ).isEqualTo(14)
            assertThat(
                lagretSamværsavtale.captured
                    .tilDto()
                    .summerTilSamværsandelerVerdiPerDag()
                    .sum(),
            ).isEqualTo(49)
        }
    }

    @Nested
    inner class Journalføring {
        @Test
        internal fun `skal journalføre samværsberegning`() {
            val arkivRequestSlot = slot<ArkiverDokumentRequest>()
            val journalførRequest = JournalførBeregnetSamværRequest("123", listOf(samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE))), "Notat", "oppsumering")

            BrukerContextUtil.mockBrukerContext()
            every { brevClient.genererFritekstBrev(any()) } returns "1".toByteArray()
            every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull("123") } returns "4489"
            every { fagsakService.finnFagsaker(setOf("123")) } returns listOf(fagsak(eksternId = 1))
            every { personopplysningerService.hentGjeldeneNavn(any()) } returns mapOf("123" to "")
            every { brevsignaturService.lagSaksbehandlerSignatur(any(), any()) } returns SignaturDto(navn = "saksbehandler", enhet = "4489", skjulBeslutter = false)
            every { journalpostClient.arkiverDokument(capture(arkivRequestSlot), any()) } returns ArkiverDokumentResponse("", true)

            samværsavtaleService.journalførBeregnetSamvær(journalførRequest)

            val arkivRequest = arkivRequestSlot.captured

            assertThat(arkivRequest.fnr).isEqualTo("123")
            assertThat(arkivRequest.hoveddokumentvarianter.size).isEqualTo(1)
            assertThat(
                arkivRequest.hoveddokumentvarianter
                    .first()
                    .dokument
                    .toString(Charsets.UTF_8),
            ).isEqualTo("1")
            assertThat(arkivRequest.hoveddokumentvarianter.first().filtype).isEqualTo(Filtype.PDFA)
            assertThat(arkivRequest.hoveddokumentvarianter.first().tittel).isEqualTo("Samværsberegning")
            assertThat(arkivRequest.hoveddokumentvarianter.first().dokumenttype).isEqualTo(Dokumenttype.BEREGNET_SAMVÆR_NOTAT)
            assertThat(arkivRequest.journalførendeEnhet).isEqualTo("4489")
            assertThat(arkivRequest.fagsakId).isEqualTo("1")
        }

        @Test
        internal fun `Skal kaste feil ved journalføring av samværsberegning uten notat`() {
            val arkivRequestSlot = slot<ArkiverDokumentRequest>()
            val journalførRequestTomNotat = JournalførBeregnetSamværRequest("123", listOf(samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE))), "", "oppsumering")

            BrukerContextUtil.mockBrukerContext()
            every { brevClient.genererFritekstBrev(any()) } returns "1".toByteArray()
            every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull("123") } returns "4489"
            every { fagsakService.finnFagsaker(any()) } returns listOf(fagsak(eksternId = 1L))
            every { personopplysningerService.hentGjeldeneNavn(any()) } returns mapOf("123" to "")
            every { brevsignaturService.lagSaksbehandlerSignatur(any(), any()) } returns SignaturDto(navn = "saksbehandler", enhet = "4489", skjulBeslutter = false)
            every { journalpostClient.arkiverDokument(capture(arkivRequestSlot), any()) } returns ArkiverDokumentResponse("", true)

            val feil = assertThrows<ApiFeil> { samværsavtaleService.journalførBeregnetSamvær(journalførRequestTomNotat) }

            assertThat(feil.message).isEqualTo("Kan ikke journalføre samværsavtale uten notat")
        }

        @Test
        internal fun `Skal kaste feil ved journalføring av samværsberegning uten oppsummering`() {
            val arkivRequestSlot = slot<ArkiverDokumentRequest>()
            val journalførRequestTomOppsumering = JournalførBeregnetSamværRequest("123", listOf(samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE))), "Notat", "")

            BrukerContextUtil.mockBrukerContext()
            every { brevClient.genererFritekstBrev(any()) } returns "1".toByteArray()
            every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull("123") } returns "4489"
            every { personopplysningerService.hentGjeldeneNavn(any()) } returns mapOf("123" to "")
            every { fagsakService.finnFagsaker(any()) } returns listOf(fagsak(eksternId = 1))
            every { brevsignaturService.lagSaksbehandlerSignatur(any(), any()) } returns SignaturDto(navn = "saksbehandler", enhet = "4489", skjulBeslutter = false)
            every { journalpostClient.arkiverDokument(capture(arkivRequestSlot), any()) } returns ArkiverDokumentResponse("", true)

            val feil = assertThrows<ApiFeil> { samværsavtaleService.journalførBeregnetSamvær(journalførRequestTomOppsumering) }

            assertThat(feil.message).isEqualTo("Kan ikke journalføre samværsavtale uten oppsumering")
        }

        @Test
        internal fun `Skal kaste feil ved journalføring av samværsberegning på person uten fagsak`() {
            val arkivRequestSlot = slot<ArkiverDokumentRequest>()
            val journalførRequestTomOppsumering = JournalførBeregnetSamværRequest("123", listOf(samværsuke(listOf(KVELD_NATT, MORGEN, BARNEHAGE_SKOLE))), "Notat", "Oppsummering")

            BrukerContextUtil.mockBrukerContext()
            every { brevClient.genererFritekstBrev(any()) } returns "1".toByteArray()
            every { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull("123") } returns "4489"
            every { personopplysningerService.hentGjeldeneNavn(any()) } returns mapOf("123" to "")
            every { fagsakService.finnFagsaker(any()) } returns listOf()
            every { brevsignaturService.lagSaksbehandlerSignatur(any(), any()) } returns SignaturDto(navn = "saksbehandler", enhet = "4489", skjulBeslutter = false)
            every { journalpostClient.arkiverDokument(capture(arkivRequestSlot), any()) } returns ArkiverDokumentResponse("", true)

            val feil = assertThrows<ApiFeil> { samværsavtaleService.journalførBeregnetSamvær(journalførRequestTomOppsumering) }

            assertThat(feil.message).isEqualTo("Kan ikke journalføre samværsavtale på person uten fagsak")
        }
    }
}
