package link.yologram.worker.config.database

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.sql.DataSource

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
    basePackages = ["link.yologram.worker"],
    entityManagerFactoryRef = "mainEntityManager",
    transactionManagerRef = "mainTransactionManager"
)
class CoreDatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "database.main.writer.datasource")
    fun writerDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "database.main.writer.datasource.hikari")
    fun writerHikariDataSource(@Qualifier("writerDataSourceProperties") writerProperty: DataSourceProperties): HikariDataSource {
        return writerProperty.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
    }

    @Bean
    @ConfigurationProperties(prefix = "database.main.reader.datasource")
    fun readerDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean
    @ConfigurationProperties(prefix = "database.main.reader.datasource.hikari")
    fun readerHikariDataSource(@Qualifier("readerDataSourceProperties") readerProperty: DataSourceProperties): HikariDataSource {
        return readerProperty.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
    }

    @Bean
    fun routingDataSource(
        @Qualifier("writerHikariDataSource") writerDataSource: DataSource,
        @Qualifier("readerHikariDataSource") readerDataSource: DataSource
    ): DataSource {
        val dataSourceMap: Map<Any, Any> = hashMapOf(
            DBType.MASTER to writerDataSource,
            DBType.SLAVE to readerDataSource
        )

        return LazyConnectionDataSourceProxy(MasterSlaveRoutingDataSource().apply {
            this.setDefaultTargetDataSource(writerDataSource)
            this.setTargetDataSources(dataSourceMap)
            this.afterPropertiesSet()
        })
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "database.main.jpa")
    fun mainJpaProperties(): JpaProperties {
        return JpaProperties()
    }

    @Bean
    @Primary
    fun mainEntityManager(
        builder: EntityManagerFactoryBuilder,
        @Qualifier("routingDataSource") routingDataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean {
        return builder
            .dataSource(routingDataSource)
            .packages("link.yologram.worker")
            .build()
    }

    @Bean
    @Primary
    fun mainTransactionManager(mainEntityManager: EntityManagerFactory): PlatformTransactionManager {
        return JpaTransactionManager(mainEntityManager)
    }
}
