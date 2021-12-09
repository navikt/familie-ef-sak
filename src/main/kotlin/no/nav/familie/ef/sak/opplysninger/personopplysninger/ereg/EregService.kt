package no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg

import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.stereotype.Service

@Service
class EregService(private val eregClient: EregClient) {

    fun hentOrganisasjoner(organisasjonsnumre: List<String>) : List<Organisasjon> {
        val organisasjoner = eregClient.hentOrganisasjoner(organisasjonsnumre)
        return mapEregResultat(organisasjoner)
    }

    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon{
        val organisasjon = eregClient.hentOrganisasjoner(listOf(organisasjonsnummer)).firstOrNull()

        return organisasjon?.let { mapOrganisasjonDto(it) } ?: error("Fant ikke organisasjon")
    }
}