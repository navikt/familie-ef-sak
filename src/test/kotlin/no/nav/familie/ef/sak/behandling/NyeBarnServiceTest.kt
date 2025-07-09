package no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonForelderBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarn
import no.nav.familie.kontrakter.ef.personhendelse.NyttBarnÅrsak
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class NyeBarnServiceTest {
    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val personService = mockk<PersonService>()
    val barnService = mockk<BarnService>()
    val pdlSøker = mockk<PdlSøker>(relaxed = true)
    val taskService = mockk<TaskService>(relaxed = true)
    val nyeBarnService = NyeBarnService(behandlingService, fagsakService, personService, barnService, taskService)

    val grunnlagsdataMedMetadata = mockk<GrunnlagsdataMedMetadata>()
    val fagsak = fagsak()
    val behandling = behandling(fagsak)
    val grunnlagsdataDomene = mockk<GrunnlagsdataDomene>()

    val fnrForEksisterendeBarn = "19011870794"
    val fødselsdatoEksisterendeBarn: LocalDate = LocalDate.of(2018, 1, 19)
    val fnrForNyttBarn = "15012279679"
    val fødselsdatoNyttBarn: LocalDate = LocalDate.of(2022, 1, 15)
    val fnrForVoksentBarn = "22100188701"
    val fødselsdatoVoksentBarn: LocalDate = LocalDate.of(2001, 10, 22)

    @BeforeEach
    fun init() {
        every { behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(any()) } returns behandling
        every { fagsakService.finnFagsaker(any()) } returns listOf(fagsak)
        every { grunnlagsdataMedMetadata.grunnlagsdata } returns grunnlagsdataDomene
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent("fnr til søker", false)))
        every { fagsakService.hentAktivIdent(any()) } returns "fnr til søker"
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med et nytt barn i PDL siden behandling, forvent ett nytt barn`() {
        val pdlBarn =
            mapOf(
                fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                fnrForNyttBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoNyttBarn)),
            )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(1)
        assertThat(barn.first()).isEqualTo(NyttBarn(fnrForNyttBarn, StønadType.OVERGANGSSTØNAD, NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING))
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med ett født terminbarn i PDL, forvent ingen treff`() {
        val terminDato = YearMonth.now().atEndOfMonth()
        val fødselsdato = YearMonth.now().atDay(15)
        val fnrForPdlBarn = FnrGenerator.generer(fødselsdato)
        val pdlBarn =
            mapOf(
                fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                fnrForPdlBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
            )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns
            listOf(
                behandlingBarn(fnrForEksisterendeBarn),
                behandlingBarn(fødselTermindato = terminDato),
            )

        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(0)
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med tvillinger i PDL av terminbarn med alle i behandlingen, forvent ingen nye barn`() {
        val terminDato = YearMonth.now().atEndOfMonth()
        val fødselsdato = YearMonth.now().atDay(15)
        val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
        val fnrForTvillingbarn = FnrGenerator.generer(fødselsdato)
        val pdlBarn =
            mapOf(
                fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                fnrForTvillingbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
            )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns
            listOf(
                behandlingBarn(fnrForEksisterendeBarn),
                behandlingBarn(fødselTermindato = terminDato),
                behandlingBarn(fødselTermindato = terminDato),
            )

        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(0)
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med tvillinger i PDL av terminbarn, men bare ett i behandlingen, forvent ett nytt barn`() {
        val terminDato = YearMonth.now().atEndOfMonth()
        val fødselsdato = YearMonth.now().atDay(15)
        println({ "Termin dato: $terminDato - Fødselsdato: $fødselsdato" })
        val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
        val fnrForTvillingbarn = FnrGenerator.generer(fødselsdato)
        val pdlBarn =
            mapOf(
                fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                fnrForTvillingbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
            )
        println("PDL barn: $pdlBarn")
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns
            listOf(
                behandlingBarn(fnrForEksisterendeBarn),
                behandlingBarn(fødselTermindato = terminDato),
            )
        println({ "Søker med barn ${søkerMedBarn(pdlBarn)}" })
        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(1)
        assertThat(barn.single().årsak).isEqualTo(NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING)
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med ett og samme barn i PDL siden behandling, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)))
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(0)
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med ett ekstra voksent barn i PDL skal returnere det voksne barnet som nytt barn`() {
        val pdlBarn =
            mapOf(
                fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                fnrForVoksentBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoVoksentBarn)),
            )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(1)
        assertThat(barn[0].personIdent).isEqualTo(fnrForVoksentBarn)
    }

    @Test
    fun `finnNyeEllerUtenforTerminFødteBarn med ett ekstra terminbarn i PDL, forvent ingen treff`() {
        val pdlBarn =
            mapOf(
                fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                fnrForNyttBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoNyttBarn)),
            )
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns
            listOf(
                behandlingBarn(fnrForEksisterendeBarn),
                behandlingBarn(fødselTermindato = fødselsdatoNyttBarn),
            )

        val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
        assertThat(barn).hasSize(0)
    }

    @Nested
    inner class FinnNyeEllerUtenforTerminFødteBarn {
        @Test
        internal fun `finnNyeEllerUtenforTerminFødteBarn - skal finne barn som blir født i en tidligere måned`() {
            val terminDato = LocalDate.of(2021, 2, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato),
                )

            val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).hasSize(1)
            assertThat(barn.map { it.årsak }).containsExactly(NyttBarnÅrsak.FØDT_FØR_TERMIN)
        }

        @Test
        internal fun `finnNyeEllerUtenforTerminFødteBarn - skal finne barn som blir født for sent inn i neste måned`() {
            val terminDato = LocalDate.of(2021, 1, 30)
            val fødselsdato = terminDato.plusWeeks(3)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato),
                )

            val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).hasSize(1)
            assertThat(barn.map { it.årsak }).containsExactly(NyttBarnÅrsak.FØDT_ETTER_TERMIN)
        }

        @Test
        internal fun `finnNyeEllerUtenforTerminFødteBarn - skal ikke finne barn som blitt født for sent innen samme måned`() {
            val terminDato = LocalDate.of(2021, 1, 1)
            val fødselsdato = terminDato.plusWeeks(3)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato),
                )

            val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).isEmpty()
        }

        @Test
        internal fun `finnNyeEllerUtenforTerminFødteBarn - født i samme måned skal ikke returnere noe`() {
            val terminDato = LocalDate.of(2021, 3, 31)
            val fødselsdato = LocalDate.of(2021, 3, 1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato),
                )

            val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).isEmpty()
        }

        @Test
        internal fun `finnNyeEllerUtenforTerminFødteBarn - født før termin, men har allerede personident i behandlingbarn - dvs allerede behandlet`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                    behandlingBarn(fødselTermindato = terminDato, fnr = fnrForTerminbarn),
                )

            val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).isEmpty()
        }

        @Test
        internal fun `finnNyeEllerUtenforTerminFødteBarn - ukjent barn`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns
                listOf(
                    behandlingBarn(fnrForEksisterendeBarn),
                )

            val barn = nyeBarnService.finnNyeEllerUtenforTerminFødteBarn(PersonIdent("fnr til søker")).nyeBarn
            assertThat(barn).hasSize(1)
            assertThat(barn.map { it.årsak }).containsExactly(NyttBarnÅrsak.BARN_FINNES_IKKE_PÅ_BEHANDLING)
        }
    }

    @Nested
    inner class FinnNyeBarnSidenGjeldendeBehandlingForFagsak {
        @Test
        internal fun `har ikke barn fra før, og har ikke noen nye barn`() {
            val pdlPersonForelderBarn = emptyMap<String, PdlPersonForelderBarn>()
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlPersonForelderBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).isEmpty()
            assertThat(dto.harBarnISisteIverksatteBehandling).isFalse
        }

        @Test
        internal fun `har ikke barn fra før, men har nytt barn`() {
            val pdlBarn =
                mapOf(
                    fnrForNyttBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoNyttBarn)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).isNotEmpty
            assertThat(dto.nyeBarn.first().personIdent).isEqualTo(fnrForNyttBarn)
            assertThat(dto.harBarnISisteIverksatteBehandling).isFalse
        }

        @Test
        internal fun `har barn fra før, og ikke noen nye barn`() {
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).isEmpty()
            assertThat(dto.harBarnISisteIverksatteBehandling).isTrue
        }

        @Test
        internal fun `har barn fra før, og nye barn`() {
            val pdlBarn =
                mapOf(
                    fnrForEksisterendeBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                    fnrForNyttBarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdatoNyttBarn)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).hasSize(1)
            assertThat(dto.nyeBarn.first().personIdent).isEqualTo(fnrForNyttBarn)
            assertThat(dto.harBarnISisteIverksatteBehandling).isTrue
        }

        @Test
        internal fun `har terminbarn fra før, som ikke er født`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(emptyMap())
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fødselTermindato = terminDato))
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).isEmpty()
            assertThat(dto.harBarnISisteIverksatteBehandling).isTrue
        }

        @Test
        internal fun `har terminbarn fra før, som nå er født`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            val fnrForTerminbarn = FnrGenerator.generer(terminDato)
            val pdlBarn =
                mapOf(
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = terminDato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fødselTermindato = terminDato))
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).isEmpty()
            assertThat(dto.harBarnISisteIverksatteBehandling).isTrue
        }

        @Test
        internal fun `har terminbarn fra før, som er født tidligere enn termindato`() {
            val terminDato = LocalDate.of(2021, 3, 1)
            val fødselsdato = terminDato.minusMonths(1)
            val fnrForTerminbarn = FnrGenerator.generer(fødselsdato)
            val pdlBarn =
                mapOf(
                    fnrForTerminbarn to pdlBarn(fødsel = fødsel(fødselsdato = fødselsdato)),
                )
            every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
            every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fødselTermindato = terminDato))
            val dto = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForFagsak(UUID.randomUUID())

            assertThat(dto.nyeBarn).isEmpty()
            assertThat(dto.harBarnISisteIverksatteBehandling).isTrue
        }
    }

    private fun behandlingBarn(
        fnr: String? = null,
        fødselTermindato: LocalDate? = null,
        søknadBarnId: UUID? = null,
    ) = BehandlingBarn(
        behandlingId = UUID.randomUUID(),
        søknadBarnId = søknadBarnId,
        personIdent = fnr,
        fødselTermindato = fødselTermindato,
        navn = null,
    )

    private fun søkerMedBarn(pdlPersonForelderBarn: Map<String, PdlPersonForelderBarn>): SøkerMedBarn = SøkerMedBarn("søker", pdlSøker, pdlPersonForelderBarn)
}
