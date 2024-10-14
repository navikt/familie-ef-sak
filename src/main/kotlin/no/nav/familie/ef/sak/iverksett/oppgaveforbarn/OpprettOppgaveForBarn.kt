package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.felles.util.er6MndEllerMerOgInnenforCutoff
import no.nav.familie.ef.sak.felles.util.erEttÅrEllerMerOgInnenforCutoff
import java.time.LocalDate
import java.util.UUID

data class OpprettOppgaveForBarn(
    val fødselsnummer: String,
    val fødselsnummerSøker: String,
    val aktivitetspliktigAlder: AktivitetspliktigAlder,
    val behandlingId: UUID,
)

enum class AktivitetspliktigAlder(
    val oppgavebeskrivelse: String,
) {
    SEKS_MND("Barn 1/2 år. Send varsel om aktivitetsplikt."),
    ETT_ÅR("Barn 1 år. Vurder aktivitetsplikten."),
    ;

    companion object {
        fun fromFødselsdato(
            fødselsdato: LocalDate?,
            numberOfDaysCutoff: Long = 7,
        ): AktivitetspliktigAlder? {
            fødselsdato?.let {
                if (fødselsdato.erEttÅrEllerMerOgInnenforCutoff(numberOfDaysCutoff)) return ETT_ÅR
                if (fødselsdato.er6MndEllerMerOgInnenforCutoff(numberOfDaysCutoff)) return SEKS_MND
            }

            return null
        }
    }
}

data class BarnTilOppgave(
    val barnPersonIdent: String,
    val behandlingId: UUID,
    val eksternBehandlingId: Long,
    val eksternFagsakId: Long,
)
