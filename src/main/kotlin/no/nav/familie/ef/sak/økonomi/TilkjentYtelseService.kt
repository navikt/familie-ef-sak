package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.økonomi.dto.*
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
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
    fun opprettTilkjentYtelse(tilkjentYtelseDTO: TilkjentYtelseDTO): Long {
        tilkjentYtelseDTO.valider()

        val tilkjentYtelse = tilkjentYtelseDTO.tilTilkjentYtelse(TilkjentYtelseStatus.OPPRETTET)
        val tilkjentYtelseId = tilkjentYtelseRepository.save(tilkjentYtelse).id

        andelTilkjentYtelseRepository.saveAll(tilkjentYtelseDTO.tilAndelerTilkjentYtelse(tilkjentYtelseId))

        return tilkjentYtelseId
    }

    @Transactional
    fun iverksettUtbetalingsoppdrag(tilkjentYtelseId: Long) {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        when (tilkjentYtelse.status) {
            TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING -> return
            TilkjentYtelseStatus.AKTIV -> return
            TilkjentYtelseStatus.IKKE_KLAR -> error("Tilkjent ytelse $tilkjentYtelseId er ikke klar")
            TilkjentYtelseStatus.AVSLUTTET -> error("Tilkjent ytelse $tilkjentYtelseId er avsluttet")
            TilkjentYtelseStatus.OPPRETTET -> sendUtbetalingsoppdragOgOppdaterStatus(
                    tilkjentYtelse,
                    TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING)
        }

    }

    @Transactional
    fun opphørUtbetalingsoppdrag(tilkjentYtelseId: Long, opphørDato: LocalDate = LocalDate.now()) {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        when (tilkjentYtelse.status) {
            TilkjentYtelseStatus.OPPRETTET -> error("Tilkjent ytelse $tilkjentYtelseId er opprettet, men ikke iverksatt")
            TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING -> error("Tilkjent ytelse $tilkjentYtelseId er i ferd med å iverksettes")
            TilkjentYtelseStatus.IKKE_KLAR -> error("Tilkjent ytelse $tilkjentYtelseId er ikke klar")
            TilkjentYtelseStatus.AVSLUTTET -> error("Tilkjent ytelse $tilkjentYtelseId er allerede avsluttet")
            TilkjentYtelseStatus.AKTIV -> sendUtbetalingsoppdragOgOppdaterStatus(
                    tilkjentYtelse.copy(opphørFom = opphørDato),
                    TilkjentYtelseStatus.AVSLUTTET)
        }
    }

    private fun sendUtbetalingsoppdragOgOppdaterStatus(tilkjentYtelse: TilkjentYtelse,
                                                       nyStatus: TilkjentYtelseStatus) {

        val andelerTilkjentYtelse = hentAndelerTilkjentYtelse(tilkjentYtelse.id)
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, tilkjentYtelse, andelerTilkjentYtelse)

        // Rulles tilbake hvis iverksettOppdrag under kaster en exception
        tilkjentYtelseRepository.save(tilkjentYtelse.copy(
                utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
                status = nyStatus
        ))

        gjørKallOgVentPåResponseEntityMedRessurs({ økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) },
                                                 "Iverksetting mot oppdrag feilet")
    }

    fun hentStatus(tilkjentYtelseId: Long): OppdragProtokollStatus {

        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        val statusFraOppdragDTO = StatusFraOppdragDTO(
                fagsystem = FAGSYSTEM,
                personIdent = tilkjentYtelse.personIdentifikator,
                behandlingsId = tilkjentYtelseId,
                vedtaksId = tilkjentYtelseId
        )

        return gjørKallOgVentPåResponseEntityMedRessurs({ økonomiKlient.hentStatus(statusFraOppdragDTO) },
                                                        "Henting av status mot oppdrag feilet")
    }

    fun hentTilkjentYtelseDto(tilkjentYtelseId: Long): TilkjentYtelseDTO {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)
        val andelerTilkjentYtelse = hentAndelerTilkjentYtelse(tilkjentYtelseId)
                .map { it.tilDto() }.toList()

        return tilkjentYtelse.tilDto(andelerTilkjentYtelse)
    }

    private fun hentTilkjentYtelse(tilkjentYtelseId: Long) =
            tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)
            ?: error("Fant ikke tilkjent ytelse med id $tilkjentYtelseId")

    private fun hentAndelerTilkjentYtelse(tilkjentYtelseId: Long) =
            andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelseId)
                    .ifEmpty { error("Fant ikke andeler tilkjent ytelse for tilkjent ytelse med id $tilkjentYtelseId") }

    private fun <T : Any> gjørKallOgVentPåResponseEntityMedRessurs(
            kall: () -> ResponseEntity<Ressurs<T>>,
            failureMessage: String): T
    {
        Result.runCatching { kall() }
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
                            throw Exception(failureMessage, it)
                        }
                )
    }

}
