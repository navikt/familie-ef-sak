package no.nav.familie.ef.sak.vedtak.dto

import no.nav.familie.kontrakter.ef.iverksett.OppgaveForOpprettelseType

data class SendTilBeslutterDto(
    val oppgavetyperSomSkalOpprettes: List<OppgaveForOpprettelseType>,
)
