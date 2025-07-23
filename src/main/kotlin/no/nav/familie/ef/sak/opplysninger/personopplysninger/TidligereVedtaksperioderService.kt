package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.Fagsaker
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.EndringType
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import org.springframework.stereotype.Service

@Service
class TidligereVedtaksperioderService(
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val infotrygdService: InfotrygdService,
    private val historiskPensjonService: HistoriskPensjonService,
    private val andelsHistorikkService: AndelsHistorikkService,
) {
    /**
     * @param folkeregisteridentifikatorer for 1 person
     */
    fun hentTidligereVedtaksperioder(folkeregisteridentifikatorer: List<Folkeregisteridentifikator>): TidligereVedtaksperioder {
        return if(folkeregisteridentifikatorer.isNotEmpty() && folkeregisteridentifikatorer.any { it.metadata.historisk == false }){
            val aktivIdent = folkeregisteridentifikatorer.gjeldende().ident
            val alleIdenter = folkeregisteridentifikatorer.map { it.ident }.toSet()
            val tidligereInnvilgetVedtak =
                mapTidligereInnvilgetVedtak(infotrygdService.hentPerioderFraReplika(alleIdenter))
            return TidligereVedtaksperioder(
                infotrygd = tidligereInnvilgetVedtak,
                sak = hentTidligereInnvilgedeVedtakEf(alleIdenter),
                historiskPensjon = historiskPensjonService.hentHistoriskPensjon(aktivIdent, alleIdenter).harPensjonsdata(),
            )
        } else{
            // Ikke hent tidligere vedtaksperioder for personer uten folkeregisteridentifikatorer (f.eks. andre foreldre med NPID hentet fra relasjon til barn)
            TidligereVedtaksperioder(infotrygd = TidligereInnvilgetVedtak(false, false, false))
        }
    }

    private fun mapTidligereInnvilgetVedtak(periodeResponse: InfotrygdPeriodeResponse) =
        TidligereInnvilgetVedtak(
            harTidligereOvergangsstønad = periodeResponse.overgangsstønad.isNotEmpty(),
            harTidligereBarnetilsyn = periodeResponse.barnetilsyn.isNotEmpty(),
            harTidligereSkolepenger = periodeResponse.skolepenger.isNotEmpty(),
        )

    private fun hentTidligereInnvilgedeVedtakEf(identer: Set<String>): TidligereInnvilgetVedtak =
        fagsakPersonService
            .finnPerson(identer)
            ?.let { fagsakService.finnFagsakerForFagsakPersonId(it.id) }
            ?.let {
                TidligereInnvilgetVedtak(
                    harTidligereOvergangsstønad = hentTidligereVedtaksperioder(it.overgangsstønad),
                    harTidligereBarnetilsyn = hentTidligereVedtaksperioder(it.barnetilsyn),
                    harTidligereSkolepenger = hentTidligereVedtaksperioder(it.skolepenger),
                    periodeHistorikkOvergangsstønad = hentGjeldendeOvergangstønadsperioder(it),
                    periodeHistorikkBarnetilsyn = hentGjeldendeBarnetilsynsperioder(it),
                )
            } ?: TidligereInnvilgetVedtak(false, false, false)

    private fun hentTidligereVedtaksperioder(fagsak: Fagsak?) =
        fagsak
            ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
            ?.let {
                val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(it.id)
                tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty()
            } ?: false

    private fun hentGjeldendeOvergangstønadsperioder(fagsaker: Fagsaker?): List<GrunnlagsdataPeriodeHistorikkOvergangsstønad> =
        hentAndelshistorikkForOvergangsstønad(fagsaker)
            .filterNot(erstattetEllerFjernet())
            .filterNot { it.erOpphør }
            .map {
                feilHvis(it.periodeType == null) { "Overgangsstønad skal ha periodetype" }
                GrunnlagsdataPeriodeHistorikkOvergangsstønad(
                    periodeType = it.periodeType,
                    fom = it.andel.periode.fomDato,
                    tom = it.andel.periode.tomDato,
                    aktivitet = it.aktivitet,
                    beløp = it.andel.beløp,
                    inntekt = it.andel.inntekt,
                    samordningsfradrag = it.andel.samordningsfradrag,
                )
            }

    private fun hentGjeldendeBarnetilsynsperioder(fagsaker: Fagsaker?): List<GrunnlagsdataPeriodeHistorikkBarnetilsyn> =
        hentAndelshistorikkForBarnetilsyn(fagsaker)
            .filterNot(erstattetEllerFjernet())
            .filterNot { it.erOpphør }
            .map {
                GrunnlagsdataPeriodeHistorikkBarnetilsyn(
                    fom = it.andel.periode.fomDato,
                    tom = it.andel.periode.tomDato,
                )
            }

    private fun hentAndelshistorikkForOvergangsstønad(fagsaker: Fagsaker?) = fagsaker?.overgangsstønad?.id?.let { andelsHistorikkService.hentHistorikk(it, null) } ?: emptyList()

    private fun hentAndelshistorikkForBarnetilsyn(fagsaker: Fagsaker?) = fagsaker?.barnetilsyn?.id?.let { andelsHistorikkService.hentHistorikk(it, null) } ?: emptyList()

    private fun erstattetEllerFjernet(): (AndelHistorikkDto) -> Boolean =
        {
            listOf(
                EndringType.FJERNET,
                EndringType.ERSTATTET,
            ).contains(it.endring?.type)
        }
}
