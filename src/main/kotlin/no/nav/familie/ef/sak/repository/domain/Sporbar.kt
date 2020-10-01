package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class Sporbar(
        @Column("opprettet_av")
        val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
        @Column("opprettet_tid")
        val opprettetTid: LocalDateTime = SporbarUtils.now(),

        @LastModifiedBy
        @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
        val endret: Endret = Endret()
)

data class Endret(@Column("endret_av")
                  val endretAv: String = SikkerhetContext.hentSaksbehandler(),
                  @Column("endret_tid")
                  val endretTid: LocalDateTime = SporbarUtils.now())

object SporbarUtils {

    fun now() = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
}
