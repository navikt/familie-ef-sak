package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.integration.FAGSYSTEM
import no.nav.familie.ef.sak.integration.OppdragClient
import no.nav.familie.ef.sak.mapper.tilDto
import no.nav.familie.ef.sak.mapper.tilOpphør
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.TilkjentYtelseRepository
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class TilkjentYtelseService(private val økonomiClient: OppdragClient,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository) {

    @Transactional
    fun opprettTilkjentYtelse(tilkjentYtelseDTO: TilkjentYtelseDTO): UUID {
        tilkjentYtelseDTO.valider()
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()

        val eksisterendeTilkjentYtelse = tilkjentYtelseRepository.findByPersonident(tilkjentYtelseDTO.søker)
        if (eksisterendeTilkjentYtelse != null) {
            error("Søker har allerede en tilkjent ytelse")
        }

        val opprettetTilkjentYtelse =
                tilkjentYtelseRepository.insert(tilkjentYtelseDTO.tilTilkjentYtelse(saksbehandlerId,
                                                                                    TilkjentYtelseStatus.OPPRETTET))

        return opprettetTilkjentYtelse.id
    }

    @Transactional
    fun iverksettUtbetalingsoppdrag(ytelseId: UUID) {
        val tilkjentYtelse = hentTilkjentYtelse(ytelseId)

        when (tilkjentYtelse.type) {
            TilkjentYtelseType.OPPHØR -> error("Tilkjent ytelse ${tilkjentYtelse.id} er opphørt")
            TilkjentYtelseType.ENDRING -> throw NotImplementedError("Har ikke støtte for endring ennå")
            TilkjentYtelseType.FØRSTEGANGSBEHANDLING -> when (tilkjentYtelse.status) {
                TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING -> return
                TilkjentYtelseStatus.AKTIV -> return
                TilkjentYtelseStatus.IKKE_KLAR -> error("Tilkjent ytelse ${tilkjentYtelse.id} er ikke klar")
                TilkjentYtelseStatus.AVSLUTTET -> error("Tilkjent ytelse ${tilkjentYtelse.id} er avsluttet")
                TilkjentYtelseStatus.OPPRETTET -> sendUtbetalingsoppdragOgOppdaterStatus(
                        tilkjentYtelse,
                        TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING)
            }
        }

    }

    @Transactional
    fun opphørUtbetalingsoppdrag(TilkjentYtelseId: UUID, opphørDato: LocalDate = LocalDate.now()): UUID {
        val tilkjentYtelse = hentTilkjentYtelse(TilkjentYtelseId)

        when (tilkjentYtelse.type) {
            TilkjentYtelseType.OPPHØR -> error("Tilkjent ytelse ${tilkjentYtelse.id} er allerede opphørt")
            TilkjentYtelseType.ENDRING -> throw NotImplementedError("Har ikke støtte for endring ennå")
            TilkjentYtelseType.FØRSTEGANGSBEHANDLING -> when (tilkjentYtelse.status) {
                TilkjentYtelseStatus.OPPRETTET -> error("Tilkjent ytelse ${tilkjentYtelse.id} er opprettet, men ikke iverksatt")
                TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING ->
                    error("Tilkjent ytelse ${tilkjentYtelse.id} er i ferd med å iverksettes")
                TilkjentYtelseStatus.IKKE_KLAR -> error("Tilkjent ytelse ${tilkjentYtelse.id} er ikke klar")
                TilkjentYtelseStatus.AVSLUTTET -> error("Tilkjent ytelse ${tilkjentYtelse.id} er allerede avsluttet")
                TilkjentYtelseStatus.AKTIV -> return lagOpphørOgSendUtbetalingsoppdrag(tilkjentYtelse, opphørDato)
            }
        }
    }

    private fun lagOpphørOgSendUtbetalingsoppdrag(tilkjentYtelse: TilkjentYtelse, opphørDato: LocalDate): UUID {

        tilkjentYtelseRepository.update(tilkjentYtelse.copy(status = TilkjentYtelseStatus.AVSLUTTET))

        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        val lagretOpphørtTilkjentYtelse = tilkjentYtelseRepository.insert(tilkjentYtelse.tilOpphør(saksbehandlerId, opphørDato))

        sendUtbetalingsoppdragOgOppdaterStatus(lagretOpphørtTilkjentYtelse,
                                               TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING)

        return lagretOpphørtTilkjentYtelse.id
    }

    private fun sendUtbetalingsoppdragOgOppdaterStatus(tilkjentYtelse: TilkjentYtelse,
                                                       nyStatus: TilkjentYtelseStatus) {
        val utbetalingsoppdrag = lagTilkjentYtelseMedUtbetalingsoppdrag(tilkjentYtelse)
                                         .utbetalingsoppdrag ?: error("Utbetalingsoppdrag har ikke blitt opprettet")

        // Rulles tilbake hvis økonomiKlient.iverksettOppdrag under kaster en exception
        tilkjentYtelseRepository.update(tilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag,
                                                            status = nyStatus))

        økonomiClient.iverksettOppdrag(utbetalingsoppdrag)
    }

    fun hentStatus(tilkjentYtelseId: UUID): OppdragStatus {

        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        val oppdragId = OppdragId(fagsystem = FAGSYSTEM,
                                  personIdent = tilkjentYtelse.personident,
                                  behandlingsId = tilkjentYtelse.id.toString())

        return økonomiClient.hentStatus(oppdragId)
    }

    fun hentTilkjentYtelseDto(tilkjentYtelseId: UUID): TilkjentYtelseDTO {
        val tilkjentYtelse = hentTilkjentYtelse(tilkjentYtelseId)

        return tilkjentYtelse.tilDto()
    }

    private fun hentTilkjentYtelse(tilkjentYtelseId: UUID) =
            tilkjentYtelseRepository.findByIdOrNull(tilkjentYtelseId)
            ?: error("Fant ikke tilkjent ytelse med id $tilkjentYtelseId")

}
