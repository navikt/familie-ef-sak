package no.nav.familie.ef.sak.opplysninger.personopplysninger

import no.nav.familie.ef.sak.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.ef.sak.infrastruktur.exception.PdlRequestException
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBolkResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdentBolkResponse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlResponse

val logger = Logg.getLogger(PdlClient::class)

inline fun <reified DATA : Any, reified T : Any> feilsjekkOgReturnerData(
    ident: String?,
    pdlResponse: PdlResponse<DATA>,
    dataMapper: (DATA) -> T?,
): T {
    if (pdlResponse.harFeil()) {
        if (pdlResponse.errors?.any { it.extensions?.notFound() == true } == true) {
            throw PdlNotFoundException()
        }
        logger.error("Feil ved henting av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${T::class} fra PDL. Se securelogs for detaljer.")
        logger.warn("Advarsel ved henting av ${T::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }
    val data = dataMapper.invoke(pdlResponse.data)
    if (data == null) {
        val errorMelding = if (ident != null) "Feil ved oppslag på ident $ident. " else "Feil ved oppslag på person."
        logger.error(
            errorMelding +
                "PDL rapporterte ingen feil men returnerte tomt datafelt",
        )
        throw PdlRequestException("Manglende ${T::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
    }
    return data
}

inline fun <reified T : Any> feilsjekkOgReturnerData(pdlResponse: PdlBolkResponse<T>): Map<String, T> {
    if (pdlResponse.data == null) {
        logger.error("Data fra pdl er null ved bolkoppslag av ${T::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Data er null fra PDL -  ${T::class}. Se secure logg for detaljer.")
    }

    val feil =
        pdlResponse.data.personBolk
            .filter { it.code != "ok" }
            .associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        logger.error("Feil ved henting av ${T::class} fra PDL: $feil")
        throw PdlRequestException("Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${T::class} fra PDL. Se securelogs for detaljer.")
        logger.warn("Advarsel ved henting av ${T::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }
    return pdlResponse.data.personBolk.associateBy({ it.ident }, { it.person!! })
}

fun feilmeldOgReturnerData(pdlResponse: PdlIdentBolkResponse): Map<String, PdlIdent> {
    if (pdlResponse.data == null) {
        logger.error("Data fra pdl er null ved bolkoppslag av identer fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Data er null fra PDL -  ${PdlIdentBolkResponse::class}. Se secure logg for detaljer.")
    }

    val feil =
        pdlResponse.data.hentIdenterBolk
            .filter { it.code != "ok" }
            .associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        // Logg feil og gå vider. Ved feil returneres nåværende ident.
        logger.error("Feil ved henting av ${PdlIdentBolkResponse::class}. Nåværende ident returnert.")
        logger.error("Feil ved henting av ${PdlIdentBolkResponse::class} fra PDL: $feil. Nåværende ident returnert.")
    }
    return pdlResponse.data.hentIdenterBolk.associateBy({ it.ident }, { it.gjeldende() })
}
