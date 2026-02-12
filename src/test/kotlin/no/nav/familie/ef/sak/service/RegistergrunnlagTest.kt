package no.nav.familie.ef.sak.service

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.kontrakter.felles.jsonMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal class RegistergrunnlagTest {
    private val om =
        jsonMapper
            .rebuild()
            .changeDefaultPropertyInclusion { JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.USE_DEFAULTS) }
            .build()
            .writerWithDefaultPrettyPrinter()

    /**
     * For å få med seg breaking changes i Grunnlagsdata. Hvis noe faktiskt er breaking change får man vurdere om man skal
     * gjøre noe annen diffing, eks att man lage en diff av Map i stedet for GrunnlagsdataData.
     * Ellers kan man oppdatere grunnlagsdata.json med den nye diffen
     * Eks hvis ett felt slettes.
     */
    @Test
    internal fun `diff av grunnlagsdata v2`() {
        val tidligereDefinisjon = this::class.java.getResource("/json/grunnlagsdata_v2.json").readText()
        val nyDefinisjon = om.writeValueAsString(getClassInfo(GrunnlagsdataDomene::class))
        assertThat(nyDefinisjon).isEqualTo(tidligereDefinisjon)
    }

    private data class ObjectInfo(
        val name: String,
        val type: String,
        val fields: Map<String, ObjectInfo>? = null,
        val values: List<String>? = null,
        val nullable: Boolean,
    )

    private val endClasses =
        setOf(
            String::class,
            UUID::class,
            Int::class,
            Long::class,
            Float::class,
            LocalDate::class,
            LocalDateTime::class,
            YearMonth::class,
            Boolean::class,
        )

    private fun getClassInfo(kClass: KClass<*>): Map<String, ObjectInfo> {
        val className = kClass.qualifiedName
        val constructors = kClass.constructors
        if (constructors.size != 1) {
            error("$className has ${constructors.size} constructors")
        }
        return constructors
            .first()
            .parameters
            .map { parameter ->
                val name = parameter.name!!
                val classifier = parameter.type.classifier as KClass<*>
                val simpleName = classifier.simpleName!!
                val qualifiedName = classifier.qualifiedName!!
                val nullable = parameter.type.isMarkedNullable
                when {
                    classifier in endClasses -> {
                        ObjectInfo(name, simpleName, nullable = nullable)
                    }

                    classifier.isSubclassOf(Collection::class) -> {
                        val arguments = parameter.type.arguments
                        if (arguments.size != 1) {
                            error("$className Cannot handle collections with more than one type argument $qualifiedName")
                        }
                        val classInfo =
                            getClassInfo(
                                parameter.type.arguments[0]
                                    .type!!
                                    .classifier as KClass<*>,
                            )
                        ObjectInfo(name, "Collection", classInfo, nullable = nullable)
                    }

                    classifier.isSubclassOf(Enum::class) -> {
                        ObjectInfo(name, "Enum", null, classifier.java.enumConstants.map { it.toString() }, nullable)
                    }

                    qualifiedName.startsWith("java.") || qualifiedName.startsWith("kotlin.") -> {
                        error("$className - Class is not defined: $qualifiedName")
                    }

                    else -> {
                        ObjectInfo(name, "Object", getClassInfo(classifier), nullable = nullable)
                    }
                }
            }.associateBy { it.name }
    }
}
