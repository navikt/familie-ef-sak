package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.YearMonth

data class UnderUtdanning(val skoleUtdanningssted: String,
                          val linjeKursGrad: String,
                          val fra: LocalDate,
                          val til: LocalDate,
                          val offentligEllerPrivat: String? = null,
                          val heltidEllerDeltid: String? = null,
                          val hvorMyeSkalDuStudere: Int?,
                          @Column("hva_er_malet_med_utdanningen")
                          val hvaErMåletMedUtdanningen: String?,
                          val utdanningEtterGrunnskolen: Boolean,
                          val semesteravgift: Int? = null,
                          val studieavgift: Int? = null,
                          val eksamensgebyr: Int? = null)

data class TidligereUtdanning(val linjeKursGrad: String,
                              val fra: YearMonth,
                              val til: YearMonth)
