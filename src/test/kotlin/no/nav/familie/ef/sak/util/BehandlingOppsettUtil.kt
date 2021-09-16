package no.nav.familie.ef.sak.no.nav.familie.ef.sak.util

import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakPerson
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

    val revurdering = behandling(fagsak)
            .copy(type = BehandlingType.REVURDERING,
                  status = BehandlingStatus.UTREDES,
                  resultat = BehandlingResultat.IKKE_SATT)

    val iverksattTekniskOpphør = behandling(fagsak)
            .copy(type = BehandlingType.TEKNISK_OPPHØR,
                  status = BehandlingStatus.FERDIGSTILT,
                  resultat = BehandlingResultat.INNVILGET)

    fun lagBehandlingerForSisteIverksatte() = listOf(annullertFørstegangsbehandling,
                                                     iverksattFørstegangsbehandling,
                                                     ferdigstiltBlankett,
                                                     annullertRevurdering,
                                                     revurderingUnderArbeid)

}