package no.nav.familie.ef.sak.ekstern.soknad

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pensjon.HistoriskPensjonService
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

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
            if (folkeregisteridentifikatorer.isEmpty() || folkeregisteridentifikatorer.none { !it.metadata.historisk }) {
                return TidligereVedtakStatus.NEI
            }

            val aktivIdent = folkeregisteridentifikatorer.gjeldende().ident
            val identer = folkeregisteridentifikatorer.map { it.ident }.toSet()

            when {
                sisteFerdigstilteBehandlingErPåNyttRegelverk(identer) -> TidligereVedtakStatus.NEI
                harGjeldendePeriodePåGammeltRegelverkIEf(identer) -> TidligereVedtakStatus.JA
                harTidligereOvergangsstønadIInfotrygd(identer) -> TidligereVedtakStatus.JA
                else -> historiskPensjonStatus(aktivIdent, identer)
            }
        } catch (e: Exception) {
            logger.warn("Feil ved sjekk av tidligere innvilget vedtak", e)
            TidligereVedtakStatus.VET_IKKE
        }
    }

    private fun sisteFerdigstilteBehandlingErPåNyttRegelverk(identer: Set<String>): Boolean =
        behandlingRepository.erSisteFerdigstilteBehandlingPåNyttRegelverk(
            identer = identer,
            stønadstype = StønadType.OVERGANGSSTØNAD,
        ) == true

    private fun harGjeldendePeriodePåGammeltRegelverkIEf(identer: Set<String>): Boolean =
        behandlingRepository.harGjeldendePeriodePåGammeltRegelverk(
            identer = identer,
            stønadstype = StønadType.OVERGANGSSTØNAD,
            iDag = LocalDate.now(),
        )

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
}
