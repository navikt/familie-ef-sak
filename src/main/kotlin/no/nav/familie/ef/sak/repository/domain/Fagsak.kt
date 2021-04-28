package no.nav.familie.ef.sak.repository.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.*

data class Fagsak(@Id
                  val id: UUID = UUID.randomUUID(),
                  @MappedCollection(idColumn = "fagsak_id")
                  val eksternId: EksternFagsakId = EksternFagsakId(),
                  @Column("stonadstype")
                  val stønadstype: Stønadstype,
                  @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                  val sporbar: Sporbar = Sporbar(),
                  @MappedCollection(idColumn = "fagsak_id")
                  val søkerIdenter: Set<FagsakPerson> = setOf()) {

    fun hentAktivIdent(): String {
        return søkerIdenter.maxByOrNull { it.sporbar.opprettetTid }?.ident ?: error("Fant ingen ident på fagsak $id")
    }
}

enum class Stønadstype {
    OVERGANGSSTØNAD,
    BARNETILSYN,
    SKOLEPENGER
}
enum class ResultatType{
    HENLEGGE,
    INNVILGE,
    AVSLÅ
}
sealed class VedtakDto
class Henlegge (val resultatType: ResultatType = ResultatType.HENLEGGE): VedtakDto()
class Innvilget(val resultatType: ResultatType = ResultatType.INNVILGE,
                val periodeBegrunnelse: String,
                val inntektBegrunnelse: String): VedtakDto()
class Avslå(val resultatType: ResultatType = ResultatType.AVSLÅ,
            val avslåBegrunnelse: String) : VedtakDto()

fun main() {
    val a: VedtakDto = Avslå(resultatType = ResultatType.AVSLÅ, "yolo")
    val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(a)
    val dto = objectMapper.readValue<Avslå>(json)
    println()
}