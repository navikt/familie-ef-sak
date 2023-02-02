package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisteridentifikator
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import org.springframework.stereotype.Service

@Service
class TidligereVedaksperioderService(
    private val fagsakPersonService: FagsakPersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val infotrygdService: InfotrygdService,
    private val historiskPensjonService: HistoriskPensjonService
) {

    /**
     * @param folkeregisteridentifikatorer for 1 person
     */
    fun hentTidligereVedtaksperioder(folkeregisteridentifikatorer: List<Folkeregisteridentifikator>): TidligereVedtaksperioder {
        val aktivIdent = folkeregisteridentifikatorer.gjeldende().ident
        val alleIdenter = folkeregisteridentifikatorer.map { it.ident }.toSet()
        val tidligereInnvilgetVedtak =
            mapTidligereInnvilgetVedtak(infotrygdService.hentPerioderFraReplika(alleIdenter))
        return TidligereVedtaksperioder(
            infotrygd = tidligereInnvilgetVedtak,
            sak = harTidligereMottattStønadEf(alleIdenter),
            infotrygdPePp = historiskPensjonService.hentHistoriskPensjon(aktivIdent, alleIdenter).harPensjonsdata
        )
    }

    private fun mapTidligereInnvilgetVedtak(periodeResponse: InfotrygdPeriodeResponse) =
        TidligereInnvilgetVedtak(
            harTidligereOvergangsstønad = periodeResponse.overgangsstønad.isNotEmpty(),
            harTidligereBarnetilsyn = periodeResponse.barnetilsyn.isNotEmpty(),
            harTidligereSkolepenger = periodeResponse.skolepenger.isNotEmpty()
        )

    private fun harTidligereMottattStønadEf(identer: Set<String>): TidligereInnvilgetVedtak {
        return fagsakPersonService.finnPerson(identer)
            ?.let { fagsakService.finnFagsakerForFagsakPersonId(it.id) }
            ?.let {
                TidligereInnvilgetVedtak(
                    harTidligereOvergangsstønad = hentTidligereVedtaksperioder(it.overgangsstønad),
                    harTidligereBarnetilsyn = hentTidligereVedtaksperioder(it.barnetilsyn),
                    harTidligereSkolepenger = hentTidligereVedtaksperioder(it.skolepenger)
                )
            } ?: TidligereInnvilgetVedtak(false, false, false)
    }

    private fun hentTidligereVedtaksperioder(fagsak: Fagsak?) = fagsak
        ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
        ?.let {
            val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(it.id)
            tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty()
        } ?: false
}
