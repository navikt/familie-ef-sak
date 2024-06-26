package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun lagreVedtak(
        vedtakDto: VedtakDto,
        behandlingId: UUID,
        stønadstype: StønadType,
    ): UUID = vedtakRepository.insert(vedtakDto.tilVedtak(behandlingId, stønadstype)).behandlingId

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.deleteById(behandlingId)
    }

    fun hentVedtak(behandlingId: UUID): Vedtak = vedtakRepository.findByIdOrThrow(behandlingId)

    fun hentVedtakHvisEksisterer(behandlingId: UUID): Vedtak? = vedtakRepository.findByIdOrNull(behandlingId)

    fun hentVedtaksresultat(behandlingId: UUID): ResultatType = hentVedtak(behandlingId).resultatType

    fun hentVedtakForBehandlinger(behandlingIder: Set<UUID>): List<Vedtak> = vedtakRepository.findAllByIdOrThrow(behandlingIder) { it.behandlingId }

    fun hentVedtakDto(behandlingId: UUID): VedtakDto = hentVedtakDtoHvisEksisterer(behandlingId) ?: error("Finner ikke vedtak for behandling=$behandlingId")

    fun hentVedtakDtoHvisEksisterer(behandlingId: UUID): VedtakDto? = vedtakRepository.findByIdOrNull(behandlingId)?.tilVedtakDto()

    fun oppdaterSaksbehandler(
        behandlingId: UUID,
        saksbehandlerIdent: String,
    ) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(saksbehandlerIdent = saksbehandlerIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun oppdaterBeslutter(
        behandlingId: UUID,
        beslutterIdent: String,
    ) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(beslutterIdent = beslutterIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun hentForventetInntektForBehandlingIds(
        behandlingId: UUID,
        dato: LocalDate,
    ): Int? {
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)
        if (vedtak?.erVedtakAktivtForDato(dato) == true) {
            val inntektsperiode =
                vedtak.inntekter?.inntekter?.firstOrNull {
                    it.periode.inneholder(YearMonth.from(dato))
                }
            if (inntektsperiode?.inntekt == null && inntektsperiode?.månedsinntekt == null) {
                return null
            }
            return (inntektsperiode.inntekt.toInt()) + (inntektsperiode.månedsinntekt?.toInt() ?: 0)
        }

        return null
    }

    fun hentForventetInntektForBehandlingIds(behandlingIds: Collection<UUID>): Map<UUID, ForventetInntektForBehandling> =
        vedtakRepository
            .findAllById(behandlingIds)
            .map { vedtak ->
                if (vedtak.erVedtakAktivtForDato(LocalDate.now())) {
                    createForventetInntektForBehandling(vedtak)
                } else {
                    ForventetInntektForBehandling(vedtak.behandlingId, null, null, null, null)
                }
            }.associateBy { it.behandlingId }

    private fun createForventetInntektForBehandling(vedtak: Vedtak): ForventetInntektForBehandling =
        ForventetInntektForBehandling(
            vedtak.behandlingId,
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(1)),
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(2)),
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(3)),
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(4)),
        )

    private fun createForventetInntektForMåned(
        vedtak: Vedtak,
        forventetInntektForDato: YearMonth,
    ): Int? {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(vedtak.behandlingId)
        return tilkjentYtelse
            ?.andelerTilkjentYtelse
            ?.firstOrNull {
                it.periode.inneholder(forventetInntektForDato)
            }?.inntekt
    }

    fun hentHarAktivtVedtak(
        behandlingId: UUID,
        localDate: LocalDate = LocalDate.now(),
    ): Boolean = hentVedtak(behandlingId).erVedtakAktivtForDato(localDate)
}

data class PersonIdentMedForventetInntekt(
    val personIdent: String,
    val forventetInntektForMåned: ForventetInntektForBehandling,
)

data class ForventetInntektForBehandling(
    val behandlingId: UUID,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?,
    val forventetInntektTreMånederTilbake: Int?,
    val forventetInntektFireMånederTilbake: Int?,
)

data class ForventetInntektForPersonIdent(
    val personIdent: String,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?,
    val forventetInntektTreMånederTilbake: Int?,
    val forventetInntektFireMånederTilbake: Int?,
)

fun Vedtak.erVedtakAktivtForDato(dato: LocalDate) =
    this.perioder?.perioder?.any {
        it.periode.inneholder(YearMonth.from(dato))
    } ?: false
