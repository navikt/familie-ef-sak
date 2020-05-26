package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.Utbetalingsoppdrag.lagUtbetalingsoppdrag
import no.nav.familie.ef.sak.økonomi.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseStatus
import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.økonomi.dto.TilkjentYtelseDTO
import no.nav.familie.kontrakter.felles.getDataOrThrow
import no.nav.familie.kontrakter.felles.oppdrag.OppdragId
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Service
class TilkjentYtelseService(private val økonomiKlient: ØkonomiKlient,
                            private val tilkjentYtelseRepository: TilkjentYtelseRepository,
                            private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository) {

    @Transactional
    fun opprettTilkjentYtelse(tilkjentYtelseDTO: TilkjentYtelseDTO): UUID {
        tilkjentYtelseDTO.valider()

        val eksisterendeTilkjentYtelse = tilkjentYtelseRepository.findByPersonIdentifikatorOrNull(tilkjentYtelseDTO.søker)
        if (eksisterendeTilkjentYtelse != null)
            error("Søker har allerede en tilkjent ytelse")

        val opprettetTilkjentYtelse = tilkjentYtelseRepository.save(
                tilkjentYtelseDTO.tilTilkjentYtelse(TilkjentYtelseStatus.OPPRETTET)
        )

        andelTilkjentYtelseRepository.saveAll(tilkjentYtelseDTO.tilAndelerTilkjentYtelse(opprettetTilkjentYtelse.id))

        return opprettetTilkjentYtelse.eksternId
    }

    @Transactional
    fun iverksettUtbetalingsoppdrag(eksternTilkjentYtelseId: UUID) {
        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)

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
    fun opphørUtbetalingsoppdrag(eksternTilkjentYtelseId: UUID, opphørDato: LocalDate = LocalDate.now()): UUID {
        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)

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

        tilkjentYtelseRepository.save(tilkjentYtelse.copy(status = TilkjentYtelseStatus.AVSLUTTET))

        val lagretOpphørtTilkjentYtelse = tilkjentYtelseRepository.save(tilkjentYtelse.tilOpphør(opphørDato))

        val andelerTilkjentYtelse = andelTilkjentYtelseRepository.findByTilkjentYtelseId(tilkjentYtelse.id)

        sendUtbetalingsoppdragOgOppdaterStatus(lagretOpphørtTilkjentYtelse,
                                               TilkjentYtelseStatus.SENDT_TIL_IVERKSETTING,
                                               andelerTilkjentYtelse)

        return lagretOpphørtTilkjentYtelse.eksternId
    }

    private fun sendUtbetalingsoppdragOgOppdaterStatus(tilkjentYtelse: TilkjentYtelse,
                                                       nyStatus: TilkjentYtelseStatus,
                                                       andelerTilkjentYtelse: Iterable<AndelTilkjentYtelse> =
                                                               hentAndelerTilkjentYtelse(tilkjentYtelse.id)) {
        val saksbehandlerId = SikkerhetContext.hentSaksbehandler()
        val utbetalingsoppdrag = lagUtbetalingsoppdrag(saksbehandlerId, tilkjentYtelse, andelerTilkjentYtelse)

        // Rulles tilbake hvis økonomiKlient.iverksettOppdrag under kaster en exception
        tilkjentYtelseRepository.save(tilkjentYtelse.copy(utbetalingsoppdrag = utbetalingsoppdrag,
                                                          status = nyStatus))

        økonomiKlient.iverksettOppdrag(utbetalingsoppdrag).getDataOrThrow()
    }

    fun hentStatus(eksternTilkjentYtelseId: UUID): OppdragStatus {

        val tilkjentYtelse = hentTilkjentYtelse(eksternTilkjentYtelseId)

        val oppdragId = OppdragId(fagsystem = FAGSYSTEM,
                                  personIdent = tilkjentYtelse.personIdentifikator,
                                  behandlingsId = tilkjentYtelse.id.toString())

        return økonomiKlient.hentStatus(oppdragId).getDataOrThrow()
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
}
