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
    private val tilkjentYtelseRepository: TilkjentYtelseRepository
) {

    fun lagreVedtak(vedtakDto: VedtakDto, behandlingId: UUID, stønadstype: StønadType): UUID {
        return vedtakRepository.insert(vedtakDto.tilVedtak(behandlingId, stønadstype)).behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.deleteById(behandlingId)
    }

    fun hentVedtak(behandlingId: UUID): Vedtak {
        return vedtakRepository.findByIdOrThrow(behandlingId)
    }

    fun hentVedtaksresultat(behandlingId: UUID): ResultatType {
        return hentVedtak(behandlingId).resultatType
    }

    fun hentVedtakForBehandlinger(behandlingIder: Set<UUID>): List<Vedtak> {
        return vedtakRepository.findAllByIdOrThrow(behandlingIder) { it.behandlingId }
    }

    fun hentVedtakDto(behandlingId: UUID): VedtakDto {
        return hentVedtakHvisEksisterer(behandlingId) ?: error("Finner ikke vedtak for behandling=$behandlingId")
    }

    fun hentVedtakHvisEksisterer(behandlingId: UUID): VedtakDto? {
        return vedtakRepository.findByIdOrNull(behandlingId)?.tilVedtakDto()
    }

    fun oppdaterSaksbehandler(behandlingId: UUID, saksbehandlerIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(saksbehandlerIdent = saksbehandlerIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun oppdaterBeslutter(behandlingId: UUID, beslutterIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(beslutterIdent = beslutterIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    // TODO andre inntekter
    fun hentForventetInntektForBehandlingIds(behandlingId: UUID, dato: LocalDate): Int? {
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)
        if (vedtak?.erVedtakAktivtForDato(dato) == true) {
            return vedtak.inntekter?.inntekter?.firstOrNull {
                it.periode.inneholder(YearMonth.from(dato))
            }?.inntekt?.toInt()
        }

        return null
    }

    fun hentForventetInntektForBehandlingIds(behandlingIds: Collection<UUID>): Map<UUID, ForventetInntektForBehandling> {
        return vedtakRepository.findAllById(behandlingIds).map { vedtak ->
            if (vedtak.erVedtakAktivtForDato(LocalDate.now())) {
                createForventetInntektForBehandling(vedtak)
            } else {
                ForventetInntektForBehandling(vedtak.behandlingId, null, null)
            }
        }.associateBy { it.behandlingId }
    }

    private fun createForventetInntektForBehandling(vedtak: Vedtak): ForventetInntektForBehandling {
        return ForventetInntektForBehandling(
            vedtak.behandlingId,
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(1)),
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(2))
        )
    }

    private fun createForventetInntektForMåned(vedtak: Vedtak, forventetInntektForDato: YearMonth): Int? {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(vedtak.behandlingId)
        return tilkjentYtelse?.andelerTilkjentYtelse?.firstOrNull {
            it.periode.inneholder(forventetInntektForDato)
        }?.inntekt
    }

    fun hentHarAktivtVedtak(behandlingId: UUID, localDate: LocalDate = LocalDate.now()): Boolean {
        return hentVedtak(behandlingId).erVedtakAktivtForDato(localDate)
    }
}

data class PersonIdentMedForventetInntekt(
    val personIdent: String,
    val forventetInntektForMåned: ForventetInntektForBehandling
)

data class ForventetInntektForBehandling(
    val behandlingId: UUID,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?
)

data class ForventetInntektForPersonIdent(
    val personIdent: String,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?
)

fun Vedtak.erVedtakAktivtForDato(dato: LocalDate) = this.perioder?.perioder?.any {
    it.periode.inneholder(YearMonth.from(dato))
} ?: false
