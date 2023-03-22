package no.nav.familie.ef.sak.vedtak.historikk

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingHistorikkData(
    val behandlingId: UUID,
    val vedtakstidspunkt: LocalDateTime,
    val vedtakDto: VedtakDto,
    val aktivitetArbeid: SvarId?,
    val tilkjentYtelse: TilkjentYtelse,
)

object BehandlingHistorikkUtil {

    fun lagBehandlingHistorikkData(
        behandlinger: List<Behandling>,
        vedtaksliste: List<Vedtak>,
        tilkjentYtelser: List<TilkjentYtelse>,
        behandlingIdsTilAktivitetArbeid: Map<UUID, SvarId?>,
    ): List<BehandlingHistorikkData> {
        val tilkjentYtelsePerBehandlingId = tilkjentYtelser.associateBy { it.behandlingId }
        val behandlingerPerBehandlingId = behandlinger.associateBy { it.id }
        return vedtaksliste.map {
            val behandlingId = it.behandlingId
            val tilkjentYtelse = tilkjentYtelsePerBehandlingId[behandlingId]
                ?: error("Mangler tilkjent ytelse for behandling=$behandlingId")
            val vedtakstidspunkt = behandlingerPerBehandlingId.getValue(behandlingId).vedtakstidspunkt
                ?: error("Mangler vedtakstidspunkt for behandling=$behandlingId")
            BehandlingHistorikkData(
                behandlingId = behandlingId,
                vedtakstidspunkt = vedtakstidspunkt,
                vedtakDto = it.tilVedtakDto(),
                aktivitetArbeid = behandlingIdsTilAktivitetArbeid[behandlingId],
                tilkjentYtelse = tilkjentYtelse,
            )
        }
    }
}
