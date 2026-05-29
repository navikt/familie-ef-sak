package no.nav.familie.ef.sak.ekstern.soknad

import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.TidligereVedtaksperioderService
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class EksternSøknadService(
    private val personService: PersonService,
    private val tidligereVedtaksperioderService: TidligereVedtaksperioderService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun harTidligereInnvilgetVedtak(personIdent: String): TidligereVedtakStatus =
        try {
            val folkeregisteridentifikatorer = personService.hentSøker(personIdent).folkeregisteridentifikator
            val tidligereVedtaksperioder = tidligereVedtaksperioderService.hentTidligereVedtaksperioder(folkeregisteridentifikatorer)

            if (tidligereVedtaksperioder.tilDto().harTidligereVedtaksperioder()) {
                TidligereVedtakStatus.JA
            } else {
                TidligereVedtakStatus.NEI
            }
        } catch (e: Exception) {
            logger.warn("Feil ved sjekk av tidligere innvilget vedtak", e)
            TidligereVedtakStatus.VET_IKKE
        }
}
