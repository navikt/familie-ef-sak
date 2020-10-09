package no.nav.familie.ef.sak.repository.domain.søknad

data class UnderUtdanning(val skoleUtdanningssted: String,
                          @Deprecated("Bruk gjeldende utdanning") val utdanning: TidligereUtdanning?,
                          val gjeldendeUtdanning: GjeldendeUtdanning?,
                          val offentligEllerPrivat: String,
                          val heltidEllerDeltid: String,
                          val hvorMyeSkalDuStudere: Int?,
                          val hvaErMåletMedUtdanningen: String?,
                          val utdanningEtterGrunnskolen: Boolean,
                          val tidligereUtdanninger: List<TidligereUtdanning>? = null,
                          val semesteravgift: Double? = null,
                          val studieavgift: Double? = null,
                          val eksamensgebyr: Double? = null)

/**
 *  semesteravgift, studieavgift, eksamensgebyr gjelder kun Skolepenger
 */
