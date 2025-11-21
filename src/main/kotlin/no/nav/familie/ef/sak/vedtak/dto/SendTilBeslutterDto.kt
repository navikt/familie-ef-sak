package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.ef.sak.brev.Brevmal
import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType

data class SendTilBeslutterDto(
    val oppgavetyperSomSkalOpprettes: List<OppgaveForOpprettelseType>,
    val årForInntektskontrollSelvstendigNæringsdrivende: Int? = null,
    val oppgaverIderSomSkalFerdigstilles: List<Long> = emptyList(),
    val beskrivelseMarkeringer: List<String> = emptyList(),
    val automatiskBrev: List<Brevmal> = emptyList(),
    val høyprioritet: Boolean? = false,
)
