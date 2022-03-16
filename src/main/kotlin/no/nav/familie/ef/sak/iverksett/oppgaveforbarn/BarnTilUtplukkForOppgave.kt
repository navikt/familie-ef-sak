package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import nonapi.io.github.classgraph.json.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.UUID

data class BarnTilUtplukkForOppgave(@Id
                                    val behandlingId: UUID,
                                    @Column("fodselsnummer_soker")
                                    val fødselsnummerSøker: String,
                                    @Column("fodselsnummer_barn")
                                    val fødselsnummerBarn: String?,
                                    val termindatoBarn: LocalDate?,
                                    val fraMigrering: Boolean)

// TODO flytt til kontrakter
data class OppgaveForBarn(val behandlingId: UUID,
                          val eksternFagsakId: Long,
                          val personIdent: String,
                          val stønadType: String,
                          val beskrivelse: String,
                          val aktivFra: LocalDate? = null)

data class OppgaverForBarnDto(val oppgaverForBarn: List<OppgaveForBarn>)