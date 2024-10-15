package no.nav.familie.ef.sak.behandling.migrering

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infotrygd.InfotrygdPerioderDto
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infotrygd.InfotrygdStønadPerioderDto
import no.nav.familie.ef.sak.infotrygd.tilSummertInfotrygdperiodeDto
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriode
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSak
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResponse
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakResultat
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.YearMonth

internal class InfotrygdPeriodeValideringServiceTest {
    private val infotrygdService = mockk<InfotrygdService>()
    private val behandlingService = mockk<BehandlingService>()
    private val featureToggleService = mockk<FeatureToggleService>()

    private val service = InfotrygdPeriodeValideringService(infotrygdService, behandlingService, featureToggleService)

    private val personIdent = "1"

    @BeforeEach
    internal fun setUp() {
        every { infotrygdService.eksisterer(any(), any()) } returns true
        every { infotrygdService.hentSaker(any()) } returns InfotrygdSakResponse(emptyList())
        every { featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_5_ÅR_TILBAKE) } returns false
        every { featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE) } returns false
    }

    @Nested
    inner class ValiderKanJournalføreUtenÅMigrere {
        @Test
        internal fun `skal kunne journalføre når personen ikke har noen saker i infotrygd`() {
            every { infotrygdService.hentDtoPerioder(personIdent) } returns infotrygdPerioderDto(emptyList())

            service.validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(personIdent, OVERGANGSSTØNAD)
        }

        @Test
        internal fun `skal kunne journalføre når det ikke trengs migrering - når personen har perioder langt bak i tiden`() {
            val dato = YearMonth.now().minusYears(6)
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                        ),
                    ),
                )
            service.validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(personIdent, OVERGANGSSTØNAD)
        }

        @Test
        internal fun `kan journalføre hvis det kun finnes perioder bak i tiden med 0-beløp`() {
            val dato = YearMonth.now().minusYears(1)
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                            beløp = 0,
                        ),
                    ),
                )
            service.validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(personIdent, OVERGANGSSTØNAD)
        }

        @Test
        internal fun `kan ikke journalføre når personen har periode`() {
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(listOf(lagInfotrygdPeriode()))

            assertThatThrownBy {
                service.validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(
                    personIdent,
                    OVERGANGSSTØNAD,
                )
            }.isInstanceOf(ApiFeil::class.java)
        }

        @Test
        internal fun `kan ikke journalføre når det finnes flere identer på perioder i infotrygd`() {
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(personIdent = "1", vedtakId = 1),
                        lagInfotrygdPeriode(personIdent = "2", vedtakId = 2),
                    ),
                )

            assertThatThrownBy {
                service.validerKanOppretteBehandlingUtenÅMigrereOvergangsstønad(
                    personIdent,
                    OVERGANGSSTØNAD,
                )
            }.isInstanceOf(ApiFeil::class.java)
        }
    }

    @Nested
    inner class ValiderHentPeriodeForMigrering {
        @Test
        internal fun `Skal kaste feil hvis perioder er mer enn fem år tilbake i tid`() {
            val dato = YearMonth.of(2018, 12)
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                        ),
                    ),
                )
            val message =
                assertThrows<MigreringException> {
                    service.hentPeriodeForMigrering(
                        personIdent,
                        OVERGANGSSTØNAD,
                    )
                }.message
            assertThat(message).contains("Kan ikke migrere når forrige utbetaling i infotrygd er før 2019-01-01, dato=2018-10-31")
        }

        @Test
        internal fun `Skal ikke kaste feil hvis perioder er etter 2016-01-01`() {
            val dato = YearMonth.of(2017, 1)
            every { featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE) } returns true
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                        ),
                    ),
                )

            val hentPeriodeForMigrering =
                service.hentPeriodeForMigrering(
                    personIdent,
                    OVERGANGSSTØNAD,
                )
            assertThat(hentPeriodeForMigrering).isNotNull
        }

        @Test
        internal fun `Skal kaste feil hvis perioder er før 2016-01-01`() {
            val dato = YearMonth.of(2015, 12)
            val grense = LocalDate.of(2016, 1, 1)

            every { featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE) } returns true
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                        ),
                    ),
                )
            val message =
                assertThrows<MigreringException> {
                    service.hentPeriodeForMigrering(
                        personIdent,
                        OVERGANGSSTØNAD,
                    )
                }.message
            assertThat(message).contains("Kan ikke migrere når forrige utbetaling i infotrygd er før $grense")
        }

        @Test
        internal fun `Skal ikke kaste feil hvis perioder er 4 år tilbake i tid og toggle tillater 5`() {
            val dato = YearMonth.now().minusYears(4)
            every { featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_5_ÅR_TILBAKE) } returns true
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                        ),
                    ),
                )

            assertThat(
                service.hentPeriodeForMigrering(
                    personIdent,
                    OVERGANGSSTØNAD,
                ),
            ).isNotNull
        }

        @Test
        internal fun `Skal ikke kaste feil hvis perioder er 6 år tilbake i tid og toggle tillater 7`() {
            val dato = YearMonth.now().minusYears(6)
            every { featureToggleService.isEnabled(Toggle.TILLAT_MIGRERING_7_ÅR_TILBAKE) } returns true
            every { infotrygdService.hentDtoPerioder(personIdent) } returns
                infotrygdPerioderDto(
                    listOf(
                        lagInfotrygdPeriode(
                            personIdent = "1",
                            stønadFom = dato.atDay(1),
                            stønadTom = dato.atEndOfMonth(),
                        ),
                    ),
                )

            assertThat(
                service.hentPeriodeForMigrering(
                    personIdent,
                    OVERGANGSSTØNAD,
                ),
            ).isNotNull
        }
    }

    @Nested
    inner class ValiderHarIkkeÅpenSakIInfotrygd {
        @Test
        internal fun `skal kunne migrere selv om personen har en klagesak`() {
            val fagsak = fagsak(stønadstype = BARNETILSYN)
            every { infotrygdService.hentSaker(any()) } returns
                InfotrygdSakResponse(
                    saker =
                        listOf(
                            InfotrygdSak(
                                personIdent = fagsak.hentAktivIdent(),
                                stønadType = fagsak.stønadstype,
                                resultat = InfotrygdSakResultat.ÅPEN_SAK,
                                type = InfotrygdSakType.KLAGE,
                            ),
                        ),
                )

            service.validerHarIkkeÅpenSakIInfotrygd(fagsak)
        }

        @Test
        internal fun `skal kunne migrere selv om personen har en klagesak for tilbakekreving`() {
            val fagsak = fagsak(stønadstype = BARNETILSYN)
            every { infotrygdService.hentSaker(any()) } returns
                InfotrygdSakResponse(
                    saker =
                        listOf(
                            InfotrygdSak(
                                personIdent = fagsak.hentAktivIdent(),
                                stønadType = fagsak.stønadstype,
                                resultat = InfotrygdSakResultat.ÅPEN_SAK,
                                type = InfotrygdSakType.KLAGE_TILBAKEBETALING,
                            ),
                        ),
                )

            service.validerHarIkkeÅpenSakIInfotrygd(fagsak)
        }
    }

    @Nested
    inner class ValiderHarÅpenBarnetilsynSakIInfotrygd {
        @Test
        fun `skal ikke kunne journalføre barnetilsyn når åpen sak i innfotrygd`() {
            val fagsak = fagsak(stønadstype = BARNETILSYN)
            every { infotrygdService.hentSaker(any()) } returns
                InfotrygdSakResponse(
                    saker =
                        listOf(
                            InfotrygdSak(
                                personIdent = fagsak.hentAktivIdent(),
                                stønadType = fagsak.stønadstype,
                                resultat = InfotrygdSakResultat.ÅPEN_SAK,
                            ),
                        ),
                )

            assertThatThrownBy { service.validerHarIkkeÅpenSakIInfotrygd(fagsak) }
                .isInstanceOf(MigreringException::class.java)
                .extracting("type")
                .isInstanceOf(MigreringExceptionType.ÅPEN_SAK::class.java)
        }

        @Test
        fun `skal kunne journalføre barnetilsyn med ferdigstilt sak i innfotrygd`() {
            val fagsak = fagsak(stønadstype = BARNETILSYN)
            every { infotrygdService.hentSaker(any()) } returns
                InfotrygdSakResponse(
                    saker =
                        listOf(
                            InfotrygdSak(
                                personIdent = fagsak.hentAktivIdent(),
                                stønadType = fagsak.stønadstype,
                                resultat = InfotrygdSakResultat.INNVILGET,
                            ),
                        ),
                )

            service.validerHarIkkeÅpenSakIInfotrygd(fagsak)
        }
    }

    private fun infotrygdPerioderDto(perioder: List<InfotrygdPeriode> = emptyList()): InfotrygdPerioderDto {
        val summertePerioder = perioder.map { it.tilSummertInfotrygdperiodeDto() }
        val emptyStønadPeriodeDto = InfotrygdStønadPerioderDto(perioder, summertePerioder)
        return InfotrygdPerioderDto(emptyStønadPeriodeDto, emptyStønadPeriodeDto, emptyStønadPeriodeDto)
    }
}
