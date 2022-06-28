package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereInnvilgetVedtak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioder
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.TidligereVedtaksperioderAnnenForelder
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
) {

    // TODO endre om til å bruke identer fra pdlSøker, då dette blir et ekstra kall for å hente identer
    fun hentTidligereVedtaksperioder(ident: String): TidligereVedtaksperioder {
        val tidligereInnvilgetVedtak = mapTidligereInnvilgetVedtak(infotrygdService.hentPerioderFraReplika(ident))
        return TidligereVedtaksperioder(infotrygd = tidligereInnvilgetVedtak)
    }

    fun hentTidligereVedaksperioderAnnenForelder(identer: Set<String>): TidligereVedtaksperioderAnnenForelder {
        val infotrygd = mapTidligereInnvilgetVedtak(infotrygdService.hentPerioderFraReplika(identer))
        return TidligereVedtaksperioderAnnenForelder(
            infotrygd = infotrygd,
            efSak = harTidligereMottattStønadEf(identer)
        )
    }

    private fun mapTidligereInnvilgetVedtak(periodeResponse: InfotrygdPeriodeResponse) =
        TidligereInnvilgetVedtak(
            harTidligereOvergangsstønad = periodeResponse.overgangsstønad.isNotEmpty(),
            harTidligereBarnetilsyn = periodeResponse.barnetilsyn.isNotEmpty(),
            harTidligereSkolepenger = periodeResponse.skolepenger.isNotEmpty(),
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
            } ?: TidligereInnvilgetVedtak()
    }

    private fun hentTidligereVedtaksperioder(fagsak: Fagsak?) = fagsak
        ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
        ?.let {
            val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(it.id)
            tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty()
        } ?: false
}
