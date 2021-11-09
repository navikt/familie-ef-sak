package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.kontrakter.ef.felles.StønadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("mellomlagret_fritstaende_brev")
data class MellomlagretFrittståendeBrev(@Id val id: UUID = UUID.randomUUID(),
                                        val fagsakId: UUID,
                                        val brev: Fritekstbrev,
                                        val brevType: FrittståendeBrevKategori,
                                        val saksbehandlerIdent: String,
                                        val tidspunktOpprettet: LocalDateTime = LocalDateTime.now())

