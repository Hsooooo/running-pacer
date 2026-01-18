package io.hansu.pacer.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.net.URI
import javax.sql.DataSource

@Configuration
@Profile("render")
class RenderDatabaseConfig {

    @Bean
    fun dataSource(): DataSource {
        val databaseUrl = System.getenv("DATABASE_URL") ?: throw IllegalStateException("DATABASE_URL not set")
        
        val standardUriString = if (databaseUrl.startsWith("jdbc:")) databaseUrl.substring(5) else databaseUrl
        val dbUri = URI(standardUriString)

        var username = ""
        var password = ""
        if (dbUri.userInfo != null) {
            val userInfo = dbUri.userInfo.split(":")
            username = userInfo[0]
            password = if (userInfo.size > 1) userInfo[1] else ""
        }

        val port = if (dbUri.port == -1) 5432 else dbUri.port
        val dbUrl = "jdbc:postgresql://${dbUri.host}:$port${dbUri.path}"

        val config = HikariConfig()
        config.jdbcUrl = dbUrl
        config.username = username
        config.password = password
        config.driverClassName = "org.postgresql.Driver"

        return HikariDataSource(config)
    }
}
