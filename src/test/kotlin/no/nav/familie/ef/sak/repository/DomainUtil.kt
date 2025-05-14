package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.amelding.Inntekt
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.amelding.InntektType
import no.nav.familie.ef.sak.amelding.Inntektsmåned
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.RevurderingsinformasjonDto
import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import no.nav.familie.ef.sak.felles.util.min
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SivilstandMedNavn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.Søker
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsdag
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke
import no.nav.familie.ef.sak.samværsavtale.domain.SamværsukeWrapper
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.fødsel
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.metadataGjeldende
import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.SamordningsfradragType
import no.nav.familie.ef.sak.vedtak.domain.SkolepengerWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.Opphavsvilkår
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import no.nav.familie.ef.sak.vilkår.regler.BarnForelderLangAvstandTilSøker
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.evalutation.RegelEvaluering.utledResultat
import no.nav.familie.ef.sak.vilkår.regler.vilkårsreglerForStønad
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.kontrakter.felles.saksbehandler.Saksbehandler
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.util.UUID

fun oppgave(
    behandling: Behandling,
    erFerdigstilt: Boolean = false,
    gsakOppgaveId: Long = 123,
    type: Oppgavetype = Oppgavetype.Journalføring,
): Oppgave =
    Oppgave(
        behandlingId = behandling.id,
        gsakOppgaveId = gsakOppgaveId,
        type = type,
        erFerdigstilt = erFerdigstilt,
    )

fun behandling(
    fagsak: Fagsak = fagsak(),
    status: BehandlingStatus = BehandlingStatus.OPPRETTET,
    steg: StegType = StegType.VILKÅR,
    kategori: BehandlingKategori = BehandlingKategori.NASJONAL,
    id: UUID = UUID.randomUUID(),
    type: BehandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
    resultat: BehandlingResultat = BehandlingResultat.IKKE_SATT,
    opprettetTid: LocalDateTime = SporbarUtils.now(),
    forrigeBehandlingId: UUID? = null,
    årsak: BehandlingÅrsak = BehandlingÅrsak.SØKNAD,
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    eksternId: Long = 0L,
    vedtakstidspunkt: LocalDateTime? = null,
    kravMottatt: LocalDate? = null,
): Behandling =
    Behandling(
        fagsakId = fagsak.id,
        forrigeBehandlingId = forrigeBehandlingId,
        id = id,
        type = type,
        status = status,
        steg = steg,
        kategori = kategori,
        resultat = resultat,
        sporbar = Sporbar(opprettetTid = opprettetTid),
        årsak = årsak,
        henlagtÅrsak = henlagtÅrsak,
        eksternId = eksternId,
        vedtakstidspunkt =
            vedtakstidspunkt
                ?: if (resultat != BehandlingResultat.IKKE_SATT) SporbarUtils.now() else null,
        kravMottatt = kravMottatt,
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
    henlagtÅrsak: HenlagtÅrsak? = HenlagtÅrsak.FEILREGISTRERT,
    kravMottatt: LocalDate? = null,
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
            henlagtÅrsak = henlagtÅrsak,
            kravMottatt = kravMottatt,
            kategori = BehandlingKategori.NASJONAL,
        ),
    )

fun saksbehandling(
    fagsak: Fagsak = fagsak(),
    behandling: Behandling = behandling(),
): Saksbehandling =
    Saksbehandling(
        id = behandling.id,
        eksternId = behandling.eksternId,
        forrigeBehandlingId = behandling.forrigeBehandlingId,
        type = behandling.type,
        status = behandling.status,
        steg = behandling.steg,
        kategori = behandling.kategori,
        årsak = behandling.årsak,
        resultat = behandling.resultat,
        vedtakstidspunkt = behandling.vedtakstidspunkt,
        henlagtÅrsak = behandling.henlagtÅrsak,
        ident = fagsak.hentAktivIdent(),
        fagsakId = fagsak.id,
        eksternFagsakId = fagsak.eksternId,
        stønadstype = fagsak.stønadstype,
        migrert = fagsak.migrert,
        opprettetAv = behandling.sporbar.opprettetAv,
        opprettetTid = behandling.sporbar.opprettetTid,
        endretTid = behandling.sporbar.endret.endretTid,
        kravMottatt = behandling.kravMottatt,
    )

fun Behandling.innvilgetOgFerdigstilt() =
    this.copy(
        resultat = BehandlingResultat.INNVILGET,
        status = BehandlingStatus.FERDIGSTILT,
        vedtakstidspunkt = SporbarUtils.now(),
    )

val defaultIdenter = setOf(PersonIdent("15"))

fun fagsakPerson(
    identer: Set<PersonIdent> = defaultIdenter,
) = FagsakPerson(identer = identer)

fun fagsak(
    identer: Set<PersonIdent> = defaultIdenter,
    stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    id: UUID = UUID.randomUUID(),
    eksternId: Long = 0,
    sporbar: Sporbar = Sporbar(),
    migrert: Boolean = false,
    fagsakPersonId: UUID = UUID.randomUUID(),
): Fagsak = fagsak(stønadstype, id, FagsakPerson(id = fagsakPersonId, identer = identer), eksternId, sporbar, migrert = migrert)

fun fagsak(
    stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    id: UUID = UUID.randomUUID(),
    person: FagsakPerson,
    eksternId: Long = 0,
    sporbar: Sporbar = Sporbar(),
    migrert: Boolean = false,
): Fagsak =
    Fagsak(
        id = id,
        fagsakPersonId = person.id,
        personIdenter = person.identer,
        stønadstype = stønadstype,
        eksternId = eksternId,
        migrert = migrert,
        sporbar = sporbar,
    )

fun fagsakDomain(
    id: UUID = UUID.randomUUID(),
    stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    personId: UUID = UUID.randomUUID(),
    eksternId: Long = 0,
): FagsakDomain =
    FagsakDomain(
        id = id,
        fagsakPersonId = personId,
        stønadstype = stønadstype,
        eksternId = eksternId,
    )

fun Fagsak.tilFagsakDomain() =
    FagsakDomain(
        id = id,
        fagsakPersonId = fagsakPersonId,
        stønadstype = stønadstype,
        eksternId = eksternId,
        sporbar = sporbar,
    )

fun samværsavtale(
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID = UUID.randomUUID(),
    behandlingBarnid: UUID = UUID.randomUUID(),
    uker: List<Samværsuke> = emptyList(),
) = Samværsavtale(
    id = id,
    behandlingId = behandlingId,
    behandlingBarnId = behandlingBarnid,
    uker = SamværsukeWrapper(uker = uker),
)

fun samværsuke(andeler: List<Samværsandel> = emptyList()) =
    Samværsuke(
        mandag = samværsdag(andeler = andeler),
        tirsdag = samværsdag(andeler = andeler),
        onsdag = samværsdag(andeler = andeler),
        torsdag = samværsdag(andeler = andeler),
        fredag = samværsdag(andeler = andeler),
        lørdag = samværsdag(andeler = andeler),
        søndag = samværsdag(andeler = andeler),
    )

fun samværsdag(andeler: List<Samværsandel> = emptyList()) = Samværsdag(andeler = andeler)

fun vilkårsvurdering(
    behandlingId: UUID,
    resultat: Vilkårsresultat = Vilkårsresultat.OPPFYLT,
    type: VilkårType = VilkårType.LOVLIG_OPPHOLD,
    delvilkårsvurdering: List<Delvilkårsvurdering> = emptyList(),
    barnId: UUID? = null,
    opphavsvilkår: Opphavsvilkår? = null,
): Vilkårsvurdering =
    Vilkårsvurdering(
        behandlingId = behandlingId,
        resultat = resultat,
        type = type,
        barnId = barnId,
        delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering),
        opphavsvilkår = opphavsvilkår,
    )

fun fagsakpersoner(vararg identer: String): Set<PersonIdent> =
    identer
        .map {
            PersonIdent(ident = it)
        }.toSet()

fun fagsakpersoner(identer: Set<String>): Set<PersonIdent> =
    identer
        .map {
            PersonIdent(ident = it)
        }.toSet()

fun fagsakpersonerAvPersonIdenter(identer: Set<PersonIdent>): Set<PersonIdent> =
    identer
        .map {
            PersonIdent(ident = it.ident, sporbar = it.sporbar)
        }.toSet()

fun årsakRevurdering(
    behandlingId: UUID = UUID.randomUUID(),
    opplysningskilde: Opplysningskilde = Opplysningskilde.MELDING_MODIA,
    årsak: Revurderingsårsak = Revurderingsårsak.ANNET,
    beskrivelse: String? = null,
) = ÅrsakRevurdering(
    behandlingId = behandlingId,
    opplysningskilde = opplysningskilde,
    årsak = årsak,
    beskrivelse = beskrivelse,
)

fun revurderingsinformasjon() =
    RevurderingsinformasjonDto(
        LocalDate.now(),
        ÅrsakRevurderingDto(Opplysningskilde.MELDING_MODIA, Revurderingsårsak.ANNET, "beskrivelse"),
    )

fun tilkjentYtelse(
    behandlingId: UUID,
    personIdent: String,
    stønadsår: Int = 2021,
    startdato: LocalDate? = null,
    grunnbeløpsmåned: YearMonth = YearMonth.of(stønadsår - 1, 5),
    samordningsfradrag: Int = 0,
    beløp: Int = 11554,
    inntekt: Int = 277100,
): TilkjentYtelse {
    val andeler =
        listOf(
            AndelTilkjentYtelse(
                beløp = beløp,
                stønadFom = LocalDate.of(stønadsår, 1, 1),
                stønadTom = LocalDate.of(stønadsår, 12, 31),
                personIdent = personIdent,
                inntektsreduksjon = 8396,
                inntekt = inntekt,
                samordningsfradrag = samordningsfradrag,
                kildeBehandlingId = behandlingId,
            ),
        )
    return TilkjentYtelse(
        behandlingId = behandlingId,
        personident = personIdent,
        startdato = min(startdato, andeler.minOfOrNull { it.stønadFom }) ?: error("Må sette startdato"),
        andelerTilkjentYtelse = andeler,
        grunnbeløpsmåned = grunnbeløpsmåned,
    )
}

fun vedtak(
    behandlingId: UUID,
    resultatType: ResultatType = ResultatType.INNVILGE,
    år: Int = 2021,
    inntekter: InntektWrapper = InntektWrapper(listOf(inntektsperiode(år = år))),
    perioder: PeriodeWrapper = PeriodeWrapper(listOf(vedtaksperiode(år = år))),
    skolepenger: SkolepengerWrapper? = null,
    samordningsfradragType: SamordningsfradragType? = null,
): Vedtak =
    Vedtak(
        behandlingId = behandlingId,
        resultatType = resultatType,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        perioder = perioder,
        inntekter = inntekter,
        skolepenger = skolepenger,
        saksbehandlerIdent = "VL",
        opprettetAv = "VL",
        opprettetTid = LocalDateTime.now(),
        samordningsfradragType = samordningsfradragType,
    )

fun vedtak(
    behandlingId: UUID,
    månedsperiode: Månedsperiode,
): Vedtak =
    Vedtak(
        behandlingId = behandlingId,
        resultatType = ResultatType.INNVILGE,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        perioder = PeriodeWrapper(listOf(vedtaksperiode(startDato = månedsperiode.fomDato, sluttDato = månedsperiode.tomDato))),
        inntekter = InntektWrapper(listOf(inntektsperiode(startDato = månedsperiode.fomDato, sluttDato = månedsperiode.tomDato))),
        skolepenger = null,
        saksbehandlerIdent = "VL",
        opprettetAv = "VL",
        opprettetTid = LocalDateTime.now(),
        samordningsfradragType = null,
    )

fun vedtak(
    behandlingId: UUID,
    månedsperioder: List<Månedsperiode>,
): Vedtak =
    Vedtak(
        behandlingId = behandlingId,
        resultatType = ResultatType.INNVILGE,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        perioder = PeriodeWrapper(månedsperioder.map { vedtaksperiode(startDato = it.fomDato, sluttDato = it.tomDato) }),
        inntekter = InntektWrapper(månedsperioder.map { inntektsperiode(startDato = it.fomDato, sluttDato = it.tomDato) }),
        skolepenger = null,
        saksbehandlerIdent = "VL",
        opprettetAv = "VL",
        opprettetTid = LocalDateTime.now(),
        samordningsfradragType = null,
    )

fun vedtak(
    inntektMånedTilBeløpMap: Map<YearMonth, Int>,
    vedtakTom: YearMonth = YearMonth.now().plusMonths(12),
): Vedtak {
    val sisteMåned = inntektMånedTilBeløpMap.keys.maxOrNull()

    val inntektsperioder =
        inntektMånedTilBeløpMap.map { (måned, beløp) ->
            val sluttDato =
                if (måned == sisteMåned) {
                    vedtakTom.atEndOfMonth()
                } else {
                    måned.atEndOfMonth()
                }

            inntektsperiode(
                startDato = måned.atDay(1),
                sluttDato = sluttDato,
                månedsinntekt = BigDecimal.valueOf(beløp.toDouble()),
                inntekt = 0.toBigDecimal(),
            )
        }

    return vedtak(InntektWrapper(inntektsperioder))
}

fun vedtak(
    inntekter: InntektWrapper,
): Vedtak =
    Vedtak(
        behandlingId = UUID.randomUUID(),
        resultatType = ResultatType.INNVILGE,
        periodeBegrunnelse = "OK",
        inntektBegrunnelse = "OK",
        avslåBegrunnelse = null,
        perioder =
            PeriodeWrapper(
                listOf(
                    vedtaksperiode(
                        startDato =
                            inntekter.inntekter
                                .minBy { it.periode.fom }
                                .periode.fomDato,
                        sluttDato =
                            inntekter.inntekter
                                .maxBy { it.periode.tom }
                                .periode.tomDato,
                    ),
                ),
            ),
        inntekter = inntekter,
        saksbehandlerIdent = "VL",
        opprettetAv = "VL",
        opprettetTid = LocalDateTime.now(),
    )

fun vedtakBarnetilsyn(
    behandlingId: UUID,
    barn: List<UUID>,
    resultatType: ResultatType = ResultatType.INNVILGE,
    beløp: Int = 1000,
    kontantstøtteWrapper: KontantstøtteWrapper = KontantstøtteWrapper(emptyList(), null),
    fom: YearMonth,
    tom: YearMonth,
) = Vedtak(
    behandlingId = behandlingId,
    resultatType = resultatType,
    barnetilsyn = BarnetilsynWrapper(listOf(barnetilsynperiode(barn = barn, beløp = beløp, fom = fom, tom = tom)), "begrunnelse"),
    kontantstøtte = kontantstøtteWrapper,
    tilleggsstønad = TilleggsstønadWrapper(emptyList(), null),
    saksbehandlerIdent = "VL",
    opprettetAv = "VL",
    opprettetTid = LocalDateTime.now(),
)

fun barnetilsynperiode(
    år: Int = 2022,
    fom: YearMonth = YearMonth.of(år, 1),
    tom: YearMonth = YearMonth.of(år, 12),
    beløp: Int = 1000,
    barn: List<UUID>,
    sanksjonsårsak: Sanksjonsårsak? = null,
    periodetype: PeriodetypeBarnetilsyn = PeriodetypeBarnetilsyn.ORDINÆR,
    aktivitetstype: AktivitetstypeBarnetilsyn = AktivitetstypeBarnetilsyn.I_ARBEID,
) = Barnetilsynperiode(
    periode = Månedsperiode(fom, tom),
    utgifter = beløp,
    barn = barn,
    sanksjonsårsak = sanksjonsårsak,
    periodetype = periodetype,
    aktivitet = aktivitetstype,
)

fun inntektsperiode(
    år: Int = 2021,
    startDato: LocalDate = LocalDate.of(år, 1, 1),
    sluttDato: LocalDate = LocalDate.of(år, 12, 1),
    inntekt: BigDecimal = BigDecimal.valueOf(100000),
    samordningsfradrag: BigDecimal = BigDecimal.valueOf(500),
    dagsats: BigDecimal? = null,
    månedsinntekt: BigDecimal? = null,
) = Inntektsperiode(periode = Månedsperiode(startDato, sluttDato), dagsats = dagsats, månedsinntekt = månedsinntekt, inntekt = inntekt, samordningsfradrag = samordningsfradrag)

fun inntektsperiode(
    månedsperiode: Månedsperiode,
    månedsinntekt: BigDecimal? = null,
) = Inntektsperiode(periode = månedsperiode, månedsinntekt = månedsinntekt, inntekt = 0.toBigDecimal(), samordningsfradrag = 0.toBigDecimal())

fun vedtaksperiode(
    år: Int = 2021,
    startDato: LocalDate = LocalDate.of(år, 1, 1),
    sluttDato: LocalDate = LocalDate.of(år, 12, 1),
    vedtaksperiodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
    aktivitetstype: AktivitetType =
        if (vedtaksperiodeType == VedtaksperiodeType.SANKSJON) AktivitetType.IKKE_AKTIVITETSPLIKT else AktivitetType.BARN_UNDER_ETT_ÅR,
    sanksjonsårsak: Sanksjonsårsak? =
        if (vedtaksperiodeType == VedtaksperiodeType.SANKSJON) Sanksjonsårsak.SAGT_OPP_STILLING else null,
) = Vedtaksperiode(startDato, sluttDato, aktivitetstype, vedtaksperiodeType, sanksjonsårsak)

fun vedtaksperiodeDto(
    årMånedFra: LocalDate = LocalDate.of(2021, 1, 1),
    årMånedTil: LocalDate = LocalDate.of(2021, 12, 1),
    periodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
    aktivitet: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
) = vedtaksperiodeDto(
    årMånedFra = YearMonth.from(årMånedFra),
    årMånedTil = YearMonth.from(årMånedTil),
    periodeType = periodeType,
    aktivitet = aktivitet,
)

fun vedtaksperiodeDto(
    årMånedFra: YearMonth = YearMonth.of(2021, 1),
    årMånedTil: YearMonth = YearMonth.of(2021, 12),
    periodeType: VedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
    aktivitet: AktivitetType = AktivitetType.BARN_UNDER_ETT_ÅR,
) = VedtaksperiodeDto(
    årMånedFra = årMånedFra,
    årMånedTil = årMånedTil,
    periode = Månedsperiode(årMånedFra, årMånedTil),
    aktivitet = aktivitet,
    periodeType = periodeType,
)

fun behandlingBarn(
    id: UUID = UUID.randomUUID(),
    behandlingId: UUID,
    søknadBarnId: UUID? = null,
    personIdent: String? = null,
    navn: String? = null,
    fødselTermindato: LocalDate? = null,
): BehandlingBarn =
    BehandlingBarn(
        id = id,
        behandlingId = behandlingId,
        søknadBarnId = søknadBarnId,
        personIdent = personIdent,
        navn = navn,
        fødselTermindato = fødselTermindato,
        sporbar = Sporbar(opprettetAv = "opprettetAv"),
    )

fun barnMedIdent(
    fnr: String,
    navn: String,
    fødsel: Fødsel = fødsel(LocalDate.now()),
): BarnMedIdent =
    BarnMedIdent(
        adressebeskyttelse = emptyList(),
        bostedsadresse = emptyList(),
        deltBosted = emptyList(),
        dødsfall = emptyList(),
        forelderBarnRelasjon = emptyList(),
        fødsel = listOf(fødsel),
        navn =
            Navn(
                fornavn = navn.split(" ")[0],
                mellomnavn = null,
                etternavn = navn.split(" ")[1],
                metadata =
                    Metadata(
                        historisk = false,
                    ),
            ),
        personIdent = fnr,
        null,
    )

fun hovedregelMetadata(
    sivilstand: Sivilstand? = null,
    sivilstandstype: Sivilstandstype = Sivilstandstype.SKILT,
    erMigrering: Boolean = false,
    barn: List<BehandlingBarn> = emptyList(),
    søktOmBarnetilsyn: List<UUID> = emptyList(),
    langAvstandTilSøker: List<BarnForelderLangAvstandTilSøker> = emptyList(),
    behandling: Behandling = behandling(),
) = HovedregelMetadata(
    sivilstandSøknad = sivilstand,
    sivilstandstype = sivilstandstype,
    erMigrering = erMigrering,
    barn = barn,
    søktOmBarnetilsyn = søktOmBarnetilsyn,
    langAvstandTilSøker = langAvstandTilSøker,
    vilkårgrunnlagDto = mockVilkårGrunnlagDto(),
    behandling = behandling,
)

fun sivilstand(
    type: Sivilstandstype,
    gyldigFraOgMed: LocalDate = LocalDate.now(),
    metadata: Metadata = metadataGjeldende,
) = SivilstandMedNavn(
    type = type,
    gyldigFraOgMed = gyldigFraOgMed,
    relatertVedSivilstand = null,
    bekreftelsesdato = null,
    dødsfall = null,
    navn = null,
    metadata = metadata,
)

fun søker(sivilstand: List<SivilstandMedNavn> = emptyList()): Søker =
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
        sivilstand = sivilstand,
        listOf(),
        listOf(),
        listOf(),
        listOf(),
        listOf(),
    )

/**
 * Oppretter alle vilkårsvurderiger med alle delvilkår - både gjeldende og historiske
 * */
fun opprettAlleVilkårsvurderinger(
    behandlingId: UUID,
    metadata: HovedregelMetadata,
    stønadstype: StønadType,
): List<Vilkårsvurdering> =
    vilkårsreglerForStønad(stønadstype).flatMap { vilkårsregel ->
        if (vilkårsregel.vilkårType.gjelderFlereBarn() && metadata.barn.isNotEmpty()) {
            metadata.barn.map { lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId, it.id) }
        } else {
            listOf(lagNyVilkårsvurdering(vilkårsregel, metadata, behandlingId))
        }
    }

fun inntektsmåneder(
    fraOgMedMåned: YearMonth,
    tilOgMedMåned: YearMonth = YearMonth.now(),
    inntektListe: List<Inntekt> = emptyList(),
): List<Inntektsmåned> =
    generateSequence(fraOgMedMåned) { it.plusMonths(1) }
        .takeWhile { it.isBefore(tilOgMedMåned) }
        .map { inntektsmåned(it, inntektListe) }
        .toList()

fun inntektsmåneder(
    fraOgMedMåned: YearMonth,
    tilOgMedMåned: YearMonth = YearMonth.now(),
    inntektListe: List<Inntekt> = emptyList(),
    inntektsøkning: Månedsperiode,
): List<Inntektsmåned> =
    generateSequence(fraOgMedMåned) { it.plusMonths(1) }
        .takeWhile { it.isBefore(tilOgMedMåned) }
        .map {
            if (inntektsøkning.inneholder(it)) {
                inntektsmåned(it, inntektListe, true)
            } else {
                inntektsmåned(it, inntektListe)
            }
        }.toList()

fun lagInntektResponseFraMånedsinntekterFraDouble(månedsinntekter: List<Double>): InntektResponse =
    InntektResponse(
        månedsinntekter
            .reversed()
            .mapIndexed { index, inntektsbeløp ->
                val dato = YearMonth.now().minusMonths(index.toLong() + 1)
                inntektsmåned(dato, listOf(inntekt(inntektsbeløp)))
            },
    )

fun lagInntektResponseFraMånedsinntekter(månedsinntekter: List<Int>): InntektResponse = lagInntektResponseFraMånedsinntekterFraDouble(månedsinntekter.map { it.toDouble() })

fun inntektsmåned(
    måned: YearMonth,
    inntektListe: List<Inntekt> = emptyList(),
    øktInntekt: Boolean = false,
) = Inntektsmåned(
    måned = måned,
    opplysningspliktig = "",
    underenhet = "",
    norskident = "",
    oppsummeringstidspunkt = OffsetDateTime.now(),
    inntektListe =
        if (øktInntekt) {
            inntektListe.map { it.copy(beløp = it.beløp * 1.15) }
        } else {
            inntektListe
        },
    forskuddstrekkListe = emptyList(),
    avvikListe = emptyList(),
)

fun inntekt(
    beløp: Double,
    inntektType: InntektType = InntektType.LØNNSINNTEKT,
    beskrivelse: String = "fastloenn",
) = Inntekt(
    type = inntektType,
    beløp = beløp,
    fordel = "",
    beskrivelse = beskrivelse,
    inngårIGrunnlagForTrekk = true,
    utløserArbeidsgiveravgift = true,
    skatteOgAvgiftsregel = null,
    opptjeningsperiodeFom = null,
    opptjeningsperiodeTom = null,
    tilleggsinformasjon = null,
    manuellVurdering = true,
    antall = null,
    skattemessigBosattLand = null,
    opptjeningsland = null,
)

fun saksbehandler(enhetsnavn: String = "NAV ARBEID OG YTELSER SKIEN") =
    Saksbehandler(
        azureId = UUID.randomUUID(),
        navIdent = "NAV123",
        fornavn = "Darth",
        etternavn = "Vader",
        enhet = "4489",
        enhetsnavn = enhetsnavn,
    )

private fun lagNyVilkårsvurdering(
    vilkårsregel: Vilkårsregel,
    metadata: HovedregelMetadata,
    behandlingId: UUID,
    barnId: UUID? = null,
): Vilkårsvurdering {
    val delvilkårsvurdering = initierDelvilkårsvurderinger(vilkårsregel, metadata, barnId)
    return Vilkårsvurdering(
        behandlingId = behandlingId,
        type = vilkårsregel.vilkårType,
        barnId = barnId,
        delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering),
        resultat = utledResultat(vilkårsregel, delvilkårsvurdering.map { it.tilDto() }).vilkår,
        opphavsvilkår = null,
    )
}

private fun initierDelvilkårsvurderinger(
    vilkårsregel: Vilkårsregel,
    metadata: HovedregelMetadata,
    barnId: UUID? = null,
): List<Delvilkårsvurdering> =
    when (vilkårsregel.vilkårType) {
        VilkårType.ALENEOMSORG -> initierDelvilkårsvurderingForHovedregler(vilkårsregel)
        else -> vilkårsregel.initiereDelvilkårsvurdering(metadata, barnId = barnId)
    }

private fun initierDelvilkårsvurderingForHovedregler(
    vilkårsregel: Vilkårsregel,
): List<Delvilkårsvurdering> =
    vilkårsregel.hovedregler.map {
        Delvilkårsvurdering(
            Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger = listOf(Vurdering(it)),
        )
    }
