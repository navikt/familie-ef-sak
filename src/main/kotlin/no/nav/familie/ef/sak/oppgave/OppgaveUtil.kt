package no.nav.familie.ef.sak.oppgave

import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

object OppgaveUtil {

    private val logger = LoggerFactory.getLogger(javaClass)

    val ENHET_NR_NAY = "4489"
    val ENHET_NR_EGEN_ANSATT = "4483"

    fun sekunderSidenEndret(oppgave: Oppgave): Long? {
        val endretTidspunkt = oppgave.endretTidspunkt
        return if (!endretTidspunkt.isNullOrBlank()) {
            try {
                OffsetDateTime.parse(endretTidspunkt).until(OffsetDateTime.now(), ChronoUnit.SECONDS)
            } catch (e: Exception) {
                logger.warn("Feilet parsing av endretTidspunkt=$endretTidspunkt for opgave=$oppgave")
                null
            }
        } else {
            null
        }
    }

    fun finnPersondentForOppgave(oppgave: Oppgave): String? =
        oppgave.identer?.first { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
}
