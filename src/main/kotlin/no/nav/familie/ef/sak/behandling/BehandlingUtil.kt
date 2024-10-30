package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling

object BehandlingUtil {
    fun List<Behandling>.sortertEtterVedtakstidspunkt() = this.sortedWith(compareBy(nullsLast()) { it.vedtakstidspunkt })

    fun List<Behandling>.sortertEtterVedtakstidspunktEllerEndretTid() = this.sortedBy { it.vedtakstidspunkt ?: it.sporbar.endret.endretTid }

    fun List<Behandling>.sisteFerdigstilteBehandling() =
        this
            .filter { it.erAvsluttet() }
            .maxByOrNull { it.vedtakstidspunktEllerFeil() }
}
