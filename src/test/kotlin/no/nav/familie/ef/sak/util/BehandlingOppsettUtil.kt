package no.nav.familie.ef.sak.no.nav.familie.ef.sak.util

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Sporbar
import java.time.LocalDateTime

object BehandlingOppsettUtil {

    private val fagsak = fagsak(setOf(FagsakPerson("1")))

    val annullertFørstegangsbehandling = behandling(fagsak)
            .copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.ANNULLERT,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now()
                          .minusDays(4)))

    val førstegangsbehandlingUnderBehandling = behandling(fagsak)
            .copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  status = BehandlingStatus.UTREDES,
                  resultat = BehandlingResultat.IKKE_SATT,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now()
                          .minusDays(4)))

    val førstegangsbehandling = behandling(fagsak)
            .copy(type = BehandlingType.FØRSTEGANGSBEHANDLING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(3)))

    val blankett = behandling(fagsak)
            .copy(type = BehandlingType.BLANKETT,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(2)))

    val annullertRevurdering = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.ANNULLERT,
                  sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)))

    val revurderingUnderArbeid = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.IVERKSETTER_VEDTAK,
                  resultat = BehandlingResultat.INNVILGET)

    val iverksattRevurdering = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET)

    fun lagBehandlingerForSisteIverksatte() = listOf(annullertFørstegangsbehandling,
                                                     førstegangsbehandling,
                                                     blankett,
                                                     annullertRevurdering,
                                                     revurderingUnderArbeid)

}