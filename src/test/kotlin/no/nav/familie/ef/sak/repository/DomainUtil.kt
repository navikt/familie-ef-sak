package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.util.*

fun oppgave(behandling: Behandling, erFerdigstilt: Boolean = false): Oppgave {
    return Oppgave(
            behandlingId = behandling.id!!,
            gsakOppgaveId = 123,
            type = Oppgavetype.Journalføring,
            erFerdigstilt = erFerdigstilt
    )
}

fun behandling(fagsak: Fagsak, aktiv: Boolean = true, status: BehandlingStatus = BehandlingStatus.OPPRETTET): Behandling {
    return Behandling(
            fagsakId = fagsak.id!!,
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            status = status,
            steg = BehandlingSteg.KOMMER_SENDERE,
            aktiv = aktiv
    )
}

fun fagsak(identer: Set<FagsakPerson> = setOf()) = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD, søkerIdenter = identer)

fun vilkårVurdering(behandlingId: UUID, resultat: VilkårResultat, type: VilkårType): VilkårVurdering =
        VilkårVurdering(behandlingId = behandlingId, resultat = resultat, type = type)

fun fagsakpersoner(identer: Set<String>): Set<FagsakPerson> = identer.map {
    FagsakPerson(ident = it)
}.toSet()