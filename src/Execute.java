import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Raiyan on 21-Aug-17.
 */
public class Execute {
    ArrayBlockingQueue<SMS>[] qList;
    LinkedBlockingQueue<SMS> dList;
    HashMap<Integer, OperatorInfo> operatorInfos;
    private Statement statement = null;
    private ResultSet resultSet = null;
    private LogWrapper logWrapper;
    public Connection connection;

    public static void main(String[] args){
        PropertyConfigurator.configure("log4j.properties");
        try {
            Execute application = new Execute();
            System.out.println("Starting Application...");
            application.start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Execute() throws FileNotFoundException, SQLException {
        Configuration cfg = new Configuration();

        if(!AppConstant.loadConfiguration(cfg)) {
            System.err.println("Error while reading configuration values in AppConstant");
            System.exit(1);
        }
        this.logWrapper = new LogWrapper(Execute.class.getName());
        operatorInfos = new HashMap<Integer, OperatorInfo>(50);

        this.logWrapper.info("Loading configuration successful.");
        this.getOperatorInformation();
        this.qList = new ArrayBlockingQueue[AppConstant.NumberOfPickThreads];
        for(int i = 0; i < AppConstant.NumberOfPickThreads; i++)
        {
            this.qList[i] = new ArrayBlockingQueue<SMS>(AppConstant.PickupNo * 5);
        }

        this.dList = new LinkedBlockingQueue<SMS>();
    }


    public void getOperatorInformation() throws SQLException {
        this.connection = DatabaseConnection.checkValidity(this.connection);
        String query = "SELECT * from operator where Status = 'Active'";
        try {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);
            while( resultSet.next() )
            {
                int id = resultSet.getInt("id");
                OperatorInfo temp = new OperatorInfo();
                temp.setValues(resultSet);

                this.operatorInfos.put(id, temp);
            }

            connection.close();
            statement.close();
            resultSet.close();
        } catch (SQLException e) {
            this.logWrapper.error("err + "+query, e);
            System.exit(1);
        } catch(NullPointerException e) {
            this.logWrapper.error(e);
            System.exit(1);
        } catch(Exception e) {
            this.logWrapper.error(e);
            System.exit(1);
        }
    }
    public void start() throws SQLException {
        int i;
        for(i=0; i < AppConstant.NumberOfPickThreads; i++)
        {
            Pick p = new Pick(i, this.qList[i] );
            new Thread(p).start();
        }

        int sendingThread = AppConstant.NumberOfPickThreads * AppConstant.Multiple;
        for(i=0; i < sendingThread; i++)
        {
            int q = i/AppConstant.Multiple;
            SendSMS s = new SendSMS(i, this.qList[q], this.operatorInfos, this.dList);
            new Thread(s).start();
        }

        for(i = 0; i < AppConstant.NumberOfDeliveryThreads; i++)
        {
            CallDeliveryURL d = new CallDeliveryURL(i, this.dList);
            new Thread(d).start();
        }
    }
}
