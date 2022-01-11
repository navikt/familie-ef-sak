package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import java.time.LocalDate
import java.util.UUID

data class GjeldendeBarn(val id: UUID,
                         val fødselsnummerSøker: String?,
                         val fodselsnummerBarn: String?,
                         val termindatoBarn: LocalDate?)

data class OppgaveForBarn(val id: UUID,
                          val beskrivelse: String,
                          val fødselsnummerSøker: String?,
                          val fødselsnummer: String?,
                          val termindato: LocalDate?)

data class OppgaverForBarnDto(val oppgaverForBarn: List<OppgaveForBarn>)