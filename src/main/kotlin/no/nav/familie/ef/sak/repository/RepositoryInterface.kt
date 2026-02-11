package no.nav.familie.ef.sak.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * På grunn av att vi setter id's på våre entitetet så prøver spring å oppdatere våre entiteter i stedet for å ta insert
 */
@NoRepositoryBean
interface RepositoryInterface<T : Any, ID : Any> : CrudRepository<T, ID> {
    @Deprecated("Støttes ikke, bruk insert/update")
    override fun <S : T> save(entity: S): S = throw UnsupportedOperationException("save() er deaktivert – bruk insert/update i stedet.")

    @Deprecated("Støttes ikke, bruk insertAll/updateAll")
    override fun <S : T> saveAll(entities: Iterable<S>): Iterable<S> = throw UnsupportedOperationException("saveAll() er deaktivert – bruk insertAll/updateAll i stedet.")
}
