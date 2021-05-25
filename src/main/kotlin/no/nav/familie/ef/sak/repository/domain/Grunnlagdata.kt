package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.domene.Grunnlagsdata
import org.springframework.data.annotation.Id
import java.util.UUID

data class Grunnlagdata(@Id
                        val behandlingId: UUID,
                        val data: Grunnlagsdata)
