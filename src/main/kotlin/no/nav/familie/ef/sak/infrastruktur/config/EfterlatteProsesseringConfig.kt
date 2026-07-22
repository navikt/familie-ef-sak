package no.nav.familie.ef.sak.infrastruktur.config

import efterlatte.prosessering.TaskProdusent
import efterlatte.prosessering.TaskRepository
import efterlatte.prosessering.postgres.PostgresTaskRepository
import efterlatte.prosessering.spring.ProsesseringProperties
import efterlatte.prosessering.spring.TaskService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * no.nav.efterlatte:prosessering-spring-boot-starter (pilotert av skyggekjøringInfotrygd-task, se
 * no.nav.familie.ef.sak.infotrygd.skygge) sin autokonfigurasjon (efterlatte.prosessering.spring.ProsesseringAutoConfiguration)
 * definerer beans under de samme standardnavnene («taskRepository», «taskService») som allerede er i bruk av
 * det etablerte no.nav.familie.prosessering-biblioteket (Spring Data JDBC-repositoryet
 * no.nav.familie.prosessering.domene.TaskRepository fra @EnableJdbcRepositories("no.nav.familie") i
 * [DatabaseConfiguration], og no.nav.familie.prosessering.internal.TaskService fra
 * @ComponentScan("no.nav.familie.prosessering") i [ApplicationConfig]).
 *
 * Uten disse to beanene ville appen fått to helt urelaterte bean-typer registrert under samme navn. Siden
 * `spring.main.allow-bean-definition-overriding` er skrudd på, ville det siste vunnet uten feilmelding ved
 * oppstart - og stille brutt alt som er avhengig av den andre typen.
 *
 * Ved å definere disse selv med egne, eksplisitte navn blir @ConditionalOnMissingBean (som matcher på
 * returtype) i autokonfigurasjonen falsk, slik at den ikke lager sine egne konfliktende beans. De to
 * task-bibliotekene kan dermed leve side om side under migreringen.
 */
@Configuration
class EfterlatteProsesseringConfig {
    @Bean("efterlatteProsesseringTaskRepository")
    fun efterlatteProsesseringTaskRepository(
        dataSource: DataSource,
        properties: ProsesseringProperties,
    ): TaskRepository = PostgresTaskRepository(dataSource = dataSource, skjema = properties.skjema)

    @Bean("efterlatteProsesseringTaskService")
    fun efterlatteProsesseringTaskService(
        dataSource: DataSource,
        taskProdusent: TaskProdusent,
    ): TaskService = TaskService(dataSource = dataSource, produsent = taskProdusent)
}
