import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class DatabaseConnection
{
	public   Connection connection;
	public static String HOSTURL;
	public LogWrapper logWrapper = new LogWrapper(DatabaseConnection.class.getName());

	public  Connection getConnection() throws SQLException 
	{
		HOSTURL = "jdbc:mysql://"+AppConstant.DB_IP+":"+AppConstant.DB_Port+"/"+AppConstant.DB_Name+"?"+AppConstant.DB_Extra_Config;
		if (connection == null || connection.isClosed())
		{
			try {
				Class.forName ("com.mysql.jdbc.Driver").newInstance();
				DriverManager.setLoginTimeout(100000);
				connection =  DriverManager.getConnection(HOSTURL,AppConstant.DB_User,AppConstant.DB_Pass);
				//System.out.println("DB connection successful");
			}
			catch (ClassNotFoundException e) {
				this.logWrapper.error(e);
			}
			catch (SQLException e) {
				this.logWrapper.error(e);
			} 
			catch (InstantiationException e) {
				this.logWrapper.error(e);
			} 
			catch (IllegalAccessException e) {
				this.logWrapper.error(e);
			}
			catch (Exception e) {
				this.logWrapper.error(e);
			}
		}
		return connection;

	}
	
	public void closeDBConnection() {
		try {
			this.connection.close();
		} catch (SQLException e) {}
	}

	public static Connection checkValidity(Connection conn)
	{
		LogWrapper log = null;
		try {
			log = new LogWrapper(DatabaseConnection.class.getName());
			if(conn == null)
			{
				return new DatabaseConnection().getConnection();
			}
			if(!conn.isValid(5))		// will check for 5 seconds
			{
				conn.close();
			}
			if(conn == null || conn.isClosed())
			{
				return new DatabaseConnection().getConnection();
			}
		} catch (SQLException e) {
			log.error(e);
		} catch (Exception e) {
			log.error(e);
		}
		return conn;
	}
}	