package no.nav.familie.ef.sak.repository.domain

import org.springframework.jdbc.core.RowMapper
import java.sql.ResultSet
import java.util.*

data class TilkjentYtelseMedMetaData(val tilkjentYtelse: TilkjentYtelse, val eksternBehandlingId: Long, val eksternFagsakId: Long)


