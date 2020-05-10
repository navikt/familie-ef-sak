package no.nav.familie.ef.sak.økonomi.dto

data class StatusFraOppdragDTO(val fagsystem: String,
                               val personIdent: String,
                               val behandlingsId: Long,
                               val vedtaksId: Long)
