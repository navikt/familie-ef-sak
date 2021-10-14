package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun oppgave(behandling: Behandling,
            erFerdigstilt: Boolean = false,
            gsakOppgaveId: Long = 123,
            type: Oppgavetype = Oppgavetype.Journalføring): Oppgave =
        Oppgave(behandlingId = behandling.id,
                gsakOppgaveId = gsakOppgaveId,
                type = type,
                erFerdigstilt = erFerdigstilt)

fun behandling(fagsak: Fagsak,
               aktiv: Boolean = true,
               status: BehandlingStatus = BehandlingStatus.OPPRETTET,
               steg: StegType = StegType.VILKÅR,
               id: UUID = UUID.randomUUID(),
               type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
               resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
               opprettetTid: LocalDateTime = SporbarUtils.now(),
               forrigeBehandlingId: UUID? = null): Behandling =
        Behandling(fagsakId = fagsak.id,
                   forrigeBehandlingId = forrigeBehandlingId,
                   id = id,
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
                     delvilkårsvurdering: List<Delvilkårsvurdering> = emptyList(),
                     barnId: UUID? = null): Vilkårsvurdering =
        Vilkårsvurdering(behandlingId = behandlingId,
                         resultat = resultat,
                         type = type,
                         barnId = barnId,
                         delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering))

fun fagsakpersoner(identer: Set<String>): Set<FagsakPerson> = identer.map {
    FagsakPerson(ident = it)
}.toSet()

fun tilkjentYtelse(behandlingId: UUID, personIdent: String): TilkjentYtelse = TilkjentYtelse(
        behandlingId = behandlingId,
        personident = personIdent,
        vedtakstidspunkt = LocalDateTime.now(),
        andelerTilkjentYtelse = listOf(
                AndelTilkjentYtelse(beløp = 9500,
                                    stønadFom = LocalDate.of(2021, 1, 1),
                                    stønadTom = LocalDate.of(2021, 12, 31),
                                    personIdent = personIdent,
                                    inntektsreduksjon = 0,
                                    inntekt = 0,
                                    samordningsfradrag = 0,
                                    kildeBehandlingId = behandlingId)))

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
