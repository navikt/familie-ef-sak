package no.nav.familie.ef.sak.arbeidsfordeling

import no.nav.familie.ef.sak.infrastruktur.config.getNullable
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerIntegrasjonerClient
import no.nav.familie.kontrakter.felles.arbeidsfordeling.Enhet
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Component

@Component
class ArbeidsfordelingService(
    private val personopplysningerIntegrasjonerClient: PersonopplysningerIntegrasjonerClient,
    @Qualifier("shortCache")
    private val cacheManager: CacheManager,
) {
    companion object {
        const val MASKINELL_JOURNALFOERENDE_ENHET = "9999"
    }

    fun hentNavEnhetId(
        ident: String,
        oppgavetype: Oppgavetype,
    ) = when (oppgavetype) {
        Oppgavetype.VurderHenvendelse -> hentNavEnhetForOppfølging(ident, oppgavetype)?.enhetId
        else -> hentNavEnhet(ident)?.enhetId
    }

    fun hentNavEnhet(ident: String): Arbeidsfordelingsenhet? =
        cacheManager.getNullable("navEnhet", ident) {
            personopplysningerIntegrasjonerClient.hentNavEnhetForPersonMedRelasjoner(ident).firstOrNull()
        }

    fun hentNavEnhetForOppfølging(
        ident: String,
        oppgavetype: Oppgavetype,
    ): Enhet? =
        cacheManager.getNullable("navEnhetForOppfølging", ident) {
            personopplysningerIntegrasjonerClient.hentBehandlendeEnhetForOppfølging(ident)
                ?: error("Fant ikke NAV-enhet for oppgave av type $oppgavetype")
        }

    fun hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent: String): String = hentNavEnhet(personIdent)?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET
}
