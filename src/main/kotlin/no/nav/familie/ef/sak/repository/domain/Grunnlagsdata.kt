package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.api.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

/**
 * TODO borde DATA lagras som JsonWrapper/json string och heta eks inngangsvilkar?
 * så hvis det er diff og man henter inngangsvilkår henter man grunnlagsdata fra her og fra "nye"/diff
 */
data class Grunnlagsdata(@Id
                         val behandlingId: UUID,
                         val data: GrunnlagsdataData,
                         val endringer: GrunnlagsdataData? = null,
                         val diff: Boolean = false,
                         @Version
                         val versjon: Int = 0,
                         @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                         val sporbar: Sporbar = Sporbar())

data class GrunnlagsdataData(val medlemskap: MedlemskapRegistergrunnlagDto,
                             val sivilstand: SivilstandRegistergrunnlagDto)
