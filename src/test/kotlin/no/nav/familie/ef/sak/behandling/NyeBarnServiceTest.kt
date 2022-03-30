package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.NyeBarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarnÅrsak
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.prosessering.domene.TaskRepository
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class NyeBarnServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val personService = mockk<PersonService>()
    val barnService = mockk<BarnService>()
    val pdlSøker = mockk<PdlSøker>(relaxed = true)
    val taskRepository = mockk<TaskRepository>(relaxed = true)
    val nyeBarnService = NyeBarnService(behandlingService, fagsakService, personService, barnService, taskRepository)

    val grunnlagsdataMedMetadata = mockk<GrunnlagsdataMedMetadata>()
    val fagsak = fagsak()
    val behandling = mockk<Behandling>()
    val grunnlagsdataDomene = mockk<GrunnlagsdataDomene>()

    val fnrForEksisterendeBarn = "19011870794"
    val fødselsdatoEksisterendeBarn: LocalDate = LocalDate.of(2018, 1, 19)
    val fnrForNyttBarn = "15012279679"
    val fødselsdatoNyttBarn: LocalDate = LocalDate.of(2022, 1, 15)
    val fnrForVoksentBarn = "22100188701"
    val fødselsdatoVoksentBarn: LocalDate = LocalDate.of(2001, 10, 22)

    @BeforeEach fun init() {
        every { behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(any()) } returns behandling
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { behandling.id } returns UUID.randomUUID()
        every { grunnlagsdataMedMetadata.grunnlagsdata } returns grunnlagsdataDomene
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent("fnr til søker", false)))
        every { fagsakService.hentAktivIdent(any()) } returns "fnr til søker"
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med et nytt barn i PDL siden behandling, forvent ett nytt barn`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForNyttBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoNyttBarn))
        )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(1)
        assertThat(barn.first()).isEqualTo(fnrForNyttBarn)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med ett født terminbarn i PDL, forvent ingen treff`() {
        val terminDato = LocalDate.now()
        val fødselsdato = LocalDate.now().minusWeeks(5)
        val fnrForPdlBarn = FnrGenerator.generer(fødselsdato)
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForPdlBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato))
        )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                behandlingBarn(fnrForEksisterendeBarn), behandlingBarn(fødselTermindato = terminDato)
        )

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med tvillinger i PDL av terminbarn med alle i behandlingen, forvent ingen nye barn`() {
        val terminDato = LocalDate.now()
        val fødselsdato = LocalDate.now().minusWeeks(5)
        val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
        val fnrForTvillingbarn = FnrGenerator.generer(fødselsdato)
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                            fnrForTvillingbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato))
        )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                behandlingBarn(fnrForEksisterendeBarn),
                behandlingBarn(fødselTermindato = terminDato),
                behandlingBarn(fødselTermindato = terminDato)
        )

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med tvillinger i PDL av terminbarn, men bare ett i behandlingen, forvent ett nytt barn`() {
        val terminDato = LocalDate.now()
        val fødselsdato = LocalDate.now().minusWeeks(5)
        val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
        val fnrForTvillingbarn = FnrGenerator.generer(fødselsdato)
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                            fnrForTvillingbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato))
        )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                behandlingBarn(fnrForEksisterendeBarn), behandlingBarn(fødselTermindato = terminDato)
        )

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(1)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med ett og samme barn i PDL siden behandling, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)))
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med ett ekstra voksent barn i PDL, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForVoksentBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoVoksentBarn))
        )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandlingForPersonIdent med ett ekstra terminbarn i PDL, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForNyttBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoNyttBarn))
        )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                behandlingBarn(fnrForEksisterendeBarn), behandlingBarn(fødselTermindato = fødselsdatoNyttBarn)
        )

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Nested
    inner class FinnNyeEllerTidligereFødteBarn {

        @Test
        internal fun `finnNyeEllerTidligereFødteBarn - skal finne barn som blitt født i en tidligere måned`() {
            val terminDato = LocalDate.of(2021, 2, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                                fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)))
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato),
            )

            val barn = nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).hasSize(1)
            assertThat(barn.map { it.årsak }).containsExactly(NyttBarnÅrsak.FØDT_FØR_TERMIN)
        }

        @Test
        internal fun `finnNyeEllerTidligereFødteBarn - født i samme måned skal ikke returnere noe`() {
            val terminDato = LocalDate.of(2021, 3, 31)
            val fødselsdato = LocalDate.of(2021, 3, 1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                                fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)))
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato),
            )

            val barn = nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).isEmpty()
        }

        @Test
        internal fun `finnNyeEllerTidligereFødteBarn - født før termin, men har allerede personident i behandlingbarn - dvs allerede behandlet`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                                fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)))
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato, fnr = fnrForTerminbarn),
            )

            val barn = nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).isEmpty()
        }

        @Test
        internal fun `finnNyeEllerTidligereFødteBarn - ukjent barn`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                                fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)))
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(
                    behandlingBarn(fnrForEksisterendeBarn)
            )

            val barn = nyeBarnService.finnNyeEllerTidligereFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).hasSize(1)
            assertThat(barn.map { it.årsak }).containsExactly(NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING)
        }
    }

    private fun behandlingBarn(fnr: String? = null,
                               fødselTermindato: LocalDate? = null,
                               søknadBarnId: UUID? = null) = BehandlingBarn(
            behandlingId = UUID.randomUUID(),
            søknadBarnId = søknadBarnId,
            personIdent = fnr,
            fødselTermindato = fødselTermindato,
            navn = null
    )

    private fun søkerMedBarn(pdlBarn: Map<String, PdlBarn>): SøkerMedBarn = SøkerMedBarn("søker", pdlSøker, pdlBarn)

}