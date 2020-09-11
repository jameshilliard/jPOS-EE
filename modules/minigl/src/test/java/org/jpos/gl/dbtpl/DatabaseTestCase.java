package org.jpos.gl.dbtpl;

import org.jpos.ee.DB;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DatabaseTestCase {
    private final String driver;

    public DatabaseTestCase(String driver) {
        this.driver = driver;
    }

    public String getDriver() {
        return driver;
    }

    private String buildPostgresConfig(String dbUrl, String user, String pass) {
        return "<?xml version='1.0' encoding='utf-8'?>\n" +
                "<!DOCTYPE hibernate-configuration PUBLIC\n" +
                "        \"-//Hibernate/Hibernate Configuration DTD 3.0//EN\"\n" +
                "        \"http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd\">\n" +
                "\n" +
                "<hibernate-configuration>\n" +
                "    <session-factory>\n" +
                "        <!-- Database connection settings -->\n" +
                "        <property name=\"connection.driver_class\">org.postgresql.Driver</property>\n" +
                "        <property name=\"dialect\">org.hibernate.dialect.PostgreSQLDialect</property>\n" +
                "        <property name=\"connection.url\">" + dbUrl + "</property>\n" +
                "        <property name=\"connection.username\">" + user + "</property>\n" +
                "        <property name=\"connection.password\">" + pass + "</property>\n" +
                "        <!-- JDBC connection pool (use the built-in) -->\n" +
                "        <property name=\"connection.pool_size\">1</property>\n" +
                "        <!-- Enable Hibernate's automatic session context management -->\n" +
                "        <property name=\"current_session_context_class\">thread</property>\n" +
                "        <!-- Disable the second-level cache  -->\n" +
                "        <property name=\"cache.provider_class\">org.hibernate.cache.internal.NoCacheProvider</property>\n" +
                "        <!-- Echo all executed SQL to stdout -->\n" +
                "        <property name=\"show_sql\">true</property>\n" +
                "        <!-- Drop and re-create the database schema on startup -->\n" +
                "        <property name=\"hbm2ddl.auto\">create</property>\n" +
                "\n" +
                "    </session-factory>\n" +
                "</hibernate-configuration>\n";
    }
    public DB getDB(File tempDir) throws IOException  {
        if (this.driver.equals("postgres")) {
            File configFile = new File(tempDir.getAbsolutePath() + "/hibernate.cfg.xml");
            if (!configFile.exists()) {
                if (!configFile.createNewFile()) {
                    throw new RuntimeException("failed to create config file: "+configFile.getAbsoluteFile());
                }
            }
            FileWriter myWriter = new FileWriter(configFile);
            myWriter.write(buildPostgresConfig("", "", "postgres"));
            myWriter.close();
            return new DB(configFile.toURI().toURL().toString());
        } else if (this.driver.equals("h2")) {
            return new DB();
        }
        throw new RuntimeException("unknown db driver");
    }
}
