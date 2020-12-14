package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

fun oppgave(behandling: Behandling, erFerdigstilt: Boolean = false): Oppgave =
        Oppgave(behandlingId = behandling.id,
                gsakOppgaveId = 123,
                type = Oppgavetype.Journalføring,
                erFerdigstilt = erFerdigstilt)

fun behandling(fagsak: Fagsak,
               aktiv: Boolean = true,
               status: BehandlingStatus = BehandlingStatus.OPPRETTET,
               steg: StegType = StegType.REGISTRERE_OPPLYSNINGER,
               oppdragId: UUID = UUID.randomUUID()): Behandling =
        Behandling(fagsakId = fagsak.id,
                   id = oppdragId,
                   type = BehandlingType.FØRSTEGANGSBEHANDLING,
                   status = status,
                   steg = steg,
                   aktiv = aktiv)

fun behandlingHistorikk(behandling: Behandling) = BehandlingHistorikk(behandlingId = behandling.id,
                                                                      steg = behandling.steg,
                                                                      endretAvNavn = "Saksbehandlernavn",
                                                                      endretAvMail = SikkerhetContext.hentSaksbehandler(),
                                                                      endretTid = LocalDateTime.now()
                                                                              .truncatedTo(ChronoUnit.MILLIS))

fun fagsak(identer: Set<FagsakPerson> = setOf(), stønadstype: Stønadstype = Stønadstype.OVERGANGSSTØNAD) =
        Fagsak(stønadstype = stønadstype, søkerIdenter = identer)

fun vilkårsvurdering(behandlingId: UUID,
                     resultat: Vilkårsresultat,
                     type: VilkårType,
                     delvilkårsvurdering: List<Delvilkårsvurdering> = emptyList()): Vilkårsvurdering =
        Vilkårsvurdering(behandlingId = behandlingId,
                         resultat = resultat,
                         type = type,
                         delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering))

fun fagsakpersoner(identer: Set<String>): Set<FagsakPerson> = identer.map {
    FagsakPerson(ident = it)
}.toSet()