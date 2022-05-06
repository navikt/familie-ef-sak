package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.felles.dto.Periode
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class VedtakService(private val vedtakRepository: VedtakRepository) {

    fun lagreVedtak(vedtakDto: VedtakDto, behandlingId: UUID, stønadstype: StønadType): UUID {
        return vedtakRepository.insert(vedtakDto.tilVedtak(behandlingId, stønadstype)).behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.findByIdOrNull(behandlingId)
                ?.let { vedtakRepository.deleteById(behandlingId) }
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

    fun hentForventetInntektForBehandlingIds(behandlingId: UUID, dato: LocalDate): Int? {
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)
        if (vedtak?.erVedtakAktivtForDato(dato) == true) {
            return vedtak.inntekter?.inntekter?.firstOrNull {
                dato.isEqualOrAfter(it.startDato) && dato.isEqualOrBefore(it.sluttDato)
            }?.inntekt?.toInt()
        }

        return null
    }

    fun hentForventetInntektForBehandlingIds(behandlingIds: Collection<UUID>): Map<UUID, Int?> {
        val vedtakList = vedtakRepository.findAllById(behandlingIds)
        val dagensDatoMinusEnMåned = LocalDate.now().minusMonths(1)
        val map = mutableMapOf<UUID, Int?>()
        for (vedtak in vedtakList) {
            if (vedtak.erVedtakAktivtForDato(LocalDate.now())) {
                map.put(vedtak.behandlingId, vedtak.inntekter?.inntekter?.firstOrNull {
                    dagensDatoMinusEnMåned.isEqualOrAfter(it.startDato) && dagensDatoMinusEnMåned.isEqualOrBefore(it.sluttDato)
                }?.inntekt?.toInt())
            } else {
                map.put(vedtak.behandlingId, null)
            }
        }

        return map
    }

    fun hentHarAktivtVedtak(behandlingId: UUID, localDate: LocalDate = LocalDate.now()): Boolean {
        return hentVedtak(behandlingId).perioder?.perioder?.any {
            it.datoFra.isEqualOrBefore(localDate) && it.datoTil.isEqualOrAfter(localDate)
        } ?: false
    }
}

fun LocalDate.isEqualOrAfter(dato: LocalDate) = this.equals(dato) || this.isAfter(dato)
fun LocalDate.isEqualOrBefore(dato: LocalDate) = this.equals(dato) || this.isBefore(dato)
fun Vedtak.erVedtakAktivtForDato(dato: LocalDate) = this.perioder?.perioder?.any { Periode(it.datoFra, it.datoTil).omslutter(dato) } ?: false