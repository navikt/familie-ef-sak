package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

internal class AndelHistorikkDtoKtTest {

    @Test
    internal fun `andel er aktiv hvis perioden ikke har endring eller er splittet`() {
        assertThat(andel.erAktivVedtaksperiode()).isTrue
        assertThat(andel.medEndring(EndringType.SPLITTET).erAktivVedtaksperiode()).isTrue
    }

    @Test
    internal fun `andel er ikke aktiv hvis den er fjernet eller erstattet`() {
        assertThat(andel.medEndring(EndringType.FJERNET).erAktivVedtaksperiode()).isFalse
        assertThat(andel.medEndring(EndringType.ERSTATTET).erAktivVedtaksperiode()).isFalse
    }

    private fun AndelHistorikkDto.medEndring(endringType: EndringType) =
        this.copy(endring = HistorikkEndring(endringType, UUID.randomUUID(), LocalDateTime.now()))

    val andel = AndelHistorikkDto(
        behandlingId = UUID.randomUUID(),
        behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
        behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
        vedtakstidspunkt = LocalDateTime.now(),
        saksbehandler = "",
        andel = andelMedGrunnlagDto(),
        aktivitet = null,
        aktivitetArbeid = null,
        periodeType = null,
        erSanksjon = false,
        sanksjonsårsak = null,
        endring = null
    )

    private fun andelMedGrunnlagDto() = AndelMedGrunnlagDto(
        beløp = 0,
        periode = Månedsperiode(YearMonth.now()),
        inntekt = 0,
        inntektsreduksjon = 0,
        samordningsfradrag = 0,
        kontantstøtte = 0,
        tilleggsstønad = 0,
        antallBarn = 0,
        barn = emptyList(),
        sats = 0,
        beløpFørFratrekkOgSatsJustering = 0
    )
}