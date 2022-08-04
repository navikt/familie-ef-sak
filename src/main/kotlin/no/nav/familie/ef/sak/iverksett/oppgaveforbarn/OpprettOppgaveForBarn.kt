package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.felles.util.erOver6Mnd
import no.nav.familie.ef.sak.felles.util.erOverEttÅr
import java.time.LocalDate
import java.util.UUID

data class OpprettOppgaveForBarn(
    val fødselsnummer: String?,
    val fødselsnummerSøker: String,
    val alder: Alder
)

data class OpprettetOppfølgingsoppgave(
    val barnPersonIdent: String,
    val alder: Alder
)

enum class Alder(val oppgavebeskrivelse: String) {
    SEKS_MND("Barn 1/2 år. Send varsel om aktivitetsplikt."),
    ETT_ÅR("Barn 1 år. Vurder aktivitetsplikten.");

    companion object {
        fun fromFødselsdato(fødselsdato: LocalDate?, numberOfDaysCutoff: Long = 7): Alder? {
            fødselsdato?.let {
                if (fødselsdato.erOverEttÅr(numberOfDaysCutoff)) return ETT_ÅR
                if (fødselsdato.erOver6Mnd(numberOfDaysCutoff)) return SEKS_MND
            }

            return null
        }
    }
}

data class BarnEksternIder(val barnPersonIdent: String, val behandlingId: UUID, val eksternBehandlingId: Long, val eksternFagsakId: Long)
