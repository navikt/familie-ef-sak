package no.nav.familie.ef.sak.barn

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknadsverdier
import no.nav.familie.ef.sak.repository.barnMedIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.testutil.tilBehandlingBarn
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.Year
import java.util.UUID

internal class BarnServiceTest {
    val barnRepository = mockk<BarnRepository>()
    val søknadService = mockk<SøknadService>()
    val behandlingService = mockk<BehandlingService>()
    val barnService = BarnService(barnRepository, søknadService, behandlingService)
    val søknadMock = mockk<Søknadsverdier>()
    val fagsakId: UUID = UUID.randomUUID()
    val behandlingId: UUID = UUID.randomUUID()

    val barnSlot = slot<List<BehandlingBarn>>()

    @BeforeEach
    internal fun setUp() {
        barnSlot.clear()
        clearMocks(behandlingService)
        every { søknadService.hentSøknadsgrunnlag(behandlingId) } returns søknadMock
        every { søknadMock.barn } returns emptySet()
        every { barnRepository.insertAll(capture(barnSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal ha både barn fra søknad og grunnlagsdata for barnetilsyn`() {
        val grunnlagsdatabarn =
            listOf(
                barnMedIdent(fnrBarnD, "Barn D"),
                barnMedIdent(fnrBarnC, "Barn C"),
                barnMedIdent(fnrBarnB, "Barn B"),
                barnMedIdent(fnrBarnA, "Barn A"),
            )

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)

        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId,
            UUID.randomUUID(),
            grunnlagsdatabarn,
            BARNETILSYN,
        )

        assertThat(barnSlot.captured).hasSize(4)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC, fnrBarnD)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C", "Barn D")
    }

    @ParameterizedTest
    @EnumSource(
        value = StønadType::class,
        names = ["OVERGANGSSTØNAD", "SKOLEPENGER"],
        mode = EnumSource.Mode.INCLUDE,
    )
    internal fun `skal ha med barn fra søknad og registeret for skolepenger`(stønadstype: StønadType) {
        val grunnlagsdatabarn =
            listOf(
                barnMedIdent(fnrBarnD, "Barn D"),
                barnMedIdent(fnrBarnC, "Barn C"),
                barnMedIdent(fnrBarnB, "Barn B"),
                barnMedIdent(fnrBarnA, "Barn A"),
            )

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)

        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId,
            UUID.randomUUID(),
            grunnlagsdatabarn,
            stønadstype,
        )

        val opprettedeBarn = barnSlot.captured
        assertThat(opprettedeBarn).hasSize(4)
        assertThat(opprettedeBarn.single { it.personIdent == fnrBarnA }.søknadBarnId).isNotNull
        assertThat(opprettedeBarn.single { it.personIdent == fnrBarnB }.søknadBarnId).isNotNull
        assertThat(opprettedeBarn.single { it.personIdent == fnrBarnC }.søknadBarnId).isNull()
        assertThat(opprettedeBarn.single { it.personIdent == fnrBarnD }.søknadBarnId).isNull()
    }

    @Test
    internal fun `revurdering uten nye barn skal ta med terminbarn fra forrige behandling`() {
        val grunnlagsdatabarn =
            listOf(
                barnMedIdent(fnrBarnD, "Barn D", fødsel(fødselsdatoBarnD)),
            )

        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, terminbarnPåSøknad)
        every { barnRepository.findByBehandlingId(any()) } returns
            søknadBarnTilBehandlingBarn(
                setOf(
                    barnPåSøknadA,
                    terminbarnPåSøknad,
                ),
                forrigeBehandlingId,
            )
        val nyeBarnPåRevurdering = emptyList<BehandlingBarn>()
        barnService.opprettBarnForRevurdering(
            behandlingId,
            forrigeBehandlingId,
            nyeBarnPåRevurdering,
            grunnlagsdatabarn,
            StønadType.OVERGANGSSTØNAD,
        )

        assertThat(barnSlot.captured).hasSize(2)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, null)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Terminbarn 1")
    }

    @Test
    internal fun `skal ta med ett nytt barn ved revurdering av Overgangsstønad hvor to barn eksisterer fra før`() {
        val grunnlagsdatabarn =
            listOf(
                barnMedIdent(fnrBarnC, "Barn C", fødsel(fødselsdatoBarnC)),
                barnMedIdent(fnrBarnB, "Barn B", fødsel(fødselsdatoBarnB)),
                barnMedIdent(fnrBarnA, "Barn A", fødsel(fødselsdatoBarnA)),
            )
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns
            søknadBarnTilBehandlingBarn(
                setOf(
                    barnPåSøknadA,
                    barnPåSøknadB,
                ),
                forrigeBehandlingId,
            )
        val nyeBarnPåRevurdering =
            listOf(
                BehandlingBarn(
                    behandlingId = behandlingId,
                    søknadBarnId = null,
                    personIdent = fnrBarnC,
                    navn = "Barn C",
                ),
            )
        barnService.opprettBarnForRevurdering(
            behandlingId,
            forrigeBehandlingId,
            nyeBarnPåRevurdering,
            grunnlagsdatabarn,
            StønadType.OVERGANGSSTØNAD,
        )

        assertThat(barnSlot.captured).hasSize(3)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C")
    }

    @Test
    internal fun `skal kaste feil ved revurdering av Barnetilsyn dersom man ikke tar med alle barna som finnes i grunnlagsdataene`() {
        val grunnlagsdatabarn =
            listOf(
                barnMedIdent(fnrBarnD, "Barn D", fødsel(fødselsdatoBarnD)),
                barnMedIdent(fnrBarnC, "Barn C", fødsel(fødselsdatoBarnC)),
                barnMedIdent(fnrBarnB, "Barn B", fødsel(fødselsdatoBarnB)),
                barnMedIdent(fnrBarnA, "Barn A", fødsel(fødselsdatoBarnA)),
            )
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns
            søknadBarnTilBehandlingBarn(
                setOf(
                    barnPåSøknadA,
                    barnPåSøknadB,
                ),
                forrigeBehandlingId,
            )
        every { barnRepository.insertAll(capture(barnSlot)) } returns emptyList()

        val nyeBarnPåRevurdering =
            listOf(
                BehandlingBarn(
                    behandlingId = behandlingId,
                    søknadBarnId = null,
                    personIdent = fnrBarnC,
                    navn = "Barn C",
                ),
            )
        val feil =
            assertThrows<Feil> {
                barnService.opprettBarnForRevurdering(
                    behandlingId,
                    forrigeBehandlingId,
                    nyeBarnPåRevurdering,
                    grunnlagsdatabarn,
                    BARNETILSYN,
                )
            }

        assertThat(feil.message).contains("Alle barn skal være med i revurderingen av en barnetilsynbehandling.")
    }

    @Test
    internal fun `skal ta med alle nye barn ved revurdering av Barnetilsyn hvor to barn eksisterer fra før`() {
        val grunnlagsdatabarn =
            listOf(
                barnMedIdent(fnrBarnD, "Barn D", fødsel(fødselsdatoBarnD)),
                barnMedIdent(fnrBarnC, "Barn C", fødsel(fødselsdatoBarnC)),
                barnMedIdent(fnrBarnB, "Barn B", fødsel(fødselsdatoBarnB)),
                barnMedIdent(fnrBarnA, "Barn A", fødsel(fødselsdatoBarnA)),
            )
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns
            søknadBarnTilBehandlingBarn(
                setOf(
                    barnPåSøknadA,
                    barnPåSøknadB,
                ),
                forrigeBehandlingId,
            )
        val nyeBarnPåRevurdering =
            listOf(
                BehandlingBarn(
                    behandlingId = behandlingId,
                    søknadBarnId = null,
                    personIdent = fnrBarnD,
                    navn = "Barn C",
                ),
                BehandlingBarn(
                    behandlingId = behandlingId,
                    søknadBarnId = null,
                    personIdent = fnrBarnC,
                    navn = "Barn C",
                ),
            )
        barnService.opprettBarnForRevurdering(
            behandlingId,
            forrigeBehandlingId,
            nyeBarnPåRevurdering,
            grunnlagsdatabarn,
            BARNETILSYN,
        )

        assertThat(barnSlot.captured).hasSize(4)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC, fnrBarnD)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C", "Barn D")
    }

    @Test
    internal fun `skal koble terminbarn med barn fra grunnlagsdata`() {
        val fødselTermindato = LocalDate.now().minusDays(1)

        val pdlTerminbarn = barnMedIdent(FnrGenerator.generer(fødselTermindato), "J B")
        val barnOver18 = barnMedIdent(fnrBarnOver18, "Barn Over 18", fødsel(år = 1986, 1, 1))
        val grunnlagsdatabarn =
            listOf(
                barnOver18,
                pdlTerminbarn,
            )

        every { søknadMock.barn } returns setOf(terminbarnPåSøknad)

        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId,
            fagsakId,
            grunnlagsdatabarn,
            StønadType.OVERGANGSSTØNAD,
            UstrukturertDokumentasjonType.IKKE_VALGT,
            vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT,
        )

        assertThat(barnSlot.captured).hasSize(2)
        assertThat(barnSlot.captured[0].personIdent).isEqualTo(pdlTerminbarn.personIdent)
        assertThat(barnSlot.captured[1].personIdent).isEqualTo(barnOver18.personIdent)
    }

    @Nested
    inner class TerminbarnFraPapirsøknad {
        @Test
        internal fun `skal ikke kunne sende inn terminbarn på annen behandling enn papirsøknad`() {
            val termindato = LocalDate.of(2021, 1, 1)
            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                emptyList(),
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.PAPIRSØKNAD,
                listOf(BarnSomSkalFødes(termindato)),
            )
        }

        @Test
        internal fun `skal opprette terminbarn når det ikke finnes match i PDL`() {
            val termindato = LocalDate.of(2021, 1, 1)
            assertThatThrownBy {
                barnService.opprettBarnPåBehandlingMedSøknadsdata(
                    behandlingId,
                    fagsakId,
                    emptyList(),
                    StønadType.OVERGANGSSTØNAD,
                    UstrukturertDokumentasjonType.IKKE_VALGT,
                    listOf(BarnSomSkalFødes(termindato)),
                )
            }.hasMessage("Kan ikke legge til terminbarn med ustrukturertDokumentasjonType=IKKE_VALGT")
        }

        @Test
        internal fun `skal opprette barn med ident når terminbarn finnes med match i PDL`() {
            val termindato = LocalDate.of(2021, 4, 16)
            val fnr = FnrGenerator.generer(termindato)
            val barnMedIdent = barnMedIdent(fnr, "Barn D").copy(fødsel = listOf(fødsel(termindato)))

            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                listOf(barnMedIdent),
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.PAPIRSØKNAD,
                listOf(BarnSomSkalFødes(termindato)),
            )
            assertThat(barnSlot.captured).hasSize(1)
            assertThat(barnSlot.captured[0].fødselTermindato).isEqualTo(termindato)
            assertThat(barnSlot.captured[0].behandlingId).isEqualTo(behandlingId)
            assertThat(barnSlot.captured[0].personIdent).isEqualTo(fnr)
            assertThat(barnSlot.captured[0].navn).isEqualTo("Barn D")
            assertThat(barnSlot.captured[0].søknadBarnId).isNull()
        }

        @Test
        internal fun `skal ta med barn fra forrige behandling som var under 18`() {
            val eksisterendeBarn = barnMedIdent(fnrBarnA, "Barn A")
            val barnOver18 = barnMedIdent(fnrBarnOver18, "Barn Over 18", fødsel(år = 1986, 1, 1))
            val grunnlagsdatabarn =
                listOf(
                    barnOver18,
                    eksisterendeBarn,
                )

            val forrigeBehandlingId = UUID.randomUUID()
            val barnPåForrigeBehandling =
                listOf(
                    barnPåSøknadA.tilBehandlingBarn(forrigeBehandlingId),
                    barnOver18.tilBehandlingBarn(forrigeBehandlingId),
                )

            every { barnRepository.findByBehandlingId(forrigeBehandlingId) } returns barnPåForrigeBehandling
            barnService.opprettBarnForRevurdering(
                behandlingId = behandlingId,
                forrigeBehandlingId = forrigeBehandlingId,
                emptyList(),
                grunnlagsdataBarn = grunnlagsdatabarn,
                stønadstype = BARNETILSYN,
            )

            assertThat(barnSlot.captured).hasSize(2)
        }

        @Test
        internal fun `skal ta med barn over 18 som ikke har innslag i forrige revurdering`() {
            val eksisterendeBarn = barnMedIdent(fnrBarnA, "Barn A")
            val barnOver18 = barnMedIdent(fnrBarnOver18, "Barn Over 18", fødsel(år = 1986, 1, 1))
            val grunnlagsdatabarn =
                listOf(
                    barnOver18,
                    eksisterendeBarn,
                )

            val forrigeBehandlingId = UUID.randomUUID()
            val søknadsBarnTilBehandlingBarn = listOf(barnPåSøknadA.tilBehandlingBarn(forrigeBehandlingId))

            every { barnRepository.findByBehandlingId(forrigeBehandlingId) } returns søknadsBarnTilBehandlingBarn

            val nyeBarnPåRevurdering =
                listOf(
                    BehandlingBarn(
                        behandlingId = behandlingId,
                        søknadBarnId = null,
                        personIdent = fnrBarnOver18,
                        navn = "Barn over 18",
                    ),
                )

            barnService.opprettBarnForRevurdering(
                behandlingId = behandlingId,
                forrigeBehandlingId = forrigeBehandlingId,
                nyeBarnPåRevurdering = nyeBarnPåRevurdering,
                grunnlagsdataBarn = grunnlagsdatabarn,
                stønadstype = BARNETILSYN,
            )

            assertThat(barnSlot.captured).hasSize(2)
        }

        @Test
        internal fun `skal legge til terminbarn og andre terminbarn for papirsøknader`() {
            val termindato = LocalDate.of(2021, 4, 16)
            val fnr = FnrGenerator.generer(termindato)
            val fnr2 = FnrGenerator.generer(termindato)
            val barnMedIdent = barnMedIdent(fnr, "Terminbarn A").copy(fødsel = listOf(fødsel(termindato)))
            val barnMedIdent2 = barnMedIdent(fnr2, "Barn D")

            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                listOf(barnMedIdent, barnMedIdent2),
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.PAPIRSØKNAD,
                listOf(BarnSomSkalFødes(termindato)),
            )
            assertThat(barnSlot.captured).hasSize(2)
            assertThat(barnSlot.captured[0].fødselTermindato).isEqualTo(termindato)
            assertThat(barnSlot.captured[0].behandlingId).isEqualTo(behandlingId)
            assertThat(barnSlot.captured[0].personIdent).isEqualTo(fnr)
            assertThat(barnSlot.captured[0].navn).isEqualTo("Terminbarn A")
            assertThat(barnSlot.captured[0].søknadBarnId).isNull()

            assertThat(barnSlot.captured[1].fødselTermindato).isNull()
            assertThat(barnSlot.captured[1].behandlingId).isEqualTo(behandlingId)
            assertThat(barnSlot.captured[1].personIdent).isEqualTo(fnr2)
            assertThat(barnSlot.captured[1].navn).isEqualTo("Barn D")
            assertThat(barnSlot.captured[1].søknadBarnId).isNull()
        }

        @Test
        internal fun `skal ha med barn over 18 år`() {
            val årOver18år = Year.now().minusYears(19).value
            val grunnlagsdataBarn =
                listOf(
                    barnMedIdent(FnrGenerator.generer(Year.now().minusYears(1).value), "Under 18"),
                    barnMedIdent(FnrGenerator.generer(årOver18år), "Over 18", fødsel(årOver18år)),
                )
            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                grunnlagsdataBarn,
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.PAPIRSØKNAD,
            )

            assertThat(barnSlot.captured).hasSize(2)
            assertThat(barnSlot.captured[0].navn).isEqualTo("Under 18")
            assertThat(barnSlot.captured[1].navn).isEqualTo("Over 18")
        }
    }

    @Nested
    inner class Ettersending {
        private val grunnlagsdataBarn =
            listOf(
                barnMedIdent(FnrGenerator.generer(), "J B", fødsel(LocalDate.now().minusYears(1))),
            )
        private val fødselTermindato = LocalDate.now().minusDays(1)
        private val tidligereBehandling = behandling()
        private val barnPåForrigeBehandling =
            listOf(
                BehandlingBarn(
                    behandlingId = tidligereBehandling.id,
                    søknadBarnId = UUID.randomUUID(),
                    personIdent = "1",
                    navn = "1",
                ),
                BehandlingBarn(
                    behandlingId = tidligereBehandling.id,
                    søknadBarnId = UUID.randomUUID(),
                    fødselTermindato = fødselTermindato,
                    navn = "asd",
                ),
            )

        @BeforeEach
        internal fun setUp() {
            println(grunnlagsdataBarn)
            println(barnPåForrigeBehandling)
            every { behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId) } returns tidligereBehandling
            every { barnRepository.findByBehandlingId(tidligereBehandling.id) } returns emptyList()
        }

        @Test
        internal fun `skal legge til registerbarn på behandling hvis man skal vilkårsbehandle nye barn`() {
            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                grunnlagsdataBarn,
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.ETTERSENDING,
                vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            )

            assertThat(barnSlot.captured).hasSize(1)
            assertThat(barnSlot.captured[0].navn).isEqualTo("J B")
        }

        @Test
        internal fun `skal beholde terminbarn fra forrige behandling`() {
            every { barnRepository.findByBehandlingId(tidligereBehandling.id) } returns barnPåForrigeBehandling

            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                emptyList(),
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.ETTERSENDING,
                vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            )

            assertThat(barnSlot.captured).hasSize(1)
            assertThat(barnSlot.captured[0].personIdent).isNull()
            assertThat(barnSlot.captured[0].fødselTermindato).isEqualTo(fødselTermindato)

            // skal ikke mappe søknadBarnId fra tidligere BehandlingBarn
            assertThat(barnPåForrigeBehandling.map { it.søknadBarnId }).doesNotContainNull()
            assertThat(barnSlot.captured[0].søknadBarnId).isNull()

            verify(exactly = 1) { behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId) }
        }

        @Test
        internal fun `skal beholde terminbarn fra forrige behandling og legge til grunnlagsdatabarn`() {
            every { barnRepository.findByBehandlingId(tidligereBehandling.id) } returns barnPåForrigeBehandling

            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                grunnlagsdataBarn,
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.ETTERSENDING,
                vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            )

            assertThat(barnSlot.captured).hasSize(2)
            assertThat(barnSlot.captured.single { it.personIdent != null }.personIdent)
                .isEqualTo(grunnlagsdataBarn[0].personIdent)
            assertThat(barnSlot.captured.single { it.personIdent == null }.fødselTermindato).isEqualTo(fødselTermindato)
        }

        @Test
        internal fun `skal koble terminbarn med barn fra grunnlagsdata`() {
            val barnFraRegister = barnMedIdent(FnrGenerator.generer(fødselTermindato), "J B")

            every { barnRepository.findByBehandlingId(tidligereBehandling.id) } returns barnPåForrigeBehandling

            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                listOf(barnFraRegister),
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.ETTERSENDING,
                vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            )

            assertThat(barnSlot.captured).hasSize(1)
            assertThat(barnSlot.captured[0].personIdent).isEqualTo(barnFraRegister.personIdent)
            assertThat(barnSlot.captured[0].fødselTermindato).isEqualTo(fødselTermindato)
        }

        @Test
        internal fun `skal ikke legge til registerbarn på behandling hvis man ikke skal vilkårsbehandle nye barn`() {
            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                grunnlagsdataBarn,
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.ETTERSENDING,
                vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE,
            )

            assertThat(barnSlot.captured).isEmpty()
        }

        @Test
        internal fun `kan ikke velge IKKE_VILKÅRSBEHANDLE hvis det finnes barn på forrige behandlingen`() {
            every { barnRepository.findByBehandlingId(tidligereBehandling.id) } returns barnPåForrigeBehandling

            assertThatThrownBy {
                barnService.opprettBarnPåBehandlingMedSøknadsdata(
                    behandlingId,
                    fagsakId,
                    grunnlagsdataBarn,
                    StønadType.OVERGANGSSTØNAD,
                    UstrukturertDokumentasjonType.ETTERSENDING,
                    vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE,
                )
            }.hasMessageContaining("Må behandle nye barn hvis det finnes barn på forrige behandling")
        }

        @Test
        internal fun `skal kaste feil hvis vilkårsbehandleNyeBarn ikke er valgt`() {
            assertThatThrownBy {
                barnService.opprettBarnPåBehandlingMedSøknadsdata(
                    behandlingId,
                    fagsakId,
                    grunnlagsdataBarn,
                    StønadType.OVERGANGSSTØNAD,
                    UstrukturertDokumentasjonType.ETTERSENDING,
                )
            }.hasMessage("Må ha valgt om man skal vilkårsbehandle nye barn når man ettersender på ny behandling")
        }
    }

    @Nested
    inner class ValiderBarnFinnesPåBehandling {
        private val barn = BehandlingBarn(id = UUID.randomUUID(), behandlingId = UUID.randomUUID(), søknadBarnId = null)
        private val barn2 =
            BehandlingBarn(id = UUID.randomUUID(), behandlingId = UUID.randomUUID(), søknadBarnId = null)
        private val barn3 =
            BehandlingBarn(id = UUID.randomUUID(), behandlingId = UUID.randomUUID(), søknadBarnId = null)

        @Test
        internal fun `tom liste med barn validerer`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barn)
            barnService.validerBarnFinnesPåBehandling(UUID.randomUUID(), setOf())
        }

        @Test
        internal fun `flere barn mangler`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barn)
            assertThatThrownBy {
                barnService.validerBarnFinnesPåBehandling(UUID.randomUUID(), setOf(barn.id, barn2.id, barn3.id))
            }.isInstanceOf(Feil::class.java)
        }

        @Test
        internal fun `1 barn mangler`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barn, barn3)
            assertThatThrownBy {
                barnService.validerBarnFinnesPåBehandling(UUID.randomUUID(), setOf(barn.id, barn2.id, barn3.id))
            }.isInstanceOf(Feil::class.java)
        }

        @Test
        internal fun `antall barn er eksakt like`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barn, barn2)
            barnService.validerBarnFinnesPåBehandling(UUID.randomUUID(), setOf(barn.id, barn2.id))
        }

        @Test
        internal fun `innsendte barn er færre enn de som finnes på behandlingen`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barn, barn2)
            barnService.validerBarnFinnesPåBehandling(UUID.randomUUID(), setOf(barn2.id))
        }
    }

    @Nested
    inner class MapTidligereBarnIdTilNårværende {
        private val barnA = BehandlingBarn(behandlingId = UUID.randomUUID(), personIdent = fnrBarnA)
        private val tidligereBarnA = BehandlingBarn(behandlingId = UUID.randomUUID(), personIdent = fnrBarnA)

        @Test
        internal fun `skal mappe tidligereBarnTilNåværende`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barnA)
            every { barnRepository.findAllById(any()) } returns listOf(tidligereBarnA)

            val map = barnService.kobleBarnForBarnetilsyn(UUID.randomUUID(), setOf(tidligereBarnA.id))
            assertThat(map.toList()).containsExactly(tidligereBarnA.id to barnA.id)
        }

        @Test
        internal fun `skal kaste feil hvis barn på behandlingen mangler personIdent`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barnA.copy(personIdent = null))

            assertThatThrownBy {
                barnService.kobleBarnForBarnetilsyn(UUID.randomUUID(), setOf(tidligereBarnA.id))
            }.hasMessageContaining("Mangler ident for barn=${barnA.id}")
        }

        @Test
        internal fun `skal kaste feil hvis tidligere barn mangler personIdent`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barnA)
            every { barnRepository.findAllById(any()) } returns listOf(tidligereBarnA.copy(personIdent = null))

            assertThatThrownBy {
                barnService.kobleBarnForBarnetilsyn(UUID.randomUUID(), setOf(tidligereBarnA.id))
            }.hasMessageContaining("Mangler ident for barn=${tidligereBarnA.id}")
        }

        @Test
        internal fun `skal kaste feil hvis det ikke finnes en match`() {
            every { barnRepository.findByBehandlingId(any()) } returns listOf(barnA)
            every { barnRepository.findAllById(any()) } returns listOf(tidligereBarnA.copy(personIdent = "abc"))

            assertThatThrownBy {
                barnService.kobleBarnForBarnetilsyn(UUID.randomUUID(), setOf(tidligereBarnA.id))
            }.hasMessageContaining("Fant ikke match for barn med ident=abc")
        }
    }

    val fnrBarnA = FnrGenerator.generer()
    val fnrBarnOver18 = FnrGenerator.generer()
    val fnrBarnB = FnrGenerator.generer()
    val fnrBarnC = FnrGenerator.generer()
    val fnrBarnD = FnrGenerator.generer()
    val fødselsdatoBarnA = LocalDate.now().minusYears(1)
    val fødselsdatoBarnB = LocalDate.now().minusYears(2)
    val fødselsdatoBarnC = LocalDate.now().minusYears(4)
    val fødselsdatoBarnD = LocalDate.now().minusYears(4)

    val barnPåSøknadA =
        SøknadBarn(
            id = UUID.randomUUID(),
            navn = "Barn A",
            fødselsnummer = fnrBarnA,
            harSkalHaSammeAdresse = false,
            ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
            erBarnetFødt = true,
            skalHaBarnepass = true,
            lagtTilManuelt = false,
        )
    val barnPåSøknadB =
        SøknadBarn(
            id = UUID.randomUUID(),
            navn = "Barn B",
            fødselsnummer = fnrBarnB,
            harSkalHaSammeAdresse = false,
            ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
            erBarnetFødt = true,
            skalHaBarnepass = true,
            lagtTilManuelt = false,
        )
    val terminbarnPåSøknad =
        SøknadBarn(
            id = UUID.randomUUID(),
            navn = "Terminbarn 1",
            fødselsnummer = null,
            harSkalHaSammeAdresse = false,
            ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
            erBarnetFødt = false,
            skalHaBarnepass = true,
            lagtTilManuelt = true,
            fødselTermindato = LocalDate.now(),
        )
}
