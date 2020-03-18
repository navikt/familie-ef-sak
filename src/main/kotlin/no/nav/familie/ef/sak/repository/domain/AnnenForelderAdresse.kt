package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.relational.core.mapping.Table

data class AnnenForelderAdresse(val adresse: String? = null,
                                val postnummer: String,
                                val poststedsnavn: String? = null,
                                val land: String? = null)

object AnnenForelderAdresseMapper {

    fun toDomain(adresse: no.nav.familie.kontrakter.ef.søknad.Adresse?): AnnenForelderAdresse? {
        if (adresse == null) {
            return null
        }
        return AnnenForelderAdresse(adresse = adresse.adresse,
                                    postnummer = adresse.postnummer,
                                    poststedsnavn = adresse.poststedsnavn,
                                    land = adresse.land)
    }
}