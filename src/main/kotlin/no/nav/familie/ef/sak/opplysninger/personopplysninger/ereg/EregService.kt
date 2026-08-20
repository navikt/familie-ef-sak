package no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg

import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.kontrakter.felles.organisasjon.Organisasjon
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class EregService(
    private val eregClient: EregClient,
) {
    fun hentOrganisasjon(organisasjonsnummer: String): Organisasjon {
        val organisasjon = eregClient.hentOrganisasjoner(listOf(organisasjonsnummer)).firstOrNull()

        return organisasjon?.let { mapOrganisasjonDto(it) } ?: throw ApiFeil(
            "Finner ingen organisasjon for søket",
            HttpStatus.BAD_REQUEST,
        )
    }
}
