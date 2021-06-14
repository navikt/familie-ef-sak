package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.tilVedtaksresultat
import no.nav.familie.ef.sak.repository.BehandlingRepository
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Delvilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.PeriodeWrapper
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.domain.Vilkårsvurdering
import no.nav.familie.ef.sak.repository.domain.Vurdering
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.GrunnlagsdataService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.service.steg.StegType
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
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.iverksett.VilkårsvurderingDto
import no.nav.familie.kontrakter.ef.iverksett.VurderingDto
import no.nav.familie.kontrakter.felles.annotasjoner.Improvement
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.RegelId as RegelIdIverksett
import no.nav.familie.kontrakter.ef.felles.VilkårType as VilkårTypeIverksett
import no.nav.familie.kontrakter.ef.felles.Vilkårsresultat as VilkårsresultatIverksett
import no.nav.familie.kontrakter.ef.iverksett.SvarId as SvarIdIverksett

@Component
class IverksettingDtoMapper(private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                            private val behandlingRepository: BehandlingRepository,
                            private val søknadService: SøknadService,
                            private val vedtakService: VedtakService,
                            private val behandlinghistorikkService: BehandlingshistorikkService,
                            private val tilkjentYtelseService: TilkjentYtelseService,
                            private val fagsakService: FagsakService,
                            private val grunnlagsdataService: GrunnlagsdataService) {

    fun tilDto(behandling: Behandling, beslutter: String): IverksettDto {

        val fagsak = fagsakService.hentFaksakForBehandling(behandling.id)
        val vedtak = vedtakService.hentVedtak(behandling.id)
        val forrigeBehandlingId = behandlingRepository.finnSisteIverksatteBehandling(behandling.id)
        val saksbehandler =
                behandlinghistorikkService.finnSisteBehandlingshistorikk(behandling.id, StegType.SEND_TIL_BESLUTTER)?.opprettetAv
                ?: error("Kan ikke finne saksbehandler på behandlingen")
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)
        val vilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandling.id)


        val behandlingsdetaljer = mapBehandlingsdetaljer(behandling, vilkårsvurderinger, forrigeBehandlingId)
        val fagsakdetaljerDto = mapFagsakdetaljer(fagsak)
        val søkerDto = mapSøkerDto(fagsak, behandling)
        val vedtakDto = mapVedtaksdetaljerDto(vedtak, saksbehandler, beslutter, tilkjentYtelse)


        return IverksettDto(behandling = behandlingsdetaljer,
                            fagsak = fagsakdetaljerDto,
                            søker = søkerDto,
                            vedtak = vedtakDto)
    }

    private fun mapFagsakdetaljer(fagsak: Fagsak) = FagsakdetaljerDto(fagsakId = fagsak.id,
                                                                      eksternId = fagsak.eksternId.id,
                                                                      stønadstype = StønadType.OVERGANGSSTØNAD)

    @Improvement("Årsak og Type må utledes når vi støtter revurdering")
    private fun mapBehandlingsdetaljer(behandling: Behandling,
                                       vilkårsvurderinger: List<Vilkårsvurdering>,
                                       forrigeBehandlingId: UUID?) =
            BehandlingsdetaljerDto(behandlingId = behandling.id,
                                   behandlingType = BehandlingType.valueOf(behandling.type.name),
                                   behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                                   eksternId = behandling.eksternId.id,
                                   vilkårsvurderinger = vilkårsvurderinger.map { it.tilIverksettDto() },
                                   forrigeBehandlingId = forrigeBehandlingId
            )

    @Improvement("Opphørårsak må utledes ved revurdering")
    private fun mapVedtaksdetaljerDto(vedtak: Vedtak,
                                      saksbehandler: String,
                                      beslutter: String,
                                      tilkjentYtelse: TilkjentYtelse) =
            VedtaksdetaljerDto(resultat = vedtak.resultatType.tilVedtaksresultat(),
                               vedtaksdato = LocalDate.now(),
                               opphørÅrsak = null,
                               saksbehandlerId = saksbehandler,
                               beslutterId = beslutter,
                               tilkjentYtelse = tilkjentYtelse.tilIverksettDto(),
                               vedtaksperioder = vedtak.perioder?.tilIverksettDto() ?: emptyList()
            )

    private fun mapSøkerDto(fagsak: Fagsak, behandling: Behandling): SøkerDto {
        val søknad = søknadService.hentOvergangsstønad(behandling.id)
        val (grunnlagsdata) = grunnlagsdataService.hentGrunnlagsdata(behandling.id)
        val alleBarn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, grunnlagsdata.barn)
        val navEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(søknad.fødselsnummer)

        return SøkerDto(adressebeskyttelse = grunnlagsdata.søker.adressebeskyttelse?.let { AdressebeskyttelseGradering.valueOf(it.gradering.name) },
                        personIdent = fagsak.hentAktivIdent(),
                        barn = alleBarn.map {
                            BarnDto(personIdent = it.fødselsnummer,
                                    termindato = it.søknadsbarn.fødselTermindato)
                        },
                        tilhørendeEnhet = navEnhet
        )
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
