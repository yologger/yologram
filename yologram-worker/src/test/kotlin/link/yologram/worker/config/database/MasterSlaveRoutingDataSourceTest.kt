package link.yologram.worker.config.database

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.transaction.support.TransactionSynchronizationManager
import kotlin.test.assertEquals

class MasterSlaveRoutingDataSourceTest {

    private val routingDataSource = MasterSlaveRoutingDataSource()

    @AfterEach
    fun cleanUp() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)
    }

    @Test
    fun `readOnly 트랜잭션이면 SLAVE로 라우팅한다`() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(true)

        assertEquals(DBType.SLAVE, routingDataSource.determineCurrentLookupKeyForTest())
    }

    @Test
    fun `쓰기 트랜잭션이면 MASTER로 라우팅한다`() {
        TransactionSynchronizationManager.setCurrentTransactionReadOnly(false)

        assertEquals(DBType.MASTER, routingDataSource.determineCurrentLookupKeyForTest())
    }

    /** protected determineCurrentLookupKey 노출용 */
    private fun MasterSlaveRoutingDataSource.determineCurrentLookupKeyForTest(): Any? {
        val method = MasterSlaveRoutingDataSource::class.java.getDeclaredMethod("determineCurrentLookupKey")
        method.isAccessible = true
        return method.invoke(this)
    }
}
