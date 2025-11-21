package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.OppgaveBeskrivelse
import no.nav.familie.ef.sak.oppgave.OppgaveSubtype
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
    val oppfølgingsoppgaverMotLokalKontor: List<OppgaveSubtype>?,
    val innstillingsoppgaveBeskjed: String?,
)

fun OppgaveSubtype.beskrivelse(innstillingsoppgaveBeskjed: String?) =
    when (this) {
        OppgaveSubtype.INFORMERE_OM_SØKT_OVERGANGSSTØNAD -> {
            OppgaveBeskrivelse.informereLokalkontorOmOvergangsstønad
        }

        OppgaveSubtype.INNSTILLING_VEDRØRENDE_UTDANNING -> {
            "${OppgaveBeskrivelse.innstillingOmBrukersUtdanning}" +
                "${if (!innstillingsoppgaveBeskjed.isNullOrEmpty()) "\n $innstillingsoppgaveBeskjed \n" else ""}"
        }
    }
