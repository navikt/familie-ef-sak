package no.nav.familie.ef.sak.ekstern.soknad

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EksternSøknadService(
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val personService: PersonService,
    private val infotrygdService: InfotrygdService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harTidligereInnvilgetVedtak(personIdent: String): TidligereVedtakStatus =
        try {
            val personIdenter = personService.hentPersonIdenter(personIdent).identer()

            val harInnvilgetIEfSak =
                StønadType.values().any { stønadstype ->
                    fagsakService
                        .finnFagsak(personIdenter, stønadstype)
                        ?.let { behandlingService.finnSisteIverksatteBehandling(it.id) }
                        ?.let { tilkjentYtelseService.hentForBehandling(it.id) }
                        ?.andelerTilkjentYtelse
                        ?.isNotEmpty() == true
                }

            val harInnvilgetIInfotrygd = infotrygdService.eksisterer(personIdent)

            if (harInnvilgetIEfSak || harInnvilgetIInfotrygd) {
                TidligereVedtakStatus.JA
            } else {
                TidligereVedtakStatus.NEI
            }
        } catch (e: Exception) {
            logger.warn("Feil ved sjekk av tidligere innvilget vedtak", e)
            TidligereVedtakStatus.VET_IKKE
        }
}
