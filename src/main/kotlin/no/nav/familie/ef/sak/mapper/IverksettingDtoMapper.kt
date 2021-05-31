package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.iverksett.infrastruktur.json.BarnDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.tilVedtaksresultat
import no.nav.familie.ef.sak.integration.dto.pdl.AdressebeskyttelseGradering
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.repository.VilkårsvurderingRepository
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.repository.domain.VilkårType
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.ef.sak.service.ArbeidsfordelingService
import no.nav.familie.ef.sak.service.BehandlingshistorikkService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.PersisterGrunnlagsdataService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.kontrakter.ef.felles.BehandlingResultat
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.AktivitetskravDto
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.FagsakdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.InntektDto
import no.nav.familie.kontrakter.ef.iverksett.InntektsType
import no.nav.familie.kontrakter.ef.iverksett.IverksettDto
import no.nav.familie.kontrakter.ef.iverksett.PeriodebeløpDto
import no.nav.familie.kontrakter.ef.iverksett.Periodetype
import no.nav.familie.kontrakter.ef.iverksett.SøkerDto
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerDto
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class IverksettingDtoMapper(private val arbeidsfordelingService: ArbeidsfordelingService,
                            private val vilkårsvurderingRepository: VilkårsvurderingRepository,
                            private val søknadService: SøknadService,
                            private val vedtakService: VedtakService,
                            private val behandlinghistorikkService: BehandlingshistorikkService,
                            private val tilkjentYtelseService: TilkjentYtelseService,
                            private val fagsakService: FagsakService,
                            private val persisterGrunnlagsdataService: PersisterGrunnlagsdataService) {

    fun tilDto(behandling: Behandling, beslutter: String): IverksettDto {

        //TODO Hardkodet tillsvidare verdier for behandlingResultat og behandlingÅrsak
        val behandlingsdetaljer = BehandlingsdetaljerDto(behandlingId = behandling.id,
                                                         behandlingType = BehandlingType.valueOf(behandling.type.name),
                                                         behandlingResultat = BehandlingResultat.FERDIGSTILT,
                                                         behandlingÅrsak = BehandlingÅrsak.SØKNAD,
                                                         eksternId = behandling.eksternId.id)

        val fagsak = fagsakService.hentFaksakForBehandling(behandling.id)
        val fagsakdetaljerDto = FagsakdetaljerDto(fagsakId = fagsak.id,
                                                  eksternId = fagsak.eksternId.id,
                                                  stønadstype = StønadType.OVERGANGSSTØNAD)


        val søkerDto = mapSøkerDto(fagsak, behandling)

        val vedtak = vedtakService.hentVedtak(behandling.id)
        val saksbehandler =
                behandlinghistorikkService.finnSisteBehandlingshistorikk(behandling.id, StegType.SEND_TIL_BESLUTTER)?.opprettetAv
                ?: error("Kan ikke finne saksbehandler på behandlingen")
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)
        val vedtakDto = mapVedtaksdetaljerDto(vedtak, saksbehandler, beslutter, tilkjentYtelse)

        return IverksettDto(behandling = behandlingsdetaljer, fagsak = fagsakdetaljerDto, søker = søkerDto, vedtak = vedtakDto)
    }

    private fun mapVedtaksdetaljerDto(vedtak: Vedtak,
                                      saksbehandler: String,
                                      beslutter: String,
                                      tilkjentYtelse: TilkjentYtelse) =
            VedtaksdetaljerDto(resultat = vedtak.resultatType.tilVedtaksresultat(),
                               vedtaksdato = LocalDate.now(), // TODO: Er dette når første saksbehandler fullfører eller når beslutter godkjenner
                               opphørÅrsak = null, // TODO: Revurdering
                               saksbehandlerId = saksbehandler,
                               beslutterId = beslutter,
                               tilkjentYtelse = TilkjentYtelseDto(
                                       andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse.map { andel ->
                                           AndelTilkjentYtelseDto(periodebeløp = PeriodebeløpDto(beløp = andel.beløp,
                                                                                                 periodetype = Periodetype.MÅNED,
                                                                                                 fraOgMed = andel.stønadFom,
                                                                                                 tilOgMed = andel.stønadTom),
                                                                  kildeBehandlingId = andel.kildeBehandlingId)
                                       }

                               ),
                               inntekter = vedtak.inntekter?.inntekter?.map {
                                   InntektDto(periodebeløp = PeriodebeløpDto(beløp = it.inntekt.intValueExact(),
                                                                             periodetype = Periodetype.MÅNED,
                                                                             fraOgMed = it.startDato,
                                                                             tilOgMed = it.sluttDato),
                                              inntektstype = InntektsType.ARBEIDINNTEKT) // TODO: Hva er inntektstype?
                               } ?: emptyList()
            )

    private fun mapSøkerDto(fagsak: Fagsak, behandling: Behandling): SøkerDto {
        val søknad = søknadService.hentOvergangsstønad(behandling.id)
        val grunnlagsdata = persisterGrunnlagsdataService.hentGrunnlagsdata(behandling.id, søknad)
        val alleBarn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn, grunnlagsdata.barn)
        val navEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(søknad.fødselsnummer)
        val harSagtOpp = hentHarSagtOppEllerRedusertFraVurderinger(behandling)

        return SøkerDto(kode6eller7 = grunnlagsdata.søker.adressebeskyttelse?.let {
            listOf(AdressebeskyttelseGradering.STRENGT_FORTROLIG,
                   AdressebeskyttelseGradering.FORTROLIG).contains(it.gradering)
        }
                                      ?: false,
                        personIdent = fagsak.hentAktivIdent(),
                        barn = alleBarn.map {
                            BarnDto(personIdent = it.fødselsnummer,
                                    termindato = it.søknadsbarn.fødselTermindato)
                        },
                        aktivitetskrav = AktivitetskravDto(
                                aktivitetspliktInntrefferDato = LocalDate.now(),
                                harSagtOppArbeidsforhold = harSagtOpp),
                        tilhørendeEnhet = navEnhet)
    }

    private fun hentHarSagtOppEllerRedusertFraVurderinger(behandling: Behandling): Boolean {
        return vilkårsvurderingRepository.findByBehandlingId(behandling.id)
                       .find { it.type === VilkårType.SAGT_OPP_ELLER_REDUSERT }
                       ?.let { vilkårsvurderingDto ->
                           if (vilkårsvurderingDto.resultat == Vilkårsresultat.SKAL_IKKE_VURDERES) {
                               false //TODO: Hva ska resultatet vare hvis resultatet er Vilkårsresultat.SKAL_IKKE_VURDERES
                           } else {
                               vilkårsvurderingDto.delvilkårsvurdering.delvilkårsvurderinger
                                       .any { delvilkår ->
                                           val vurdering =
                                                   delvilkår.vurderinger.find { vurdering -> vurdering.regelId == RegelId.SAGT_OPP_ELLER_REDUSERT }
                                           vurdering?.svar?.let {
                                               when (it) {
                                                   SvarId.JA -> true
                                                   SvarId.NEI -> false
                                                   else -> error("Jajaj")

                                               }
                                           } ?: error("detta var inte så bra")
                                       }
                           }
                       } ?: error("detta var inte så bra")
    }
}