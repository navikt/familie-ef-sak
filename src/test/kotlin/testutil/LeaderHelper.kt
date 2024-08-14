package no.nav.familie.ef.sak.testutil

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import no.nav.familie.leader.LeaderClient

fun kjørSomLeader(
    erLeder: Boolean = true,
    testkode: () -> Unit,
) {
    mockkStatic(LeaderClient::class)
    mockkObject(LeaderClient)
    every { LeaderClient.isLeader() } returns erLeder
    testkode()
    mockkObject(LeaderClient)
    unmockkStatic(LeaderClient::class)
}
