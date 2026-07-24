package no.nav.familie.ef.sak.infrastruktur.config

import efterlatte.prosessering.ProcessingEngine
import efterlatte.prosessering.Reaper
import efterlatte.prosessering.StandardTaskProdusent
import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.TaskStep
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.spring.ProsesseringLivssyklus
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
 * I tillegg mangler autokonfigurasjonens `@ConditionalOnBean(DataSource::class)` en
 * `@AutoConfigureAfter(DataSourceAutoConfiguration::class)`, så den kan bli evaluert før DataSource-bønnen
 * finnes - da hopper *hele* autokonfigurasjonen over, inkludert [ProsesseringLivssyklus] (selve motoren som
 * plukker og kjører KLAR-tasks). Tasks ble da lagret fint via vår egen [TaskService], men aldri plukket opp.
 *
 * Vi ekskluderer derfor `ProsesseringAutoConfiguration` helt (se `spring.autoconfigure.exclude` i
 * application.yml) og tar full manuell kontroll over [TaskRepository], [TaskProdusent], [TaskService] og
 * [ProsesseringLivssyklus] selv, uavhengig av bean-oppdagelsesrekkefølge.
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

    @Bean("efterlatteProsesseringLivssyklus")
    fun efterlatteProsesseringLivssyklus(
        taskRepository: TaskRepository,
        taskProdusent: TaskProdusent,
        steg: List<TaskStep<*>>,
        properties: ProsesseringProperties,
    ): ProsesseringLivssyklus {
        val engine =
            ProcessingEngine(
                repo = taskRepository,
                produsent = taskProdusent,
                steg = steg,
                node = properties.node,
                batchStorrelse = properties.batchStorrelse,
                maxSamtidighet = properties.maxSamtidighet,
                maxAntallFeil = properties.maxAntallFeil,
            )
        val reaper = if (properties.reaperPaa) Reaper(repo = taskRepository) else null
        return ProsesseringLivssyklus(engine = engine, reaper = reaper, node = properties.node)
    }
}
