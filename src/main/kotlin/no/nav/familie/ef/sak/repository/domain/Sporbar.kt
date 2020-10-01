package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class Sporbar(@Column("opprettet_av")
                   val opprettetAv: String = finnBrukernavn(),
                   @Column("opprettet_tid")
                   val opprettetTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
                   @Column("endret_av")
                   val endretAv: String = finnBrukernavn(),
                   @Column("endret_tid")
                   val endretTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)) {

    fun oppdater(): Sporbar {
        return this.copy(endretAv = finnBrukernavn(),
                         endretTid = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS))
    }

    companion object {

        private const val BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES = "VL"
        private fun finnBrukernavn(): String {
            val brukerident: String? = null // SikkerhetContext.hentSaksbehandler()
            return brukerident ?: BRUKERNAVN_NÅR_SIKKERHETSKONTEKST_IKKE_FINNES
        }
    }

}
