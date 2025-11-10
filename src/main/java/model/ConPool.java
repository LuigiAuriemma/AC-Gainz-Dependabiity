package model;

/*
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.TimeZone;

public class ConPool {

    private static DataSource dataSource;

    public static Connection getConnection() throws SQLException {
        if( dataSource == null) {
            PoolProperties p=new PoolProperties();
            p.setUrl("jdbc:mysql://localhost:3306/Progetto_TSW_Dependability?serverTimezone=" + TimeZone.getDefault().getID());
            p.setDriverClassName("com.mysql.cj.jdbc.Driver");
            p.setUsername("root");
            p.setPassword("123456789");
            p.setMaxActive(100);
            p.setInitialSize(10);
            p.setMinIdle(10);
            p.setRemoveAbandonedTimeout(60);
            p.setRemoveAbandoned(true);
            dataSource=new DataSource();
            dataSource.setPoolProperties(p);
        }
        return dataSource.getConnection();
    }
}
 */
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

            String host = System.getenv().getOrDefault("DB_HOST", "db");
            String port = System.getenv().getOrDefault("DB_PORT", "3306");
            String db   = System.getenv().getOrDefault("DB_NAME", "Progetto_TSW_Dependability");
            String user = System.getenv().getOrDefault("DB_USER", "root");
            String pass = System.getenv().getOrDefault("DB_PASS", "123456789");
            String tz   = TimeZone.getDefault().getID();

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
