package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ef.sak.repository.domain.GrunnlagsdataData
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal class GrunnlagsdataServiceTest {
    private val om = objectMapper.copy()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .writerWithDefaultPrettyPrinter()

    /**
     * For å få med seg breaking changes i Grunnlagsdata. Hvis noe faktiskt er breaking change får man vurdere om man skal
     * gjøre noe annen diffing, eks att man lage en diff av Map i stedet for GrunnlagsdataData
     * Eks hvis ett felt slettes.
     */
    @Test
    internal fun `diff av grunnlagsdata`() {
        val tidligereDefinisjon = this::class.java.getResource("/json/grunnlagsdata.json").readText()
        val nyDefinisjon = om.writeValueAsString(getClassInfo(GrunnlagsdataData::class))
        assertThat(nyDefinisjon).isEqualTo(tidligereDefinisjon)
    }

    /**
     * Hvis GrunnlagsdataData har nullable felt, så må diff i GrunnlagsdataService endres, då den forventer seg att alle felt
     * på rootnivå er not nullable
     */
    @Test
    internal fun `GrunnlagsdataData kan ikke ha nullable felt`() {
        val nullableFeltIRootGrunnlagsdata = GrunnlagsdataData::class.constructors.first().parameters.filter {
            it.type.isMarkedNullable
        }.map { it.name }
        assertThat(nullableFeltIRootGrunnlagsdata).isEmpty()
    }

    private data class ObjectInfo(val name: String,
                                  val type: String,
                                  val fields: Map<String, ObjectInfo>? = null,
                                  val values: List<String>? = null)

    private val endClasses = setOf(String::class,
                                   Int::class,
                                   Long::class,
                                   LocalDate::class,
                                   LocalDateTime::class,
                                   YearMonth::class,
                                   Boolean::class)


    private fun getClassInfo(kClass: KClass<*>): Map<String, ObjectInfo> {
        val constructors = kClass.constructors
        if (constructors.size != 1) {
            error("${kClass.qualifiedName} has ${constructors.size} constructors")
        }
        return constructors.first().parameters.map { parameter ->
            val name = parameter.name!!
            val classifier = parameter.type.classifier as KClass<*>
            val simpleName = classifier.simpleName!!
            val qualifiedName = classifier.qualifiedName!!
            when {
                classifier in endClasses -> ObjectInfo(name, simpleName)
                classifier.isSubclassOf(Collection::class) -> {
                    val arguments = parameter.type.arguments
                    if (arguments.size != 1) {
                        error("Cannot handle collections with more than one type argument $qualifiedName")
                    }
                    val classInfo = getClassInfo(parameter.type.arguments[0].type!!.classifier as KClass<*>)
                    ObjectInfo(name, "Collection", classInfo)
                }
                classifier.isSubclassOf(Enum::class) ->
                    ObjectInfo(name, "Enum", null, classifier.java.enumConstants.map { it.toString() })
                qualifiedName.startsWith("java.") || qualifiedName.startsWith("kotlin.") ->
                    error("Class is not defined: $qualifiedName")
                else -> ObjectInfo(name, "Object", getClassInfo(classifier))
            }
        }.map { it.name to it }.toMap()
    }
}