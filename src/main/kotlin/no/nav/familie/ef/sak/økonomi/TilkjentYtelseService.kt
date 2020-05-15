package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.lagUtbetalingsoppdrag
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
import java.util.*

@Service
class TilkjentYtelseService(
        private val økonomiKlient: ØkonomiKlient,
        private val tilkjentYtelseRepository: TilkjentYtelseRepository,
        private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository
) {

    @Transactional
    fun opprettTilkjentYtelse(tilkjentYtelseDTO: TilkjentYtelseDTO): UUID {
        tilkjentYtelseDTO.valider()

        val opprettetTilkjentYtelse = tilkjentYtelseRepository.save(
                tilkjentYtelseDTO.tilTilkjentYtelse(TilkjentYtelseStatus.OPPRETTET)
        )

        andelTilkjentYtelseRepository.saveAll(tilkjentYtelseDTO.tilAndelerTilkjentYtelse(opprettetTilkjentYtelse.id))

        return opprettetTilkjentYtelse.eksternId
    }

    @Transactional
    fun iverksettUtbetalingsoppdrag(eksternTilkjentYtelseId: UUID) {
        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)

        when (tilkjentYtelse.status) {
            TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING -> return
            TilkjentYtelseStatus.AKTIV -> return
            TilkjentYtelseStatus.IKKE_KLAR -> error("Tilkjent ytelse ${tilkjentYtelse.id} er ikke klar")
            TilkjentYtelseStatus.AVSLUTTET -> error("Tilkjent ytelse ${tilkjentYtelse.id} er avsluttet")
            TilkjentYtelseStatus.OPPRETTET -> sendUtbetalingsoppdragOgOppdaterStatus(
                    tilkjentYtelse,
                    TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING)
        }

    }

    @Transactional
    fun opphørUtbetalingsoppdrag(eksternTilkjentYtelseId: UUID, opphørDato: LocalDate = LocalDate.now()) {
        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)

        when (tilkjentYtelse.status) {
            TilkjentYtelseStatus.OPPRETTET -> error("Tilkjent ytelse ${tilkjentYtelse.id} er opprettet, men ikke iverksatt")
            TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING -> error("Tilkjent ytelse ${tilkjentYtelse.id} er i ferd med å iverksettes")
            TilkjentYtelseStatus.IKKE_KLAR -> error("Tilkjent ytelse ${tilkjentYtelse.id} er ikke klar")
            TilkjentYtelseStatus.AVSLUTTET -> error("Tilkjent ytelse ${tilkjentYtelse.id} er allerede avsluttet")
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

        // Rulles tilbake hvis økonomiKlient.iverksettOppdrag under kaster en exception
        tilkjentYtelseRepository.save(tilkjentYtelse.copy(
                utbetalingsoppdrag = objectMapper.writeValueAsString(utbetalingsoppdrag),
                status = nyStatus
        ))

        gjørKallOgVentPåResponseEntityMedRessurs({ økonomiKlient.iverksettOppdrag(utbetalingsoppdrag) },
                                                 "Iverksetting mot oppdrag feilet")
    }

    fun hentStatus(eksternTilkjentYtelseId: UUID): OppdragProtokollStatus {

        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)

        val statusFraOppdragDTO = StatusFraOppdragDTO(
                fagsystem = FAGSYSTEM,
                personIdent = tilkjentYtelse.personIdentifikator,
                behandlingsId = tilkjentYtelse.id,
                vedtaksId = tilkjentYtelse.id
        )

        return gjørKallOgVentPåResponseEntityMedRessurs({ økonomiKlient.hentStatus(statusFraOppdragDTO) },
                                                        "Henting av status mot oppdrag feilet")
    }

    fun hentTilkjentYtelseDto(eksternTilkjentYtelseId: UUID): TilkjentYtelseDTO {
        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)
        val andelerTilkjentYtelse = hentAndelerTilkjentYtelse(tilkjentYtelse.id)
                .map { it.tilDto() }.toList()

        return tilkjentYtelse.tilDto(andelerTilkjentYtelse)
    }

    private fun hentTilkjentYtelse(eksternTilkjentYtelseId: UUID) =
            tilkjentYtelseRepository.findByEksternIdOrNull(eksternTilkjentYtelseId)
            ?: error("Fant ikke tilkjent ytelse med ekstern id $eksternTilkjentYtelseId")

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
