package no.nav.familie.ef.sak.ekstern.soknad

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
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

    fun harTidligereInnvilgetVedtak(personIdent: String): TidligereVedtakStatus {
        return try {
            val folkeregisteridentifikatorer = personService.hentSøker(personIdent).folkeregisteridentifikator
            if (folkeregisteridentifikatorer.none { !it.metadata.historisk }) {
                return TidligereVedtakStatus.VET_IKKE
            }

            val aktivIdent = folkeregisteridentifikatorer.gjeldende().ident
            val identer = folkeregisteridentifikatorer.map { it.ident }.toSet()

            when (sisteFerdigstilteBehandlingErPåNyttRegelverk(identer)) {
                true -> TidligereVedtakStatus.NEI
                false -> TidligereVedtakStatus.JA
                null -> statusFraEksternHistorikk(aktivIdent = aktivIdent, identer = identer)
            }
        } catch (e: Exception) {
            logger.warn("Feil ved sjekk av tidligere innvilget vedtak", e)
            TidligereVedtakStatus.VET_IKKE
        }
    }

    private fun sisteFerdigstilteBehandlingErPåNyttRegelverk(identer: Set<String>): Boolean? =
        behandlingRepository.erSisteFerdigstilteBehandlingPåNyttRegelverk(
            identer = identer,
            stønadstyper = STØNADSTYPER_SOM_GIR_GAMMEL_ORDNING,
        )

    private fun statusFraEksternHistorikk(
        aktivIdent: String,
        identer: Set<String>,
    ): TidligereVedtakStatus =
        when {
            harTidligereOvergangsstønadIInfotrygd(identer) -> TidligereVedtakStatus.JA
            else -> historiskPensjonStatus(aktivIdent = aktivIdent, identer = identer)
        }

    private fun harTidligereOvergangsstønadIInfotrygd(identer: Set<String>): Boolean = infotrygdService.hentPerioderFraReplika(identer).overgangsstønad.isNotEmpty()

    private fun historiskPensjonStatus(
        aktivIdent: String,
        identer: Set<String>,
    ): TidligereVedtakStatus =
        when (historiskPensjonService.hentHistoriskPensjon(aktivIdent, identer).harPensjonsdata()) {
            null -> TidligereVedtakStatus.VET_IKKE
            true -> TidligereVedtakStatus.JA
            false -> TidligereVedtakStatus.NEI
        }

    companion object {
        private val STØNADSTYPER_SOM_GIR_GAMMEL_ORDNING =
            setOf(StønadType.OVERGANGSSTØNAD, StønadType.BARNETILSYN, StønadType.SKOLEPENGER)
    }
}
