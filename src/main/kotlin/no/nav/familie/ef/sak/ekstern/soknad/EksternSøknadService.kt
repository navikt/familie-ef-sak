package no.nav.familie.ef.sak.ekstern.soknad

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldendeEllerNull
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EksternSøknadService(
    private val personService: PersonService,
    private val behandlingRepository: BehandlingRepository,
    private val infotrygdService: InfotrygdService,
    private val historiskPensjonService: HistoriskPensjonService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harTidligereInnvilgetVedtak(personIdent: String): TidligereVedtakStatus =
        try {
            val folkeregisteridentifikatorer = personService.hentSøker(personIdent).folkeregisteridentifikator
            val aktivIdent = folkeregisteridentifikatorer.gjeldendeEllerNull()?.ident

            if (aktivIdent == null) {
                TidligereVedtakStatus.VET_IKKE
            } else {
                val identer = folkeregisteridentifikatorer.map { it.ident }.toSet()

                statusFraSisteBehandling(identer)
                    ?: statusFraInfotrygd(identer)
                    ?: statusFraHistoriskPensjon(aktivIdent = aktivIdent, identer = identer)
            }
        } catch (e: Exception) {
            logger.warn("Feil ved sjekk av tidligere innvilget vedtak", e)
            TidligereVedtakStatus.VET_IKKE
        }

    private fun statusFraSisteBehandling(identer: Set<String>): TidligereVedtakStatus? =
        when (behandlingRepository.erSisteFerdigstilteBehandlingPåNyttRegelverk(identer, STØNADSTYPER_SOM_GIR_GAMMEL_ORDNING)) {
            true -> TidligereVedtakStatus.NEI
            false -> TidligereVedtakStatus.JA
            null -> null
        }

    private fun statusFraInfotrygd(identer: Set<String>): TidligereVedtakStatus? =
        TidligereVedtakStatus.JA
            .takeIf { infotrygdService.hentPerioderFraReplika(identer).overgangsstønad.isNotEmpty() }

    private fun statusFraHistoriskPensjon(
        aktivIdent: String,
        identer: Set<String>,
    ): TidligereVedtakStatus =
        when (historiskPensjonService.hentHistoriskPensjon(aktivIdent, identer).harPensjonsdata()) {
            true -> TidligereVedtakStatus.JA
            false -> TidligereVedtakStatus.NEI
            null -> TidligereVedtakStatus.VET_IKKE
        }

    companion object {
        private val STØNADSTYPER_SOM_GIR_GAMMEL_ORDNING =
            setOf(StønadType.OVERGANGSSTØNAD, StønadType.BARNETILSYN, StønadType.SKOLEPENGER)
    }
}
