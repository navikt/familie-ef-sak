package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.OppgaveBeskrivelse
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet

// TODO: Fjern nullable for oppfølgingsoppgaverMotLokalKontor etter 05.05.2023
data class SettPåVentRequest(
    val oppgaveId: Long,
    val saksbehandler: String,
    val prioritet: OppgavePrioritet,
    val frist: String,
    val mappe: Long?,
    val beskrivelse: String,
    val oppgaveVersjon: Int,
    val oppfølgingsoppgaverMotLokalKontor: List<VurderHenvendelseOppgaveSubtype>?,
)

enum class VurderHenvendelseOppgaveSubtype {
    INFORMERE_OM_SØKT_OVERGANGSSTØNAD,
    INNSTILLING_VEDRØRENDE_UTDANNING,
}
fun VurderHenvendelseOppgaveSubtype.beskrivelse() = when (this) {
    VurderHenvendelseOppgaveSubtype.INFORMERE_OM_SØKT_OVERGANGSSTØNAD -> OppgaveBeskrivelse.informereLokalkontorOmOvergangsstønad
    VurderHenvendelseOppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING -> OppgaveBeskrivelse.innstillingOmBrukersUtdanning
}
