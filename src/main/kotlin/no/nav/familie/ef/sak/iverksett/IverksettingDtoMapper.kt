package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.brev.BrevmottakereRepository
import no.nav.familie.ef.sak.brev.domain.MottakerRolle
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.hentSammenhengendePerioderMedFeilutbetaling
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.tilVedtaksresultat
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.iverksett.AdressebeskyttelseGradering
import no.nav.familie.kontrakter.ef.iverksett.AktivitetType
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.BarnDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.ef.iverksett.DelvilkårsvurderingDto
import no.nav.familie.kontrakter.ef.iverksett.FagsakdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.Periodetype
import no.nav.familie.kontrakter.ef.iverksett.SøkerDto
import no.nav.familie.kontrakter.ef.iverksett.TilbakekrevingDto
import no.nav.familie.kontrakter.ef.iverksett.TilbakekrevingMedVarselDto
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.iverksett.VilkårsvurderingDto
import no.nav.familie.kontrakter.ef.iverksett.VurderingDto
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.RegelId as RegelIdIverksett
import no.nav.familie.kontrakter.ef.felles.VilkårType as VilkårTypeIverksett
import no.nav.familie.kontrakter.ef.felles.Vilkårsresultat as VilkårsresultatIverksett
import no.nav.familie.kontrakter.ef.iverksett.SvarId as SvarIdIverksett
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg as TilbakekrevingsvalgKontrakter

@Component
class IverksettingDtoMapper(private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                            private val vedtakService: VedtakService,
                            private val barnService: BarnService,
                            private val behandlingshistorikkService: BehandlingshistorikkService,
                            private val tilkjentYtelseService: TilkjentYtelseService,
                            private val fagsakService: FagsakService,
                            private val simuleringService: SimuleringService,
                            private val tilbakekrevingService: TilbakekrevingService,
                            private val grunnlagsdataService: GrunnlagsdataService,
                            private val brevmottakereRepository: BrevmottakereRepository) {

    fun tilDto(saksbehandling: Saksbehandling, beslutter: String): IverksettDto {

        val saksbehandler =
                behandlingshistorikkService.finnSisteBehandlingshistorikk(saksbehandling.id,
                                                                          StegType.SEND_TIL_BESLUTTER)?.opprettetAv
                ?: error("Kan ikke finne saksbehandler på behandlingen")
        return tilDto(saksbehandling, saksbehandler, beslutter)
    }

    fun tilMigreringDto(saksbehandling: Saksbehandling): IverksettDto {
        return tilDto(saksbehandling, SikkerhetContext.SYSTEM_FORKORTELSE, SikkerhetContext.SYSTEM_FORKORTELSE)
    }

    private fun tilDto(saksbehandling: Saksbehandling,
                       saksbehandler: String,
                       beslutter: String): IverksettDto {
        val vedtak = vedtakService.hentVedtak(saksbehandling.id)
        val tilkjentYtelse =
                if (vedtak.resultatType != ResultatType.AVSLÅ) tilkjentYtelseService.hentForBehandling(saksbehandling.id) else null
        val vilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(saksbehandling.id)

        val behandlingsdetaljer = mapBehandlingsdetaljer(saksbehandling, vilkårsvurderinger)
        val fagsakdetaljerDto = mapFagsakdetaljer(saksbehandling)
        val søkerDto = mapSøkerDto(saksbehandling)
        val tilbakekreving = mapTilbakekreving(saksbehandling.id)
        val brevmottakere = mapBrevmottakere(saksbehandling.id)
        val vedtakDto = mapVedtaksdetaljerDto(vedtak, saksbehandler, beslutter, tilkjentYtelse, tilbakekreving, brevmottakere)

        return IverksettDto(behandling = behandlingsdetaljer,
                            fagsak = fagsakdetaljerDto,
                            søker = søkerDto,
                            vedtak = vedtakDto)
    }

    fun mapTilbakekreving(behandlingId: UUID): TilbakekrevingDto? {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        return tilbakekreving?.let {
            TilbakekrevingDto(tilbakekrevingsvalg = mapTilbakekrevingsvalg(it.valg),
                              tilbakekrevingMedVarsel = mapTilbakekrevingMedVarsel(it, behandlingId))
        }
    }

    private fun mapTilbakekrevingMedVarsel(tilbakekreving: Tilbakekreving, behandlingId: UUID): TilbakekrevingMedVarselDto? {
        if (tilbakekreving.valg == Tilbakekrevingsvalg.OPPRETT_MED_VARSEL) {
            val lagretSimuleringsresultat = simuleringService.hentLagretSimuleringsoppsummering(behandlingId)
            val perioder = lagretSimuleringsresultat.hentSammenhengendePerioderMedFeilutbetaling()
                    .map { Periode(fom = it.fom, tom = it.tom) }
            return TilbakekrevingMedVarselDto(varseltekst = tilbakekreving.varseltekst ?: "",
                                              sumFeilutbetaling = lagretSimuleringsresultat.feilutbetaling,
                                              perioder = perioder)
        }
        return null
    }

    private fun mapTilbakekrevingsvalg(valg: Tilbakekrevingsvalg): TilbakekrevingsvalgKontrakter =
            when (valg) {
                Tilbakekrevingsvalg.AVVENT -> TilbakekrevingsvalgKontrakter.IGNORER_TILBAKEKREVING
                Tilbakekrevingsvalg.OPPRETT_MED_VARSEL -> TilbakekrevingsvalgKontrakter.OPPRETT_TILBAKEKREVING_MED_VARSEL
                Tilbakekrevingsvalg.OPPRETT_UTEN_VARSEL -> TilbakekrevingsvalgKontrakter.OPPRETT_TILBAKEKREVING_UTEN_VARSEL
            }


    private fun mapFagsakdetaljer(saksbehandling: Saksbehandling) =
            FagsakdetaljerDto(fagsakId = saksbehandling.fagsakId,
                              eksternId = saksbehandling.eksternFagsakId,
                              stønadstype = StønadType.OVERGANGSSTØNAD)

    @Improvement("Årsak og Type må utledes når vi støtter revurdering")
    private fun mapBehandlingsdetaljer(saksbehandling: Saksbehandling,
                                       vilkårsvurderinger: List<Vilkårsvurdering>) =
            BehandlingsdetaljerDto(behandlingId = saksbehandling.id,
                                   behandlingType = BehandlingType.valueOf(saksbehandling.type.name),
                                   behandlingÅrsak = saksbehandling.årsak,
                                   eksternId = saksbehandling.eksternId,
                                   vilkårsvurderinger = vilkårsvurderinger.map { it.tilIverksettDto() },
                                   forrigeBehandlingId = saksbehandling.forrigeBehandlingId,
                                   kravMottatt = saksbehandling.kravMottatt)

    @Improvement("Opphørårsak må utledes ved revurdering")
    private fun mapVedtaksdetaljerDto(vedtak: Vedtak,
                                      saksbehandler: String,
                                      beslutter: String,
                                      tilkjentYtelse: TilkjentYtelse?,
                                      tilbakekreving: TilbakekrevingDto?,
                                      brevmottakere: List<Brevmottaker>) =
            VedtaksdetaljerDto(resultat = vedtak.resultatType.tilVedtaksresultat(),
                               vedtakstidspunkt = LocalDateTime.now(),
                               opphørÅrsak = null,
                               saksbehandlerId = saksbehandler,
                               beslutterId = beslutter,
                               tilkjentYtelse = tilkjentYtelse?.tilIverksettDto(),
                               vedtaksperioder = vedtak.perioder?.tilIverksettDto() ?: emptyList(),
                               tilbakekreving = tilbakekreving,
                               brevmottakere = brevmottakere)

    private fun mapSøkerDto(saksbehandling: Saksbehandling): SøkerDto {
        val personIdent = saksbehandling.ident
        val barn = barnService.finnBarnPåBehandling(saksbehandling.id)
        val (grunnlagsdata) = grunnlagsdataService.hentGrunnlagsdata(saksbehandling.id)
        val alleBarn = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(barn, grunnlagsdata.barn)
        val navEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)

        return SøkerDto(personIdent,
                        alleBarn.map {
                            BarnDto(personIdent = it.fødselsnummer,
                                    termindato = it.behandlingBarn.fødselTermindato)
                        },
                        navEnhet,
                        grunnlagsdata.søker.adressebeskyttelse?.let { AdressebeskyttelseGradering.valueOf(it.gradering.name) })
    }

    private fun mapBrevmottakere(behandlingId: UUID): List<Brevmottaker> {
        return brevmottakereRepository.findByIdOrNull(behandlingId)?.let {
            val personer = it.personer.personer.map { mottaker ->
                Brevmottaker(ident = mottaker.personIdent,
                             navn = mottaker.navn,
                             mottakerRolle = mottaker.mottakerRolle.tilIverksettDto(),
                             identType = Brevmottaker.IdentType.PERSONIDENT)
            }

            val organisasjoner = it.organisasjoner.organisasjoner.map { mottaker ->
                Brevmottaker(ident = mottaker.organisasjonsnummer,
                             navn = mottaker.navnHosOrganisasjon,
                             mottakerRolle = mottaker.mottakerRolle.tilIverksettDto(),
                             identType = Brevmottaker.IdentType.ORGANISASJONSNUMMER)
            }

            personer + organisasjoner
        } ?: emptyList()

    }


}


fun TilkjentYtelse.tilIverksettDto(): TilkjentYtelseDto = TilkjentYtelseDto(
        andelerTilkjentYtelse = andelerTilkjentYtelse.map { andel ->
            AndelTilkjentYtelseDto(beløp = andel.beløp,
                                   fraOgMed = andel.stønadFom,
                                   tilOgMed = andel.stønadTom,
                                   inntekt = andel.inntekt,
                                   samordningsfradrag = andel.samordningsfradrag,
                                   inntektsreduksjon = andel.inntektsreduksjon,
                                   kildeBehandlingId = andel.kildeBehandlingId,
                                   periodetype = Periodetype.MÅNED)
        },
        startdato = startdato
)

fun Vurdering.tilIverksettDto(): VurderingDto = VurderingDto(
        regelId = RegelIdIverksett.valueOf(this.regelId.name),
        svar = this.svar?.let { SvarIdIverksett.valueOf(it.name) },
        begrunnelse = this.begrunnelse
)

fun Delvilkårsvurdering.tilIverksettDto(): DelvilkårsvurderingDto = DelvilkårsvurderingDto(
        resultat = VilkårsresultatIverksett.valueOf(this.resultat.name),
        vurderinger = this.vurderinger.map { vurdering -> vurdering.tilIverksettDto() }

)

fun Vilkårsvurdering.tilIverksettDto(): VilkårsvurderingDto = VilkårsvurderingDto(
        vilkårType = VilkårTypeIverksett.valueOf(this.type.name),
        resultat = VilkårsresultatIverksett.valueOf(this.resultat.name),
        delvilkårsvurderinger = this.delvilkårsvurdering.delvilkårsvurderinger.map { delvilkårsvurdering ->
            delvilkårsvurdering.tilIverksettDto()
        }
)

fun PeriodeWrapper.tilIverksettDto(): List<VedtaksperiodeDto> = this.perioder
        .filter { it.periodeType != no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType.MIDLERTIDIG_OPPHØR }
        .map {
            VedtaksperiodeDto(fraOgMed = it.datoFra,
                              tilOgMed = it.datoTil,
                              aktivitet = AktivitetType.valueOf(it.aktivitet.name),
                              periodeType = VedtaksperiodeType.valueOf(it.periodeType.name)
            )
        }

fun MottakerRolle.tilIverksettDto(): Brevmottaker.MottakerRolle =
        when (this) {
            MottakerRolle.FULLMAKT -> Brevmottaker.MottakerRolle.FULLMEKTIG
            MottakerRolle.VERGE -> Brevmottaker.MottakerRolle.VERGE
            MottakerRolle.BRUKER -> Brevmottaker.MottakerRolle.BRUKER
        }

