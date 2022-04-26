package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import java.util.UUID

data class BehandlingHistorikkData(
        val behandlingId: UUID,
        val vedtakDto: VedtakDto,
        val aktivitetArbeid: SvarId?,
        val tilkjentYtelse: TilkjentYtelse
)

object BehandlingHistorikkUtil {

    fun lagBehandlingHistorikkData(vedtaksliste: List<Vedtak>,
                                   tilkjentYtelser: List<TilkjentYtelse>,
                                   behandlingIdsTilAktivitetArbeid: Map<UUID, SvarId?>): List<BehandlingHistorikkData> {
        val tilkjentYtelsePerBehandlingId = tilkjentYtelser.associateBy { it.behandlingId }
        return vedtaksliste.map {
            val behandlingId = it.behandlingId
            val tilkjentYtelse = tilkjentYtelsePerBehandlingId[behandlingId]
                                 ?: error("Mangler tilkjent ytelse for behandling=$behandlingId")
            BehandlingHistorikkData(behandlingId,
                                    it.tilVedtakDto(),
                                    behandlingIdsTilAktivitetArbeid[behandlingId],
                                    tilkjentYtelse)
        }
    }

}