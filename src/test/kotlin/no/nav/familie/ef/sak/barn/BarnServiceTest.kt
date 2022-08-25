package no.nav.familie.ef.sak.barn

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknadsverdier
import no.nav.familie.ef.sak.repository.barnMedIdent
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.søknadsBarnTilBehandlingBarn
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.Year
import java.util.UUID

internal class BarnServiceTest {

    val barnRepository = mockk<BarnRepository>()
    val søknadService = mockk<SøknadService>()
    val barnService = BarnService(barnRepository, søknadService)
    val søknadMock = mockk<Søknadsverdier>()
    val fagsakId: UUID = UUID.randomUUID()
    val behandlingId: UUID = UUID.randomUUID()

    val barnSlot = slot<List<BehandlingBarn>>()

    @BeforeEach
    internal fun setUp() {
        barnSlot.clear()
        every { søknadService.hentSøknadsgrunnlag(behandlingId) } returns søknadMock
        every { søknadMock.barn } returns emptySet()
        every { barnRepository.insertAll(capture(barnSlot)) } answers { firstArg() }
    }

    @Test
    internal fun `skal ha både barn fra søknad og grunnlagsdata for barnetilsyn`() {
        val grunnlagsdatabarn = listOf(
            barnMedIdent(fnrBarnD, "Barn D"),
            barnMedIdent(fnrBarnC, "Barn C"),
            barnMedIdent(fnrBarnB, "Barn B"),
            barnMedIdent(fnrBarnA, "Barn A")
        )

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)

        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId,
            UUID.randomUUID(),
            grunnlagsdatabarn,
            StønadType.BARNETILSYN
        )

        assertThat(barnSlot.captured).hasSize(4)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC, fnrBarnD)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C", "Barn D")
    }

    @Test
    internal fun `skal kun ha barn fra søknad for overgangsstønad`() {
        val grunnlagsdatabarn = listOf(
            barnMedIdent(fnrBarnD, "Barn D"),
            barnMedIdent(fnrBarnC, "Barn C"),
            barnMedIdent(fnrBarnB, "Barn B"),
            barnMedIdent(fnrBarnA, "Barn A")
        )

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)

        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId,
            UUID.randomUUID(),
            grunnlagsdatabarn,
            StønadType.OVERGANGSSTØNAD
        )

        assertThat(barnSlot.captured).hasSize(2)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B")
    }

    @Test
    internal fun `skal kun ha med barn fra søknad for skolepenger`() {
        val grunnlagsdatabarn = listOf(
            barnMedIdent(fnrBarnD, "Barn D"),
            barnMedIdent(fnrBarnC, "Barn C"),
            barnMedIdent(fnrBarnB, "Barn B"),
            barnMedIdent(fnrBarnA, "Barn A")
        )

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)

        barnService.opprettBarnPåBehandlingMedSøknadsdata(
            behandlingId,
            UUID.randomUUID(),
            grunnlagsdatabarn,
            StønadType.SKOLEPENGER
        )

        assertThat(barnSlot.captured).hasSize(2)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B")
    }

    @Test
    internal fun `skal ta med ett nytt barn ved revurdering av Overgangsstønad hvor to barn eksisterer fra før`() {
        val grunnlagsdatabarn = listOf(
            barnMedIdent(fnrBarnD, "Barn D"),
            barnMedIdent(fnrBarnC, "Barn C"),
            barnMedIdent(fnrBarnB, "Barn B"),
            barnMedIdent(fnrBarnA, "Barn A")
        )
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns søknadsBarnTilBehandlingBarn(
            setOf(
                barnPåSøknadA,
                barnPåSøknadB
            ),
            forrigeBehandlingId
        )
        val nyeBarnPåRevurdering = listOf(
            BehandlingBarn(
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = fnrBarnC,
                navn = "Barn C"
            )
        )
        barnService.opprettBarnForRevurdering(
            behandlingId,
            forrigeBehandlingId,
            nyeBarnPåRevurdering,
            grunnlagsdatabarn,
            StønadType.OVERGANGSSTØNAD
        )

        assertThat(barnSlot.captured).hasSize(3)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C")
    }

    @Test
    internal fun `skal kaste feil ved revurdering av Barnetilsyn dersom man ikke tar med alle barna som finnes i grunnlagsdataene`() {
        val grunnlagsdatabarn = listOf(
            barnMedIdent(fnrBarnD, "Barn D"),
            barnMedIdent(fnrBarnC, "Barn C"),
            barnMedIdent(fnrBarnB, "Barn B"),
            barnMedIdent(fnrBarnA, "Barn A")
        )
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns søknadsBarnTilBehandlingBarn(
            setOf(
                barnPåSøknadA,
                barnPåSøknadB
            ),
            forrigeBehandlingId
        )
        every { barnRepository.insertAll(capture(barnSlot)) } returns emptyList()

        val nyeBarnPåRevurdering = listOf(
            BehandlingBarn(
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = fnrBarnC,
                navn = "Barn C"
            )
        )
        val feil = assertThrows<Feil> {
            barnService.opprettBarnForRevurdering(
                behandlingId,
                forrigeBehandlingId,
                nyeBarnPåRevurdering,
                grunnlagsdatabarn,
                StønadType.BARNETILSYN
            )
        }

        assertThat(feil.message).contains("Alle barn skal være med i revurderingen av en barnetilsynbehandling.")
    }

    @Test
    internal fun `skal ta med alle nye barn ved revurdering av Barnetilsyn hvor to barn eksisterer fra før`() {
        val grunnlagsdatabarn = listOf(
            barnMedIdent(fnrBarnD, "Barn D"),
            barnMedIdent(fnrBarnC, "Barn C"),
            barnMedIdent(fnrBarnB, "Barn B"),
            barnMedIdent(fnrBarnA, "Barn A")
        )
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns søknadsBarnTilBehandlingBarn(
            setOf(
                barnPåSøknadA,
                barnPåSøknadB
            ),
            forrigeBehandlingId
        )
        val nyeBarnPåRevurdering = listOf(
            BehandlingBarn(
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = fnrBarnD,
                navn = "Barn C"
            ),
            BehandlingBarn(
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = fnrBarnC,
                navn = "Barn C"
            )
        )
        barnService.opprettBarnForRevurdering(
            behandlingId,
            forrigeBehandlingId,
            nyeBarnPåRevurdering,
            grunnlagsdatabarn,
            StønadType.BARNETILSYN
        )

        assertThat(barnSlot.captured).hasSize(4)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC, fnrBarnD)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C", "Barn D")
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
                listOf(BarnSomSkalFødes(termindato))
            )
        }

        @Test
        internal fun `skal opprette terminbarn når det ikke finnes match i PDL`() {
            val termindato = LocalDate.of(2021, 1, 1)
            assertThatThrownBy {  barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                emptyList(),
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.IKKE_VALGT,
                listOf(BarnSomSkalFødes(termindato))
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
                listOf(BarnSomSkalFødes(termindato))
            )
            assertThat(barnSlot.captured).hasSize(1)
            assertThat(barnSlot.captured[0].fødselTermindato).isEqualTo(termindato)
            assertThat(barnSlot.captured[0].behandlingId).isEqualTo(behandlingId)
            assertThat(barnSlot.captured[0].personIdent).isEqualTo(fnr)
            assertThat(barnSlot.captured[0].navn).isEqualTo("Barn D")
            assertThat(barnSlot.captured[0].søknadBarnId).isNull()
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
                listOf(BarnSomSkalFødes(termindato))
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
        internal fun `skal kun ha med barn under 18 år`() {
            val årOver18år = Year.now().minusYears(19).value
            val grunnlagsdataBarn = listOf(
                barnMedIdent(FnrGenerator.generer(Year.now().minusYears(1).value), "Under 18"),
                barnMedIdent(FnrGenerator.generer(årOver18år), "Over 18", fødsel(årOver18år))
            )
            barnService.opprettBarnPåBehandlingMedSøknadsdata(
                behandlingId,
                fagsakId,
                grunnlagsdataBarn,
                StønadType.OVERGANGSSTØNAD,
                UstrukturertDokumentasjonType.PAPIRSØKNAD
            )

            assertThat(barnSlot.captured).hasSize(1)
            assertThat(barnSlot.captured[0].navn).isEqualTo("Under 18")
        }
    }

    @Nested
    inner class ValiderBarnFinnesPåBehandling {

        private val barn = BehandlingBarn(id = UUID.randomUUID(), behandlingId = UUID.randomUUID(), søknadBarnId = null)
        private val barn2 = BehandlingBarn(id = UUID.randomUUID(), behandlingId = UUID.randomUUID(), søknadBarnId = null)
        private val barn3 = BehandlingBarn(id = UUID.randomUUID(), behandlingId = UUID.randomUUID(), søknadBarnId = null)

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

    val fnrBarnA = "11111111111"
    val fnrBarnB = "22222222222"
    val fnrBarnC = "33333333333"
    val fnrBarnD = "44444444444"
    val barnPåSøknadA = SøknadBarn(
        id = UUID.randomUUID(),
        navn = "Barn A",
        fødselsnummer = fnrBarnA,
        harSkalHaSammeAdresse = false,
        ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
        erBarnetFødt = true,
        skalHaBarnepass = true,
        lagtTilManuelt = false
    )
    val barnPåSøknadB = SøknadBarn(
        id = UUID.randomUUID(),
        navn = "Barn B",
        fødselsnummer = fnrBarnB,
        harSkalHaSammeAdresse = false,
        ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
        erBarnetFødt = true,
        skalHaBarnepass = true,
        lagtTilManuelt = false
    )
}
