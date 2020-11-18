package no.nav.familie.ef.sak.repository.domain

data class TilkjentYtelseMedMetaData(val tilkjentYtelse: TilkjentYtelse,
                                     val eksternBehandlingId: Long,
                                     val stønadstype: Stønadstype,
                                     val eksternFagsakId: Long)


