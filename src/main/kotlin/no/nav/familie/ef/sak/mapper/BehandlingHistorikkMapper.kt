package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.BehandlingsHistorikkDto
import no.nav.familie.ef.sak.repository.domain.BehandlingsHistorikk

class BehandlingHistorikkMapper {

    companion object {

        fun transform(behandlingHistorikk: BehandlingsHistorikk): BehandlingsHistorikkDto {
            return BehandlingsHistorikkDto(behandlingHistorikk.id,
                                           behandlingHistorikk.behandlingId,
                                           behandlingHistorikk.steg,
                                           behandlingHistorikk.endretAvNavn,
                                           behandlingHistorikk.endretAvMail,
                                           behandlingHistorikk.endretTid)
        }

        fun transform(behandlingHistorikk: List<BehandlingsHistorikk>) : List<BehandlingsHistorikkDto> {
            return behandlingHistorikk.map { transform(it) }.toList()
        }
    }

}
