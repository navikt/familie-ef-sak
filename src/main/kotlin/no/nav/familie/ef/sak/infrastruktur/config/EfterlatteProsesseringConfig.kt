package no.nav.familie.ef.sak.infrastruktur.config

import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.spring.ProsesseringProperties
import efterlatte.prosessering.spring.TaskService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * efterlatte-prosessering sin autokonfigurasjon (ProsesseringAutoConfiguration) definerer beans under de
 * samme standardnavnene («taskRepository», «taskService») som det etablerte no.nav.familie.prosessering-
 * biblioteket allerede bruker. Med `allow-bean-definition-overriding` på ville det stille brutt en av dem.
 * Vi definerer derfor [TaskRepository], [TaskProdusent] og [TaskService] selv under egne navn, slik at
 * autokonfigurasjonens @ConditionalOnMissingBean slår av dens egne (konfliktende) beans.
 *
 * [TaskProdusent] og [ProsesseringProperties] lages/aktiveres eksplisitt her fordi autokonfigurasjonens
 * `@ConditionalOnBean(DataSource::class)` mangler `@AutoConfigureAfter(DataSourceAutoConfiguration::class)`
 * og derfor kan bli evaluert før DataSource-bønnen finnes - da hopper hele autokonfigurasjonen over, og gir
 * `NoSuchBeanDefinitionException` ved oppstart.
 */
@Configuration
@EnableConfigurationProperties(ProsesseringProperties::class)
class EfterlatteProsesseringConfig {
    @Bean("efterlatteProsesseringTaskRepository")
    fun efterlatteProsesseringTaskRepository(
        dataSource: DataSource,
        properties: ProsesseringProperties,
    ): TaskRepository = PostgresTaskRepository(dataSource = dataSource, skjema = properties.skjema)

    @Bean("efterlatteProsesseringTaskProdusent")
    fun efterlatteProsesseringTaskProdusent(taskRepository: TaskRepository): TaskProdusent = StandardTaskProdusent(repo = taskRepository)

    @Bean("efterlatteProsesseringTaskService")
    fun efterlatteProsesseringTaskService(
        dataSource: DataSource,
        taskProdusent: TaskProdusent,
    ): TaskService = TaskService(dataSource = dataSource, produsent = taskProdusent)
}
