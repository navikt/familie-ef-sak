package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseStatus
import no.nav.familie.ef.sak.økonomi.dto.StatusFraOppdragDTO
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ØkonomiService(
        private val økonomiKlient: ØkonomiKlient,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {

    @Transactional
    fun oppdaterTilkjentYtelseOgIverksettVedtak(tilkjentYtelseId: Long, saksbehandlerId: String) {
        val tilkjentYtelse = tilkjentYtelseRepository.findById(tilkjentYtelseId).get()
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId)

        val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, tilkjentYtelse, andelerTilkjentYtelse)
        iverksettOppdrag(utbetalingsoppdrag)

        tilkjentYtelseRepository.save(tilkjentYtelse.copy(
                utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
                status = TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING
        ))
    }

    private fun iverksettOppdrag(utbetalingsoppdrag: Utbetalingsoppdrag) {
        Result.runCatching { økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) }
                .fold(
                        onSuccess = {
                            checkNotNull(it.body) { "Finner ikke ressurs" }
                            checkNotNull(it.body?.data) { "Ressurs mangler data" }

                            check(it.body?.status == Ressurs.Status.SUKSESS) {
                                "Ressurs returnerer ${it.body?.status} men har http status kode ${it.statusCode}"
                            }

                        },
                        onFailure = {
                            throw Exception("Iverksetting mot oppdrag feilet", it)
                        }
                )
    }

    fun hentStatus(statusFraOppdragDTO: StatusFraOppdragDTO): OppdragProtokollStatus {
        Result.runCatching { økonomiKlient.hentStatus(statusFraOppdragDTO) }
                .fold(
                        onSuccess = {
                            checkNotNull(it.body) { "Finner ikke ressurs" }
                            checkNotNull(it.body?.data) { "Ressurs mangler data" }
                            check(it.body?.status == Ressurs.Status.SUKSESS) {
                                "Ressurs returnerer ${it.body?.status} men har http status kode ${it.statusCode}"
                            }

                            return it.body?.data!!
                        },
                        onFailure = {
                            throw Exception("Henting av status mot oppdrag feilet", it)
                        }
                )
    }

}
