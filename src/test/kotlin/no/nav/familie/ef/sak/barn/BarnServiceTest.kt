package no.nav.familie.ef.sak.barn

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknadsverdier
import no.nav.familie.ef.sak.testutil.søknadsBarnTilBehandlingBarn
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class BarnServiceTest {


    val barnRepository = mockk<BarnRepository>()
    val søknadService = mockk<SøknadService>()
    val barnService = BarnService(barnRepository, søknadService)
    val søknadMock = mockk<Søknadsverdier>()
    val behandlingId: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {

        every { søknadService.hentSøknadsgrunnlag(behandlingId) } returns søknadMock
    }

    @Test
    internal fun `skal ha både barn fra søknad og grunnlagsdata for barnetilsyn`() {
        val grunnlagsdatabarn = listOf(barnMedIdent(fnrBarnD, "Barn D"),
                                       barnMedIdent(fnrBarnC, "Barn C"),
                                       barnMedIdent(fnrBarnB, "Barn B"),
                                       barnMedIdent(fnrBarnA, "Barn A"))
        val barnSlot = slot<List<BehandlingBarn>>()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.insertAll(capture(barnSlot)) } returns emptyList()

        barnService.opprettBarnPåBehandlingMedSøknadsdata(behandlingId,
                                                          UUID.randomUUID(),
                                                          grunnlagsdatabarn,
                                                          StønadType.BARNETILSYN)

        assertThat(barnSlot.captured).hasSize(4)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC, fnrBarnD)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C", "Barn D")

    }

    @Test
    internal fun `skal kun ha barn fra søknad for overgangsstønad`() {
        val grunnlagsdatabarn = listOf(barnMedIdent(fnrBarnD, "Barn D"),
                                       barnMedIdent(fnrBarnC, "Barn C"),
                                       barnMedIdent(fnrBarnB, "Barn B"),
                                       barnMedIdent(fnrBarnA, "Barn A"))
        val barnSlot = slot<List<BehandlingBarn>>()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.insertAll(capture(barnSlot)) } returns emptyList()

        barnService.opprettBarnPåBehandlingMedSøknadsdata(behandlingId,
                                                          UUID.randomUUID(),
                                                          grunnlagsdatabarn,
                                                          StønadType.OVERGANGSSTØNAD)

        assertThat(barnSlot.captured).hasSize(2)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B")

    }

    @Test
    internal fun `skal feile hvis man prøver å opprette barn for skolepenger`() {
        assertThrows<NotImplementedError> {
            barnService.opprettBarnPåBehandlingMedSøknadsdata(behandlingId,
                                                              UUID.randomUUID(),
                                                              emptyList(),
                                                              StønadType.SKOLEPENGER)
        }

    }

    @Test
    internal fun `skal ta med ett nytt barn ved revurdering hvor to barn eksisterer fra før`() {
        val grunnlagsdatabarn = listOf(barnMedIdent(fnrBarnD, "Barn D"),
                                       barnMedIdent(fnrBarnC, "Barn C"),
                                       barnMedIdent(fnrBarnB, "Barn B"),
                                       barnMedIdent(fnrBarnA, "Barn A"))
        val barnSlot = slot<List<BehandlingBarn>>()
        val forrigeBehandlingId = UUID.randomUUID()

        every { søknadMock.barn } returns setOf(barnPåSøknadA, barnPåSøknadB)
        every { barnRepository.findByBehandlingId(any()) } returns søknadsBarnTilBehandlingBarn(setOf(barnPåSøknadA,
                                                                                                      barnPåSøknadB),
                                                                                                forrigeBehandlingId)
        every { barnRepository.insertAll(capture(barnSlot)) } returns emptyList()

        val nyeBarnPåRevurdering = listOf(BehandlingBarn(behandlingId = behandlingId,
                                                         søknadBarnId = null,
                                                         personIdent = fnrBarnC,
                                                         navn = "Barn C")
        )
        barnService.opprettBarnForRevurdering(behandlingId,
                                              forrigeBehandlingId,
                                              nyeBarnPåRevurdering,
                                              grunnlagsdatabarn)

        assertThat(barnSlot.captured).hasSize(3)
        assertThat(barnSlot.captured.map { it.personIdent }).containsOnlyOnce(fnrBarnA, fnrBarnB, fnrBarnC)
        assertThat(barnSlot.captured.map { it.navn }).containsOnlyOnce("Barn A", "Barn B", "Barn C")

    }

    val fnrBarnA = "11111111111"
    val fnrBarnB = "22222222222"
    val fnrBarnC = "33333333333"
    val fnrBarnD = "44444444444"
    val barnPåSøknadA = SøknadBarn(id = UUID.randomUUID(),
                                   navn = "Barn A",
                                   fødselsnummer = fnrBarnA,
                                   harSkalHaSammeAdresse = false,
                                   ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
                                   erBarnetFødt = true,
                                   skalHaBarnepass = true,
                                   lagtTilManuelt = false
    )
    val barnPåSøknadB = SøknadBarn(id = UUID.randomUUID(),
                                   navn = "Barn B",
                                   fødselsnummer = fnrBarnB,
                                   harSkalHaSammeAdresse = false,
                                   ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
                                   erBarnetFødt = true,
                                   skalHaBarnepass = true,
                                   lagtTilManuelt = false
    )

    private fun barnMedIdent(fnr: String, navn: String): BarnMedIdent =
            BarnMedIdent(adressebeskyttelse = emptyList(),
                         bostedsadresse = emptyList(),
                         deltBosted = emptyList(),
                         dødsfall = emptyList(),
                         forelderBarnRelasjon = emptyList(),
                         fødsel = emptyList(),
                         navn = Navn(fornavn = navn.split(" ")[0],
                                     mellomnavn = null,
                                     etternavn = navn.split(" ")[1],
                                     metadata = Metadata(
                                             historisk = false)),
                         personIdent = fnr)

}