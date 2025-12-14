package model;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

public class ConPool {

    private static DataSource dataSource;

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            PoolProperties p = new PoolProperties();

            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String db = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String pass = System.getenv("DB_PASS");

            if (host == null)
                host = "localhost"; // Safe default
            if (port == null)
                port = "3306"; // Safe default
            if (db == null)
                db = "Progetto_TSW_Dependability"; // Safe default
            if (user == null)
                user = "root"; // Safe default
            if (pass == null)
                throw new RuntimeException("DB_PASS environment variable not set!");
            String tz = TimeZone.getDefault().getID();

            p.setUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?serverTimezone=" + tz);
            p.setDriverClassName("com.mysql.cj.jdbc.Driver");
            p.setUsername(user);
            p.setPassword(pass);

            // Impostazioni pool simili alle tue
            p.setMaxActive(100);
            p.setInitialSize(10);
            p.setMinIdle(10);
            p.setRemoveAbandoned(true);
            p.setRemoveAbandonedTimeout(60);

            dataSource = new DataSource();
            dataSource.setPoolProperties(p);
        }
        return dataSource.getConnection();
    }
}
