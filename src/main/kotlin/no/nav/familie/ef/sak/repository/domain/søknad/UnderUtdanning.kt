package no.nav.familie.ef.sak.repository.domain.søknad

import no.nav.familie.kontrakter.ef.søknad.EnumTekstverdiMedSvarId
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.YearMonth

data class UnderUtdanning(val skoleUtdanningssted: String,
                          val linjeKursGrad: String,
                          val fra: LocalDate,
                          val til: LocalDate,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "offentlig_eller_privat_")
                          val offentligEllerPrivat: EnumTekstverdiMedSvarId,
                          @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "heltid_eller_deltid_")
                          val heltidEllerDeltid: EnumTekstverdiMedSvarId,
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
