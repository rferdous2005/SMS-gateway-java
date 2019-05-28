import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.ArrayList;


/**
 * Created by Raiyan on 21-Aug-17.
 */
public class Pick implements Runnable {
    int threadNo;
    ArrayBlockingQueue<SMS> sharedQueue;
    private Statement statement = null;
    private ResultSet resultSet = null;
    public Connection connection;
    private LogWrapper logWrapper;
    String query;
    public Pick(int tno, ArrayBlockingQueue<SMS> bq) throws SQLException {
        this.threadNo = tno;
        sharedQueue = bq;
        this.connection = new DatabaseConnection().getConnection();
        this.logWrapper = new LogWrapper(Pick.class.getName());
    }

    @Override
    public void run()
    {

        while(true)
        {
            try {
                this.fetch();
            } catch (SQLException e) {
                this.logWrapper.error("Err + "+this.query, e);
            } catch (InterruptedException e) {
                this.logWrapper.error(e);
            } catch (NullPointerException e) {
                this.logWrapper.error(e);
            } catch (Exception e) {
                this.logWrapper.error(e);
            } catch (Throwable e) {
                this.logWrapper.error(e);
            }
        }
    }

    public void fetch() throws SQLException, InterruptedException, NullPointerException {
        query = "select * from "+AppConstant.Outbox_Table+" join serviceconfig on "+AppConstant.Outbox_Table+".ServiceID = serviceconfig.ServiceID where "+
                AppConstant.conditions[this.threadNo] +
                " AND msgStatus in ('QUE','FAILED') and retryCount < "+ AppConstant.MaxRetry +
                " AND schedule < NOW() ORDER BY serviceconfig.`Priority`"+
                " limit "+ AppConstant.PickupNo;
        this.logWrapper.debug(query);
        this.connection = DatabaseConnection.checkValidity(this.connection);
        statement = connection.createStatement();
        resultSet = statement.executeQuery(query);
        ArrayList<SMS> tempList = new ArrayList<SMS>(AppConstant.ProcessingBatch);
        ArrayList<Long> tempIdList = new ArrayList<Long>(AppConstant.ProcessingBatch);
        int totalRow = 0;
        while(resultSet.next())
        {
            totalRow++;
            SMS sms = new SMS();
            sms.wrapSMS(resultSet);
            tempList.add(sms);
            tempIdList.add(sms.msgID);

            if(tempIdList.size() == AppConstant.ProcessingBatch)
            {
                int row = this.takeToProcessing(tempIdList);
                if(row >= 0) {
                    this.pushToQueue(tempList, sharedQueue);
                } else {
                    this.logWrapper.error("Pick.fetch(): Error in taking to processing status");
                    return ;
                }
                tempIdList.clear();
                tempList.clear();
            }
        }
        if(tempIdList.size() < AppConstant.ProcessingBatch && (!tempIdList.isEmpty()))
        {
            int row = this.takeToProcessing(tempIdList);

            if(row >= 0) {
                try {
                    this.pushToQueue(tempList, sharedQueue);
                }
                catch(InterruptedException e)
                {
                    this.logWrapper.error(e);
                }
                catch(Exception e)
                {
                    this.logWrapper.error(e);
                }
                catch(Throwable e)
                {
                    this.logWrapper.error(e);
                }
            } else {
                this.logWrapper.error("Pick.fetch(): Error in taking to processing status + tempIdList.size() < batch");
                return ;
            }
        }
        statement.close();
        resultSet.close();

        if(totalRow == 0)
        {
            System.out.println("No data");
            Thread.sleep(AppConstant.ThreadSleepTime);
        }
    }

    public void pushToQueue (ArrayList<SMS> list, ArrayBlockingQueue<SMS> q) throws InterruptedException {
        for(int i = 0; i < list.size(); i++)
        {
            q.put(list.get(i));
        }
    }
    public int takeToProcessing(ArrayList<Long> list) throws NullPointerException {
        String inClause = list.toString();
        inClause = inClause.substring(1, inClause.length()-1);
        inClause = "( "+inClause+" )";
        String QueryString = "update "+AppConstant.Outbox_Table+" set msgStatus='PROCESSING' where msgID IN "
                + inClause;
        try {
            this.connection = DatabaseConnection.checkValidity(this.connection);

            statement = connection.createStatement();
            int affectedRows = statement.executeUpdate(QueryString);
            statement.close();
            return affectedRows;

        } catch (SQLException exp) {
            this.logWrapper.error("sqlerr + "+QueryString, exp);
            return -1;
        } catch (Exception exp) {
            this.logWrapper.error("err + "+QueryString, exp);
            return -1;
        }
    }
}
