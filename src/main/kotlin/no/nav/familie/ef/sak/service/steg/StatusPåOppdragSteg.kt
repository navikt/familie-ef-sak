package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import org.springframework.stereotype.Service
import java.util.*


@Service
class StatusPåOppdragSteg(private val tilkjentYtelseService: TilkjentYtelseService) : BehandlingSteg<Void?> {

    override fun utførSteg(behandling: Behandling, data: Void?) {
        tilkjentYtelseService.hentStatus(behandling).let {
            when (it) {
                OppdragStatus.KVITTERT_OK -> TODO()
                else -> {
                    kastFeilSlikAtPrøverHenteStatusPåNytt(status = it, behandingId = behandling.id)
                }
            }
        }
    }


    fun kastFeilSlikAtPrøverHenteStatusPåNytt(status: OppdragStatus, behandingId: UUID) {
        error("Mottok status '$status' fra oppdrag for behandlingId $behandingId")
    }

    override fun stegType(): StegType {
        return StegType.STATUS_PÅ_OPPDRAG
    }
}