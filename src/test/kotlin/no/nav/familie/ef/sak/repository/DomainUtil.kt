package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.api.beregning.Inntektsperiode
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.repository.domain.AktivitetType
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.InntektWrapper
import no.nav.familie.ef.sak.repository.domain.Oppgave
import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.ef.sak.repository.domain.SporbarUtils
import no.nav.familie.ef.sak.repository.domain.PeriodeWrapper
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.domain.Vedtaksperiode
import no.nav.familie.ef.sak.repository.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun oppgave(behandling: Behandling, erFerdigstilt: Boolean = false, gsakOppgaveId: Long = 123, type: Oppgavetype = Oppgavetype.Journalføring): Oppgave =
        Oppgave(behandlingId = behandling.id,
                gsakOppgaveId = gsakOppgaveId,
                type = type,
                erFerdigstilt = erFerdigstilt)

fun behandling(fagsak: Fagsak,
               aktiv: Boolean = true,
               status: BehandlingStatus = BehandlingStatus.OPPRETTET,
               steg: StegType = StegType.VILKÅR,
               oppdragId: UUID = UUID.randomUUID(),
               type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
               resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
               opprettetTid: LocalDateTime = SporbarUtils.now()): Behandling =
        Behandling(fagsakId = fagsak.id,
                   id = oppdragId,
                   type = type,
                   status = status,
                   steg = steg,
                   aktiv = aktiv,
                   resultat = resultat,
                   sporbar = Sporbar(opprettetTid = opprettetTid))


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

fun tilkjentYtelse(behandlingId: UUID, personIdent: String): TilkjentYtelse = TilkjentYtelse(
        behandlingId = behandlingId, personident = personIdent, vedtaksdato = LocalDate.now(), andelerTilkjentYtelse = listOf(
        AndelTilkjentYtelse(beløp = 9500,
                            stønadFom = LocalDate.of(2021, 1, 1),
                            stønadTom = LocalDate.of(2021, 12, 31),
                            personIdent = personIdent,
                            inntektsreduksjon = 0,
                            inntekt = 0,
                            samordningsfradrag = 0,
                            kildeBehandlingId = behandlingId))
)

fun vedtak(behandlingId: UUID): Vedtak =
        Vedtak(behandlingId = behandlingId,
               resultatType = ResultatType.INNVILGE,
               periodeBegrunnelse = "OK",
               inntektBegrunnelse = "OK",
               avslåBegrunnelse = null,
               perioder = PeriodeWrapper(listOf(Vedtaksperiode(datoFra = LocalDate.of(2021, 1, 1),
                                                               datoTil = LocalDate.of(2021, 12, 31),
                                                               aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                                               periodeType = VedtaksperiodeType.HOVEDPERIODE))),
               inntekter = InntektWrapper(listOf(Inntektsperiode(startDato = LocalDate.of(2021, 1, 1),
                                                                 sluttDato = LocalDate.of(2021, 12, 1),
                                                                 inntekt = BigDecimal.valueOf(100000),
                                                                 samordningsfradrag = BigDecimal.valueOf(500)))))
