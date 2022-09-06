package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.fagsak.domain.EksternFagsakId
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.min
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.Datoperiode
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

fun oppgave(
    behandling: Behandling,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring
): Oppgave =
    Oppgave(
        behandlingId = behandling.id,
        gsakOppgaveId = gsakOppgaveId,
        type = type,
        erFerdigstilt = erFerdigstilt
    )

fun behandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.VILKÅR,
    id: UUID = UUID.randomUUID(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeBehandlingId: UUID? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT
): Behandling =
    Behandling(
        fagsakId = fagsak.id,
        forrigeBehandlingId = forrigeBehandlingId,
        id = id,
        type = type,
        status = status,
        steg = steg,
        resultat = resultat,
        sporbar = Sporbar(opprettetTid = opprettetTid),
        årsak = årsak,
        henlagtÅrsak = henlagtÅrsak
    )

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.VILKÅR,
    id: UUID = UUID.randomUUID(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeBehandlingId: UUID? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT
): Saksbehandling =
    saksbehandling(
        fagsak,
        Behandling(
            fagsakId = fagsak.id,
            forrigeBehandlingId = forrigeBehandlingId,
            id = id,
            type = type,
            status = status,
            steg = steg,
            resultat = resultat,
            sporbar = Sporbar(opprettetTid = opprettetTid),
            årsak = årsak,
            henlagtÅrsak = henlagtÅrsak
        )
    )

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    behandling: Behandling = behandling()
): Saksbehandling =
    Saksbehandling(
        id = behandling.id,
        eksternId = behandling.eksternId.id,
        forrigeBehandlingId = behandling.forrigeBehandlingId,
        type = behandling.type,
        status = behandling.status,
        steg = behandling.steg,
        årsak = behandling.årsak,
        resultat = behandling.resultat,
        henlagtÅrsak = behandling.henlagtÅrsak,
        ident = fagsak.hentAktivIdent(),
        fagsakId = fagsak.id,
        eksternFagsakId = fagsak.eksternId.id,
        stønadstype = fagsak.stønadstype,
        migrert = fagsak.migrert,
        opprettetAv = behandling.sporbar.opprettetAv,
        opprettetTid = behandling.sporbar.opprettetTid,
        endretTid = behandling.sporbar.endret.endretTid
    )

fun Behandling.innvilgetOgFerdigstilt() =
    this.copy(
        resultat = BehandlingResultat.INNVILGET,
        status = BehandlingStatus.FERDIGSTILT
    )

val defaultIdenter = setOf(PersonIdent("15"))
fun fagsakPerson(
    identer: Set<PersonIdent> = defaultIdenter
) = FagsakPerson(identer = identer)

fun fagsak(
    identer: Set<PersonIdent> = defaultIdenter,
    stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    id: UUID = UUID.randomUUID(),
    eksternId: EksternFagsakId = EksternFagsakId(),
    sporbar: Sporbar = Sporbar(),
    migrert: Boolean = false,
    fagsakPersonId: UUID = UUID.randomUUID()
): Fagsak {
    return fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), eksternId, sporbar, migrert = migrert)
}

fun fagsak(
    stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    id: UUID = UUID.randomUUID(),
    person: FagsakPerson,
    eksternId: EksternFagsakId = EksternFagsakId(),
    sporbar: Sporbar = Sporbar(),
    migrert: Boolean = false
): Fagsak {
    return Fagsak(
        id = id,
        fagsakPersonId = person.id,
        personIdenter = person.identer,
        stønadstype = stønadstype,
        eksternId = eksternId,
        migrert = migrert,
        sporbar = sporbar
    )
}

fun fagsakDomain(
    id: UUID = UUID.randomUUID(),
    stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    personId: UUID = UUID.randomUUID(),
    eksternId: EksternFagsakId = EksternFagsakId()
): FagsakDomain =
    FagsakDomain(
        id = id,
        fagsakPersonId = personId,
        stønadstype = stønadstype,
        eksternId = eksternId
    )

fun Fagsak.tilFagsakDomain() =
    FagsakDomain(
        id = id,
        fagsakPersonId = fagsakPersonId,
        stønadstype = stønadstype,
        eksternId = eksternId,
        sporbar = sporbar
    )

fun vilkårsvurdering(
    behandlingId: UUID,
    resultat: Vilkårsresultat,
    type: VilkårType,
    delvilkårsvurdering: List<Delvilkårsvurdering> = emptyList(),
    barnId: UUID? = null
): Vilkårsvurdering =
    Vilkårsvurdering(
        behandlingId = behandlingId,
        resultat = resultat,
        type = type,
        barnId = barnId,
        delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering)
    )

fun fagsakpersoner(vararg identer: String): Set<PersonIdent> = identer.map {
    PersonIdent(ident = it)
}.toSet()

fun fagsakpersoner(identer: Set<String>): Set<PersonIdent> = identer.map {
    PersonIdent(ident = it)
}.toSet()

fun fagsakpersonerAvPersonIdenter(identer: Set<PersonIdent>): Set<PersonIdent> = identer.map {
    PersonIdent(ident = it.ident, sporbar = it.sporbar)
}.toSet()

fun tilkjentYtelse(
    behandlingId: UUID,
    personIdent: String,
    stønadsår: Int = 2021,
    startmåned: YearMonth? = null,
    grunnbeløpsmåned: YearMonth = YearMonth.of(stønadsår - 1, 5),
    samordningsfradrag: Int = 0,
    beløp: Int = 11554
): TilkjentYtelse {
    val andeler = listOf(
        AndelTilkjentYtelse(
            beløp = beløp,
            periode = Datoperiode(
                LocalDate.of(stønadsår, 1, 1),
                LocalDate.of(stønadsår, 12, 31)
            ),
            personIdent = personIdent,
            inntektsreduksjon = 8396,
            inntekt = 277100,
            samordningsfradrag = samordningsfradrag,
            kildeBehandlingId = behandlingId
        )
    )
    return TilkjentYtelse(
        behandlingId = behandlingId,
        personident = personIdent,
        vedtakstidspunkt = LocalDateTime.now(),
        startmåned = min(startmåned, andeler.minOfOrNull { it.periode.fomMåned }) ?: error("Må sette startdato"),
        andelerTilkjentYtelse = andeler,
        grunnbeløpsmåned = grunnbeløpsmåned
    )
}

fun vedtak(
    behandlingId: UUID,
    resultatType: ResultatType = ResultatType.INNVILGE,
    år: Int = 2021,
    inntekter: InntektWrapper = InntektWrapper(listOf(inntektsperiode(år = år))),
    perioder: PeriodeWrapper = PeriodeWrapper(listOf(vedtaksperiode(år = år)))
): Vedtak =
    Vedtak(
        behandlingId = behandlingId,
        resultatType = resultatType,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        perioder = perioder,
        inntekter = inntekter
    )

fun inntektsperiode(
    år: Int = 2021,
    startDato: LocalDate = LocalDate.of(år, 1, 1),
    sluttDato: LocalDate = LocalDate.of(år, 12, 1),
    inntekt: BigDecimal = BigDecimal.valueOf(100000),
    samordningsfradrag: BigDecimal = BigDecimal.valueOf(500)
) =
    Inntektsperiode(periode = Månedsperiode(startDato, sluttDato), inntekt = inntekt, samordningsfradrag = samordningsfradrag)

fun vedtaksperiode(
    år: Int = 2021,
    startDato: YearMonth = YearMonth.of(år, 1),
    sluttDato: YearMonth = YearMonth.of(år, 12),
    aktivitetstype: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
    vedtaksperiodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE
) =
    Vedtaksperiode(periode = Månedsperiode(startDato, sluttDato), aktivitet = aktivitetstype, periodeType = vedtaksperiodeType)

fun vedtaksperiodeDto(
    årMånedFra: LocalDate = LocalDate.of(2021, 1, 1),
    årMånedTil: LocalDate = LocalDate.of(2021, 12, 1),
    aktivitet: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
    periodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE
) =
    vedtaksperiodeDto(
        årMånedFra = YearMonth.from(årMånedFra),
        årMånedTil = YearMonth.from(årMånedTil),
        aktivitet = aktivitet,
        periodeType = periodeType
    )

fun vedtaksperiodeDto(
    årMånedFra: YearMonth = YearMonth.of(2021, 1),
    årMånedTil: YearMonth = YearMonth.of(2021, 12),
    aktivitet: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
    periodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE
) =
    VedtaksperiodeDto(
        årMånedFra = årMånedFra,
        årMånedTil = årMånedTil,
        periode = Månedsperiode(årMånedFra, årMånedTil),
        aktivitet = aktivitet,
        periodeType = periodeType
    )

fun behandlingBarn(
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID,
    søknadBarnId: UUID? = null,
    personIdent: String? = null,
    navn: String? = null,
    fødselTermindato: LocalDate? = null
): BehandlingBarn {
    return BehandlingBarn(
        id = id,
        behandlingId = behandlingId,
        søknadBarnId = søknadBarnId,
        personIdent = personIdent,
        navn = navn,
        fødselTermindato = fødselTermindato,
        sporbar = Sporbar(opprettetAv = "opprettetAv")
    )
}

fun barnMedIdent(fnr: String, navn: String, fødsel: Fødsel = fødsel(LocalDate.now())): BarnMedIdent =
    BarnMedIdent(
        adressebeskyttelse = emptyList(),
        bostedsadresse = emptyList(),
        deltBosted = emptyList(),
        dødsfall = emptyList(),
        forelderBarnRelasjon = emptyList(),
        fødsel = listOf(fødsel),
        navn = Navn(
            fornavn = navn.split(" ")[0],
            mellomnavn = null,
            etternavn = navn.split(" ")[1],
            metadata = Metadata(
                historisk = false
            )
        ),
        personIdent = fnr
    )

fun søker(): Søker =
    Søker(
        adressebeskyttelse = Adressebeskyttelse(AdressebeskyttelseGradering.UGRADERT, Metadata(false)),
        bostedsadresse = listOf(),
        dødsfall = null,
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        KjønnType.KVINNE,
        listOf(),
        Navn("fornavn", null, "etternavn", Metadata(false)),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf()
    )
