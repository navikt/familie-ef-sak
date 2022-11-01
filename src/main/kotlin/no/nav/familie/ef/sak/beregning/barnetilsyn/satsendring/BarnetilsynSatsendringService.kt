package no.nav.familie.ef.sak.beregning.barnetilsyn.satsendring

import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnetilsynSatsendringService(
    val barnetilsynSatsendringRepository: BarnetilsynSatsendringRepository,
    val vedtakHistorikkService: VedtakHistorikkService
) {

    fun kjørSatsendring() {
        val fagsakIds = barnetilsynSatsendringRepository.finnSatsendringskandidaterForBarnetilsyn()

        val BarnetilsynSatsendringKanditat = fagsakIds.map { BarnetilsynSatsendringKanditat(it, vedtakHistorikkService.hentAktivHistorikk(it)) }
    }
}

data class BarnetilsynSatsendringKanditat(
    val fagsakId: UUID,
    val andelshistorikk: List<AndelHistorikkDto>
) {
    fun trengerSatsendring(): Boolean {

        return false
    }
}
