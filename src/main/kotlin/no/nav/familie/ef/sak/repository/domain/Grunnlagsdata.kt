package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Grunnlagsdata(@Id
                         val behandlingId: UUID,
                         val data: GrunnlagsdataData,
                         val diff: Boolean = false,
                         @Version
                         val versjon: Int = 0,
                         @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                         val sporbar: Sporbar = Sporbar())

data class GrunnlagsdataData(val medlemskap: MedlemskapRegistergrunnlagDto,
                             val sivilstand: SivilstandRegistergrunnlagDto,
                             val version: Int = 1)

