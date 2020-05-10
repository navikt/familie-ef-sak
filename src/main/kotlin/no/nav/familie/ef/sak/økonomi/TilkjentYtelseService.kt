package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.dto.*
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppdrag.Utbetalingsoppdrag
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class TilkjentYtelseService(
        private val økonomiKlient: ØkonomiKlient,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {
    @Transactional
    fun opprettTilkjentYtelse(tilkjentYtelseRestDTO : TilkjentYtelseRestDTO) : Long {
        tilkjentYtelseRestDTO.valider()

        val tilkjentYtelse = tilkjentYtelseRestDTO.tilTilkjentYtelse(TilkjentYtelseStatus.OPPRETTET)
        val tilkjentYtelseId = tilkjentYtelseRepository.save(tilkjentYtelse).id

        andelTilkjentYtelseRepository.saveAll(tilkjentYtelseRestDTO.tilAndelerTilkjentYtelse(tilkjentYtelseId))

        return tilkjentYtelseId
    }

    @Transactional
    fun iverksettUtbetalingsoppdrag(tilkjentYtelseId: Long, saksbehandlerId: String) {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        when(tilkjentYtelse.status) {
            TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING -> return
            TilkjentYtelseStatus.AKTIV -> return
            TilkjentYtelseStatus.IKKE_KLAR -> throw IllegalStateException("Tilkjent ytelse $tilkjentYtelseId er ikke klar")
            TilkjentYtelseStatus.AVSLUTTET -> throw IllegalStateException("Tilkjent ytelse $tilkjentYtelseId er avsluttet")
            TilkjentYtelseStatus.OPPRETTET -> {
                val andelerTilkjentYtelse = hentAndelerTilkjentYtelse(tilkjentYtelseId)

                val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, tilkjentYtelse, andelerTilkjentYtelse)
                iverksettOppdrag(utbetalingsoppdrag)

                // Mulighet for inkonsistens hvis exception her, eller neste steg feiler
                tilkjentYtelseRepository.save(tilkjentYtelse.copy(
                        utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
                        status = TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING
                ))
            }
        }

     }

    fun opphørUtbetalingsoppdrag(tilkjentYtelseId: Long, saksbehandlerId: String, opphørDato : LocalDate = LocalDate.now()) {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        when(tilkjentYtelse.status) {
            TilkjentYtelseStatus.OPPRETTET ->
                throw java.lang.IllegalStateException("Tilkjent ytelse $tilkjentYtelseId er opprettet, men ikke iverksatt")
            TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING ->
                throw IllegalStateException("Tilkjent ytelse $tilkjentYtelseId er i ferd med å iverksettes")
             TilkjentYtelseStatus.IKKE_KLAR ->
                 throw IllegalStateException("Tilkjent ytelse $tilkjentYtelseId er ikke klar")
            TilkjentYtelseStatus.AVSLUTTET ->
                throw IllegalStateException("Tilkjent ytelse $tilkjentYtelseId er allerede avsluttet")
            TilkjentYtelseStatus.AKTIV -> {
                val andelerTilkjentYtelse = hentAndelerTilkjentYtelse(tilkjentYtelseId)

                val tilkjentYtelseMedOpphør = tilkjentYtelse.copy(opphørFom = opphørDato)

                val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, tilkjentYtelseMedOpphør, andelerTilkjentYtelse)
                iverksettOppdrag(utbetalingsoppdrag)

                // Mulighet for inkonsistens hvis exception her, eller neste steg feiler
                tilkjentYtelseRepository.save(tilkjentYtelseMedOpphør.copy(
                        status = TilkjentYtelseStatus.AVSLUTTET,
                        utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag)
                ))
            }
        }

    }

    fun hentTilkjentYtelseDto(tilkjentYtelseId: Long): TilkjentYtelseRestDTO {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)
        val andelerTilkjentYtelse = hentAndelerTilkjentYtelse(tilkjentYtelseId)
                .map { it.tilDto() }.toList()

        return tilkjentYtelse.tilDto(andelerTilkjentYtelse)
    }


    fun hentStatus(tilkjentYtelseId: Long): OppdragProtokollStatus {

        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        val statusFraOppdragDTO = StatusFraOppdragDTO(
                fagsystem = FAGSYSTEM,
                personIdent = tilkjentYtelse.personIdentifikator,
                behandlingsId = tilkjentYtelseId,
                vedtaksId = tilkjentYtelseId
        )

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

    private fun hentTilkjentYtelse(tilkjentYtelseId: Long): TilkjentYtelse {
        val kanskjeTilkjentYtelse = tilkjentYtelseRepository.findById(tilkjentYtelseId)

        if (kanskjeTilkjentYtelse.isEmpty()) {
            throw IllegalArgumentException("Fant ikke tilkjent ytelse med id $tilkjentYtelseId")
        }

        return kanskjeTilkjentYtelse.get()
    }

    private fun hentAndelerTilkjentYtelse(tilkjentYtelseId: Long) : List<AndelTilkjentYtelse> {
        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId)

        if(andelerTilkjentYtelse.size==0) {
            throw java.lang.IllegalStateException("Fant ikke andeler tilkjent ytelse for tilkjent ytelse med id $tilkjentYtelseId")
        }

        return andelerTilkjentYtelse
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

}
