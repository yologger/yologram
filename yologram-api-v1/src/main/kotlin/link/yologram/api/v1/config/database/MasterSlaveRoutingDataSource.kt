package link.yologram.api.v1.config.database

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource
import org.springframework.transaction.support.TransactionSynchronizationManager

class MasterSlaveRoutingDataSource : AbstractRoutingDataSource() {
    override fun determineCurrentLookupKey(): Any? {
        if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return DBType.SLAVE
        }
        return DBType.MASTER
    }
}

enum class DBType {
    MASTER, SLAVE
}
