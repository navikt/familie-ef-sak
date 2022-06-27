package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.EksternBehandlingId
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.UUID

data class GammelBehandlingDto (
        val id: UUID,
        val fagsakId: UUID,
        val forrigeBehandlingId: UUID? = null,
        val versjon: Int = 0,

        val type: BehandlingType,
        val status: BehandlingStatus,
        @Column("stonadstype")
        val stønadstype: StønadType? = null,
        @Column("arsak")
        val årsak: BehandlingÅrsak,
        val kravMottatt: LocalDate? = null,

        @Column("opprettet_tid")
        val opprettet: LocalDate? = null,

        val resultat: BehandlingResultat,
)