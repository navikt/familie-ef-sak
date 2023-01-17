package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling

object BehandlingUtil {

    fun List<Behandling>.sortertEtterVedtakstidspunkt() =
        this.sortedWith(Comparator.nullsLast(compareBy { it.vedtakstidspunkt }))

    fun List<Behandling>.sisteFerdigstilteBehandling() =
        this.filter { it.erAvsluttet() }
            .maxByOrNull { it.vedtakstidspunktEllerFeil() }
}