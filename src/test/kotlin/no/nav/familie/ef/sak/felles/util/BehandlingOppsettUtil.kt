package no.nav.familie.ef.sak.felles.util

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import java.time.LocalDateTime

object BehandlingOppsettUtil {

    private val fagsak = fagsak(setOf(PersonIdent("1")))

    private val henlagtFørstegangsbehandling = behandling(fagsak)
            .copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.HENLAGT,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now()
                          .minusDays(4)))

    val førstegangsbehandlingUnderBehandling = behandling(fagsak)
            .copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  status = BehandlingStatus.UTREDES,
                  resultat = BehandlingResultat.IKKE_SATT,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now()
                          .minusDays(4)))

    val iverksattFørstegangsbehandling = behandling(fagsak)
            .copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(3)))

    val ferdigstiltBlankett = behandling(fagsak)
            .copy(type = BehandlingType.BLANKETT,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(2)))

    val henlagtRevurdering = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.HENLAGT,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)))

    private val revurderingUnderArbeid = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.IVERKSETTER_VEDTAK,
                  resultat = BehandlingResultat.INNVILGET)

    val iverksattRevurdering = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET)

    val revurdering = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.UTREDES,
                  resultat = BehandlingResultat.IKKE_SATT)

    val iverksattTekniskOpphør = behandling(fagsak)
            .copy(type = BehandlingType.TEKNISK_OPPHØR,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET)

    fun lagBehandlingerForSisteIverksatte() = listOf(henlagtFørstegangsbehandling,
                                                     iverksattFørstegangsbehandling,
                                                     ferdigstiltBlankett,
                                                     henlagtRevurdering,
                                                     revurderingUnderArbeid)

}