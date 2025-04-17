package org.incept5.scheduler.db.helper

import org.hsqldb.jdbc.JDBCDataSource
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.sql.Connection
import java.sql.SQLException
import java.util.function.Consumer
import javax.sql.DataSource

/**
 * Creates a DataSource to the HSQL DB and initialises the tables used for the
 * DB scheduler.
 */
class TestDataSource {
    companion object {
        @Volatile
        private var ds: DataSource? = null

        fun getInstance(): DataSource {
            if (ds == null) {
                synchronized(TestDataSource::class.java) {
                    if (ds == null) {
                        ds = initDatabase()
                    }
                }
            }
            return ds!!
        }

        fun initDatabase(): DataSource {
            val dataSource = JDBCDataSource()
            dataSource.setURL("jdbc:hsqldb:mem:schedule_testing")
            dataSource.user = "sa"
            dataSource.setPassword("")
            doWithConnection(dataSource) { c: Connection ->
                try {
                    c.createStatement().use { statement ->
                        val createTables = readFile("/hsql_tables.sql")
                        statement.execute(createTables)
                    }
                } catch (e: Exception) {
                    throw RuntimeException("Failed to create tables", e)
                }
            }
            return dataSource
        }

        @Throws(IOException::class)
        private fun readFile(resource: String): String {
            BufferedReader(InputStreamReader(TestDataSource::class.java.getResourceAsStream(resource)!!)).use { `in` ->
                var line: String?
                val createTables = StringBuilder()
                while (`in`.readLine().also { line = it } != null) {
                    createTables.append(line)
                }
                return createTables.toString()
            }
        }

        private fun doWithConnection(dataSource: DataSource, consumer: Consumer<Connection>) {
            try {
                dataSource.connection.use { connection -> consumer.accept(connection) }
            } catch (e: SQLException) {
                throw RuntimeException("Error getting connection from datasource.", e)
            }
        }
    }
}
