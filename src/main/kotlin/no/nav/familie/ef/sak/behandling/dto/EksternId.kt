package no.nav.familie.ef.sak.behandling.dto

import java.util.UUID

data class EksternId(val behandlingId: UUID, val eksternBehandlingId: Long, val eksternFagsakId: Long)