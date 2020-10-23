package no.nav.familie.ef.sak.repository.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection

data class Medlemskap(val oppholderDuDegINorge: Boolean,
                      @Column("bosatt_norge_siste_arene")
                      val bosattNorgeSisteÅrene: Boolean,
                      @MappedCollection(idColumn = "soknadsskjema_id")
                      val utenlandsopphold: Set<Utenlandsopphold> = emptySet())
