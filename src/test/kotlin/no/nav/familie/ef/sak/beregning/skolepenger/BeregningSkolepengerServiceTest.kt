package no.nav.familie.ef.sak.beregning.skolepenger

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerStudietype
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.DelårsperiodeSkoleårDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.tilDomene
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth
import java.util.UUID

internal class BeregningSkolepengerServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vedtakService = mockk<VedtakService>()
    private val service = BeregningSkolepengerService(behandlingService, vedtakService)

    private val fagsak = fagsak()
    private val førstegangsbehandling = behandling()
    private val revurdering = behandling(forrigeBehandlingId = førstegangsbehandling.id)

    private val defaultFra = YearMonth.of(2021, 8)
    private val defaultTil = YearMonth.of(2022, 6)
    private val defaultStønad = 50

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentSaksbehandling(førstegangsbehandling.id) } returns
            saksbehandling(fagsak = fagsak, behandling = førstegangsbehandling)
        every { behandlingService.hentSaksbehandling(revurdering.id) } returns
            saksbehandling(fagsak = fagsak, behandling = revurdering)
    }

    @Test
    internal fun `førstegangsvedtak med gyldige perioder`() {
        val utgift = utgift()
        val delårsperiode = delårsperiode()
        val skoleårsperiode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift))
        val beregnedePerioder = service.beregnYtelse(listOf(skoleårsperiode), førstegangsbehandling.id)
        assertThat(beregnedePerioder.perioder).containsOnly(BeløpsperiodeSkolepenger(defaultFra, 50))
    }

    @Nested
    inner class TommeListerKasterFeil {
        @Test
        internal fun `mangler skoleår`() {
            val skoleårsperioder = emptyList<SkoleårsperiodeSkolepengerDto>()

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("Mangler skoleår")
        }

        @Test
        internal fun `mangler skoleårdelårsperioder`() {
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(), listOf(utgift())))

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("Mangler skoleårsperioder")
        }

        @Test
        internal fun `mangler utgiftsperioder`() {
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf()))

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("Mangler utgiftsperioder")
        }
    }

    @Nested
    inner class Skoleårsvalidering {
        @Test
        internal fun `ulike skoleår i fra og til er ikke gyldig`() {
            val delårsperiode = delårsperiode(til = YearMonth.of(2022, 9))
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift())))

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Ugyldig skoleårsperiode: Når tildato er i neste år, så må måneden være før september")
        }

        @Test
        internal fun `ulike skoleår i ulike delårperioder er ikke gyldig`() {
            val delårsperiode1 = delårsperiode()
            val delårsperiode2 =
                delårsperiode(
                    fra = defaultFra.plusYears(1),
                    til = defaultTil.plusYears(1),
                )
            val skoleårsperioder =
                listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode1, delårsperiode2), listOf(utgift())))
            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Periode 08.2022-06.2023 er definert utenfor skoleåret 21/22")
        }

        @Test
        internal fun `samme skoleår i flere skoleårsperioder er ikke gyldig`() {
            val delårsperiode = delårsperiode()
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift()))
            val periode2 = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift()))
            val skoleårsperioder = listOf(periode, periode2)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Skoleåret 21/22 kan ikke legges inn flere ganger")
        }

        @Test
        internal fun `overlappende skoleår er ikke gyldig`() {
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(), delårsperiode()), listOf(utgift())),
                )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Skoleår 21/22 inneholder overlappende perioder")
        }

        @Test
        internal fun `utgifter med samme ider er ikke gyldig`() {
            val utgift = utgift()
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift, utgift)),
                )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java) // Dette er ikke brukerfeil
                .hasMessageContaining("Det finnes duplikat av ider på utgifter")
        }

        @Test
        internal fun `skoleår inneholder ulike studietyper`() {
            val delårsperiode1 = delårsperiode(til = defaultFra)
            val delårsperiode2 =
                delårsperiode(fra = defaultFra.plusMonths(1), studietype = SkolepengerStudietype.VIDEREGÅENDE)
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode1, delårsperiode2), listOf(utgift())),
                )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java) // Dette burde vært håndtert i frontend
                .hasMessageContaining("Skoleår 21/22 inneholder ulike studietyper")
        }

        @Test
        internal fun `skal kunne flytte på ordningen av skoleårsperioder`() {
            val utgift = utgift()
            val utgift2 = utgift()
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift)),
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(fra = defaultFra.plusYears(1), til = defaultTil.plusYears(1))),
                        listOf(utgift2),
                    ),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(fra = defaultFra.plusYears(1), til = defaultTil.plusYears(1))),
                        listOf(utgift2),
                    ),
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift)),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            val perioder = service.beregnYtelse(skoleårsperioder, revurdering.id)
            assertThat(perioder.perioder).hasSize(1)
            assertThat(perioder.perioder[0].beløp).isEqualTo(utgift.stønad + utgift2.stønad)
            assertThat(perioder.perioder[0].årMånedFra).isEqualTo(utgift.årMånedFra)
        }
    }

    @Nested
    inner class ValideringAvBeløp {
        @Test
        internal fun `stønad kan ikke være under 0kr`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift(stønad = -1)))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Stønad kan ikke være lavere enn 0kr")
        }

        @Test
        internal fun `studiebelastning må være 50-100 prosent`() {
            listOf(50, 51, 99, 100).forEach {
                val perioder = listOf(delårsperiode(studiebelastning = it))
                service.beregnYtelse(
                    listOf(SkoleårsperiodeSkolepengerDto(perioder, listOf(utgift()))),
                    førstegangsbehandling.id,
                )
            }
        }

        @Test
        internal fun `studiebelastning kan ikke være under 50 prosent`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(studiebelastning = 49)), listOf(utgift()))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Studiebelastning må være mellom 50-100%")
        }

        @Test
        internal fun `studiebelastning må være under 101 prosent`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(studiebelastning = 101)), listOf(utgift()))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Studiebelastning må være mellom 50-100%")
        }
    }

    @Nested
    inner class Revurdering {
        @Test
        internal fun `revurdering uten endringer`() {
            val utgift = utgift()
            val delårsperiode = delårsperiode()
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift)))

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(skoleårsperioder)

            val beregnedePerioder = service.beregnYtelse(skoleårsperioder, revurdering.id)
            assertThat(beregnedePerioder.perioder).containsOnly(BeløpsperiodeSkolepenger(defaultFra, 50))
        }

        @Test
        internal fun `revurdering med utgiftsperiode med annen id feiler`() {
            val delårsperiode = delårsperiode()
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift())))
            val revurderingsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift())))

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(skoleårsperioder)

            assertThatThrownBy { service.beregnYtelse(revurderingsperioder, revurdering.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContainingAll(
                    "Mangler utgiftsperioder fra forrige vedtak",
                    "fakturadato=2021-08 stønad=50",
                )
        }

        @Test
        internal fun `revurdering med endret beløp for utgiftsperiode er ikke tillatt`() {
            val delårsperiode = delårsperiode()
            val utgift = utgift()
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift)))
            val revurderingsperioder =
                listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift.copy(stønad = 100))))

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(skoleårsperioder)

            assertThatThrownBy { service.beregnYtelse(revurderingsperioder, revurdering.id) }
                .isInstanceOf(Feil::class.java) // denne skal ikke være brukerfeil (ApiFeil)
                .hasMessageContaining(
                    "Utgiftsperiode er endret for skoleår=21/22 id=${utgift.id} er endret",
                )
        }
    }

    @Nested
    inner class OpphørValiderFinnesEndringer {
        @Test
        internal fun `ingen endringer kaster feil`() {
            val utgift = utgift()
            val tidligerePerioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift)))
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift.copy())))

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Periodene er uendrede")
        }

        @Test
        internal fun `skal oppdage hvis alle skoleårsperioder er fjernet`() {
            val tidligerePerioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift())))
            val skoleårsperioder = emptyList<SkoleårsperiodeSkolepengerDto>()

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            val perioder = service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true)
            assertThat(perioder.perioder).hasSize(0)
        }

        @Test
        internal fun `skal oppdage hvis en skoleårsperioder er fjernet`() {
            val utgift = utgift()
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift)),
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(fra = defaultFra.plusYears(1), til = defaultTil.plusYears(1))),
                        listOf(utgift()),
                    ),
                )
            val skoleårsperioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift)))

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            val perioder = service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true)
            assertThat(perioder.perioder)
                .containsOnly(BeløpsperiodeSkolepenger(defaultFra, defaultStønad))
        }

        @Test
        internal fun `skal oppdage hvis alle delårsperiode er fjernet`() {
            val utgift = utgift()
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(til = defaultFra), delårsperiode(fra = defaultTil)),
                        listOf(utgift),
                    ),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(til = defaultFra)), listOf(utgift.copy())),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            val perioder = service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true)
            assertThat(perioder.perioder)
                .containsOnly(BeløpsperiodeSkolepenger(defaultFra, defaultStønad))
        }

        @Test
        internal fun `skal oppdage hvis en utgift er fjernet`() {
            val utgift1 = utgift()
            val utgift2 = utgift()
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift1, utgift2)),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift1)),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            val perioder = service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true)
            assertThat(perioder.perioder)
                .containsOnly(BeløpsperiodeSkolepenger(defaultFra, defaultStønad))
        }
    }

    @Nested
    inner class OpphørValiderIngenNyePerioderFinnes {
        @Test
        internal fun `skal oppdage en ny skoleårsperioder`() {
            val tidligerePerioder = listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift())))
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift())),
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(fra = defaultFra.plusYears(1), til = defaultTil.plusYears(1))),
                        listOf(utgift()),
                    ),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("Det finnes nye skoleårsperioder")
        }

        @Test
        internal fun `skal oppdage hvis en delårsperiode er lagt til`() {
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(til = defaultFra)),
                        listOf(utgift()),
                    ),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode(til = defaultFra), delårsperiode(fra = defaultTil)),
                        listOf(utgift()),
                    ),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("En ny periode for skoleår=21/22 er lagt til")
        }

        @Test
        internal fun `skal tillate endringer på delårsperioder`() {
            val utgift = utgift()
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode()),
                        listOf(utgift),
                    ),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(til = defaultFra)), listOf(utgift)),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            val perioder = service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true)
            assertThat(perioder.perioder)
                .containsOnly(BeløpsperiodeSkolepenger(defaultFra, defaultStønad))
        }

        @Test
        internal fun `skal oppdage hvis en utgift er lagt til`() {
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode()),
                        listOf(utgift()),
                    ),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode()),
                        listOf(utgift(), utgift()),
                    ),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("En ny utgiftsperiode for skoleår=21/22 er lagt til")
        }

        @Test
        internal fun `skal oppdage hvis en utgiftsperiode er endret`() {
            val tidligerePerioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(
                        listOf(delårsperiode()),
                        listOf(utgift()),
                    ),
                )
            val skoleårsperioder =
                listOf(
                    SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift(stønad = 1))),
                )

            every { vedtakService.hentVedtak(førstegangsbehandling.id) } returns vedtak(tidligerePerioder)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, revurdering.id, erOpphør = true) }
                .isInstanceOf(Feil::class.java)
                .hasMessageContaining("Utgiftsperioder for 21/22 er endrede")
        }
    }

    private fun vedtak(
        skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
        behandlingId: UUID = UUID.randomUUID(),
        resultatType: ResultatType = ResultatType.INNVILGE,
    ) = Vedtak(
        behandlingId = behandlingId,
        resultatType = resultatType,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        skolepenger = SkolepengerWrapper(skoleårsperioder.map { it.tilDomene() }, begrunnelse = null),
    )

    private fun utgift(
        id: UUID = UUID.randomUUID(),
        fra: YearMonth = defaultFra,
        stønad: Int = defaultStønad,
    ) = SkolepengerUtgiftDto(
        id = id,
        årMånedFra = fra,
        stønad = stønad,
    )

    private fun delårsperiode(
        studietype: SkolepengerStudietype = SkolepengerStudietype.HØGSKOLE_UNIVERSITET,
        fra: YearMonth = defaultFra,
        til: YearMonth = defaultTil,
        studiebelastning: Int = 100,
    ) = DelårsperiodeSkoleårDto(
        studietype = studietype,
        årMånedFra = fra,
        årMånedTil = til,
        periode = Månedsperiode(fra, til),
        studiebelastning = studiebelastning,
    )
}
