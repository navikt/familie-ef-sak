package no.nav.familie.ef.sak.iverksett.oppgaveterminbarn

import nonapi.io.github.classgraph.json.Id
import java.time.LocalDate
import java.util.UUID

data class TerminbarnTilUtplukkForOppgave(@Id
                                          val behandlingId: UUID,
                                          val fagsakId: UUID,
                                          val eksternFagsakId : Long,
                                          val termindatoBarn: LocalDate)
