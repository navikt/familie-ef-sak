package no.nav.familie.ef.sak.repository.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.time.YearMonth

data class UnderUtdanning(val skoleUtdanningssted: String,
                          val linjeKursGrad: String,
                          val fra: LocalDate,
                          val til: LocalDate,
                          val offentligEllerPrivat: String,
                          val heltidEllerDeltid: String,
                          val hvorMyeSkalDuStudere: Int?,
                          @Column("hva_er_malet_med_utdanningen")
                          val hvaErMåletMedUtdanningen: String?,
                          val utdanningEtterGrunnskolen: Boolean,
                          @MappedCollection(idColumn = "soknadsskjema_id")
                          val tidligereUtdanninger: Set<TidligereUtdanning>? = emptySet(),
                          val semesteravgift: Int? = null,
                          val studieavgift: Int? = null,
                          val eksamensgebyr: Int? = null)

data class TidligereUtdanning(val linjeKursGrad: String,
                              val fra: YearMonth,
                              val til: YearMonth)
