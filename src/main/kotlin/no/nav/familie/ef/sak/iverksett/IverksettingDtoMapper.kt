package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.tilVedtaksresultat
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.AdressebeskyttelseGradering
import no.nav.familie.kontrakter.ef.iverksett.AktivitetType
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.BarnDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
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
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.RegelId as RegelIdIverksett
import no.nav.familie.kontrakter.ef.felles.VilkårType as VilkårTypeIverksett
import no.nav.familie.kontrakter.ef.felles.Vilkårsresultat as VilkårsresultatIverksett
import no.nav.familie.kontrakter.ef.iverksett.SvarId as SvarIdIverksett
import no.nav.familie.kontrakter.felles.tilbakekreving.Tilbakekrevingsvalg as TilbakekrevingsvalgKontrakter

@Component
class IverksettingDtoMapper(private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                            private val søknadService: SøknadService,
                            private val vedtakService: VedtakService,
                            private val behandlinghistorikkService: BehandlingshistorikkService,
                            private val tilkjentYtelseService: TilkjentYtelseService,
                            private val fagsakService: FagsakService,
                            private val simuleringService: SimuleringService,
                            private val tilbakekrevingService: TilbakekrevingService,
                            private val grunnlagsdataService: GrunnlagsdataService) {

    fun tilDto(behandling: Behandling, beslutter: String): IverksettDto {

        val fagsak = fagsakService.hentFagsakForBehandling(behandling.id)
        val vedtak = vedtakService.hentVedtak(behandling.id)
        val saksbehandler =
                behandlinghistorikkService.finnSisteBehandlingshistorikk(behandling.id, StegType.SEND_TIL_BESLUTTER)?.opprettetAv
                ?: error("Kan ikke finne saksbehandler på behandlingen")
        val tilkjentYtelse =
                if (vedtak.resultatType != ResultatType.AVSLÅ) tilkjentYtelseService.hentForBehandling(behandling.id) else null
        val vilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandling.id)

        val behandlingsdetaljer = mapBehandlingsdetaljer(behandling, vilkårsvurderinger)
        val fagsakdetaljerDto = mapFagsakdetaljer(fagsak)
        val søkerDto = mapSøkerDto(fagsak, behandling)
        val tilbakekreving = mapTilbakekreving(behandling.id)
        val vedtakDto = mapVedtaksdetaljerDto(vedtak, saksbehandler, beslutter, tilkjentYtelse, tilbakekreving)

        return IverksettDto(behandling = behandlingsdetaljer,
                            fagsak = fagsakdetaljerDto,
                            søker = søkerDto,
                            vedtak = vedtakDto)
    }

    private fun mapTilbakekreving(behandlingId: UUID): TilbakekrevingDto? {
        val tilbakekreving = tilbakekrevingService.hentTilbakekreving(behandlingId)
        return tilbakekreving?.let {
            TilbakekrevingDto(tilbakekrevingsvalg = mapTilbakekrevingsvalg(it.valg),
                              tilbakekrevingMedVarsel = mapTilbakekrevingMedVarsel(it, behandlingId))
        }
    }

    private fun mapTilbakekrevingMedVarsel(tilbakekreving: Tilbakekreving, behandlingId: UUID): TilbakekrevingMedVarselDto? {
        if (tilbakekreving.valg == Tilbakekrevingsvalg.OPPRETT_MED_VARSEL) {
            val lagretSimuleringsresultat = simuleringService.hentLagretSimuleringsresultat(behandlingId)
            val perioder = lagretSimuleringsresultat.hentSammenhengendePerioderMedFeilutbetaling()
                    .map { Periode(fom = it.fom, tom = it.tom) }
            TilbakekrevingMedVarselDto(varseltekst = tilbakekreving.varseltekst ?: "",
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


    private fun mapFagsakdetaljer(fagsak: Fagsak) = FagsakdetaljerDto(fagsakId = fagsak.id,
                                                                      eksternId = fagsak.eksternId.id,
                                                                      stønadstype = StønadType.OVERGANGSSTØNAD)

    @Improvement("Årsak og Type må utledes når vi støtter revurdering")
    private fun mapBehandlingsdetaljer(behandling: Behandling,
                                       vilkårsvurderinger: List<Vilkårsvurdering>) =
            BehandlingsdetaljerDto(behandlingId = behandling.id,
                                   behandlingType = BehandlingType.valueOf(behandling.type.name),
                                   behandlingÅrsak = behandling.årsak ?: BehandlingÅrsak.SØKNAD,
                                   eksternId = behandling.eksternId.id,
                                   vilkårsvurderinger = vilkårsvurderinger.map { it.tilIverksettDto() },
                                   forrigeBehandlingId = behandling.forrigeBehandlingId,
                                   kravMottatt = behandling.kravMottatt,
            )

    @Improvement("Opphørårsak må utledes ved revurdering")
    private fun mapVedtaksdetaljerDto(vedtak: Vedtak,
                                      saksbehandler: String,
                                      beslutter: String,
                                      tilkjentYtelse: TilkjentYtelse?,
                                      tilbakekreving: TilbakekrevingDto?) =
            VedtaksdetaljerDto(resultat = vedtak.resultatType.tilVedtaksresultat(),
                               vedtaksdato = LocalDate.now(),
                               opphørÅrsak = null,
                               saksbehandlerId = saksbehandler,
                               beslutterId = beslutter,
                               tilkjentYtelse = tilkjentYtelse?.tilIverksettDto(),
                               vedtaksperioder = vedtak.perioder?.tilIverksettDto() ?: emptyList(),
                               tilbakekreving = tilbakekreving
            )

    private fun mapSøkerDto(fagsak: Fagsak, behandling: Behandling): SøkerDto {
        val søknad = søknadService.hentOvergangsstønad(behandling.id)
        val (grunnlagsdata) = grunnlagsdataService.hentGrunnlagsdata(behandling.id)
        val alleBarn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, grunnlagsdata.barn)
        val navEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(søknad.fødselsnummer)

        return SøkerDto(fagsak.hentAktivIdent(),
                        alleBarn.map {
                            BarnDto(personIdent = it.fødselsnummer,
                                    termindato = it.søknadsbarn.fødselTermindato)
                        },
                        navEnhet,
                        grunnlagsdata.søker.adressebeskyttelse?.let { AdressebeskyttelseGradering.valueOf(it.gradering.name) })
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
        }
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

fun PeriodeWrapper.tilIverksettDto(): List<VedtaksperiodeDto> = this.perioder.map {
    VedtaksperiodeDto(fraOgMed = it.datoFra,
                      tilOgMed = it.datoTil,
                      aktivitet = AktivitetType.valueOf(it.aktivitet.name),
                      periodeType = VedtaksperiodeType.valueOf(it.periodeType.name)
    )
}

fun Simuleringsoppsummering.hentSammenhengendePerioderMedFeilutbetaling(): List<Periode> {
    val perioderMedFeilutbetaling =
            perioder.sortedBy { it.fom }.filter { it.feilutbetaling > BigDecimal(0) }.map {
                Periode(it.fom, it.tom)
            }

    return perioderMedFeilutbetaling.fold(mutableListOf()) { akkumulatorListe, nestePeriode ->
        val gjeldendePeriode = akkumulatorListe.lastOrNull()

        if (gjeldendePeriode != null && erPerioderSammenhengende(gjeldendePeriode, nestePeriode)) {
            val oppdatertGjeldendePeriode = Periode(fom = gjeldendePeriode.fom,tom=nestePeriode.tom)
            akkumulatorListe.removeLast()
            akkumulatorListe.add(oppdatertGjeldendePeriode)
        } else {
            akkumulatorListe.add(nestePeriode)
        }
        akkumulatorListe
    }
}

private fun erPerioderSammenhengende(gjeldendePeriode: Periode, nestePeriode: Periode) =
        gjeldendePeriode.tom.plusDays(1) == nestePeriode.fom