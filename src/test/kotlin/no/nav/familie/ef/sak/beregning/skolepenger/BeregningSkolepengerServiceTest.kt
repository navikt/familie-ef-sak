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
import no.nav.familie.ef.sak.vedtak.domain.Utgiftstype
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.DelårsperiodeSkoleårDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.SkolepengerUtgiftDto
import no.nav.familie.ef.sak.vedtak.dto.SkoleårsperiodeSkolepengerDto
import no.nav.familie.ef.sak.vedtak.dto.tilDomene
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
        assertThat(beregnedePerioder.perioder).containsOnly(BeløpsperiodeSkolepenger(defaultFra, 100, 50))
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
                .hasMessageContaining("Alle perioder i et skoleår må være i det samme skoleåret")
        }

        @Test
        internal fun `ulike skoleår i ulike delårperioder er ikke gyldig`() {
            val delårsperiode1 = delårsperiode()
            val delårsperiode2 = delårsperiode(
                fra = defaultFra.plusYears(1),
                til = defaultTil.plusYears(1)
            )
            val skoleårsperioder =
                listOf(SkoleårsperiodeSkolepengerDto(listOf(delårsperiode1, delårsperiode2), listOf(utgift())))
            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Alle perioder i et skoleår må være i det samme skoleåret")
        }

        @Test
        internal fun `samme skoleår i flere skoleårsperioder er ikke gyldig`() {
            val delårsperiode = delårsperiode()
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift()))
            val periode2 = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode), listOf(utgift()))
            val skoleårsperioder = listOf(periode, periode2)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Skoleåret 21/22 er definiert flere ganger")
        }

        @Test
        internal fun `overlappende skoleår er ikke gyldig`() {
            val skoleårsperioder = listOf(
                SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(), delårsperiode()), listOf(utgift())),
            )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Skoleår 21/22 inneholder overlappende perioder")
        }

        @Test
        internal fun `utgifter med samme ider er ikke gyldig`() {
            val utgift = utgift()
            val skoleårsperioder = listOf(
                SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift, utgift)),
            )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java) // Dette er ikke brukerfeil
                .hasMessageContaining("Det finnes duplikat av ider på utgifter")
        }

        @Test
        internal fun `må inneholde en utgiftstype`() {
            val utgift = utgift(utgiftstyper = emptySet())
            val skoleårsperioder = listOf(
                SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift)),
            )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java) // Dette burde vært håndtert i frontend
                .hasMessageContaining("Skoleåret 2021 mangler utgiftstyper for en eller flere utgifter")
        }

        @Test
        internal fun `skoleår inneholder ulike studietyper`() {
            val delårsperiode1 = delårsperiode(til = defaultFra)
            val delårsperiode2 = delårsperiode(fra = defaultFra.plusMonths(1), studietype = SkolepengerStudietype.VIDEREGÅENDE)
            val skoleårsperioder = listOf(
                SkoleårsperiodeSkolepengerDto(listOf(delårsperiode1, delårsperiode2), listOf(utgift())),
            )

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(Feil::class.java) // Dette burde vært håndtert i frontend
                .hasMessageContaining("Skoleår 2021 inneholder ulike studietyper")
        }
    }

    @Nested
    inner class ValideringAvBeløp {
        @Test
        internal fun `utgifter er mindre enn 1`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift(utgifter = 0)))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Utgifter må være høyere enn 0kr")
        }

        @Test
        internal fun `stønad kan ikke være under 0kr`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift(stønad = -1)))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Stønad kan ikke være lavere enn 0kr")
        }

        @Test
        internal fun `stønad kan ikke være høyere enn utgifter`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode()), listOf(utgift(stønad = 200)))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Stønad kan ikke være høyere enn utgifter")
        }

        @Test
        internal fun `studiebelastning kan ikke være under 1%`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(studiebelastning = 0)), listOf(utgift()))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Studiebelastning må være over 0")
        }

        @Test
        internal fun `studiebelastning må være under 101%`() {
            val periode = SkoleårsperiodeSkolepengerDto(listOf(delårsperiode(studiebelastning = 101)), listOf(utgift()))
            val skoleårsperioder = listOf(periode)

            assertThatThrownBy { service.beregnYtelse(skoleårsperioder, førstegangsbehandling.id) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("Studiebelastning må være under eller lik 100")
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
            assertThat(beregnedePerioder.perioder).containsOnly(BeløpsperiodeSkolepenger(defaultFra, 100, 50))
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
                    "fakturadato=2021-08 utgifter=100 stønad=50"
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

    private fun vedtak(
        skoleårsperioder: List<SkoleårsperiodeSkolepengerDto>,
        behandlingId: UUID = UUID.randomUUID(),
        resultatType: ResultatType = ResultatType.INNVILGE
    ) =
        Vedtak(
            behandlingId = behandlingId,
            resultatType = resultatType,
            periodeBegrunnelse = "OK",
            inntektBegrunnelse = "OK",
            avslåBegrunnelse = null,
            skolepenger = SkolepengerWrapper(skoleårsperioder.map { it.tilDomene() }, begrunnelse = null)
        )

    private fun utgift(
        id: UUID = UUID.randomUUID(),
        utgiftstyper: Set<Utgiftstype> = setOf(Utgiftstype.SEMESTERAVGIFT),
        fra: YearMonth = defaultFra,
        utgifter: Int = 100,
        stønad: Int = 50
    ) = SkolepengerUtgiftDto(
        id = id,
        utgiftstyper = utgiftstyper,
        årMånedFra = fra,
        utgifter = utgifter,
        stønad = stønad
    )

    private fun delårsperiode(
        studietype: SkolepengerStudietype = SkolepengerStudietype.HØGSKOLE_UNIVERSITET,
        fra: YearMonth = defaultFra,
        til: YearMonth = defaultTil,
        studiebelastning: Int = 100
    ) = DelårsperiodeSkoleårDto(
        studietype = studietype,
        årMånedFra = fra,
        årMånedTil = til,
        studiebelastning = studiebelastning
    )
}
