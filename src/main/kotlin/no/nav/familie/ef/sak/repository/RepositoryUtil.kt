package no.nav.familie.ef.sak.repository

import org.springframework.data.repository.CrudRepository

inline fun <reified T, ID> CrudRepository<T, ID>.findByIdOrThrow(id: ID): T {
    return findById(id).orElseThrow {
        IllegalStateException("Finner ikke ${T::class.simpleName} med id=$id")
    }
}
