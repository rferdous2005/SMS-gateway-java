import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
/**
 * Created by Raiyan on 28-Aug-17.
 */
public class CallDeliveryURL implements Runnable {
    int threadNo;
    LinkedBlockingQueue<SMS> myList;
    LogWrapper logWrapper;
    private Statement statement = null;
    private ResultSet resultSet = null;
    public Connection connection;
    String query;
    public CallDeliveryURL(int n, LinkedBlockingQueue<SMS> list) throws SQLException {
        this.threadNo = n;
        this.myList = list;
        this.logWrapper = new LogWrapper(CallDeliveryURL.class.getName());
        this.connection = new DatabaseConnection().getConnection();
    }

    @Override
    public void run()
    {
        while(true)
        {
            try {
                while(this.myList.isEmpty())
                {
                    Thread.sleep(AppConstant.ThreadSleepTime);
                }
                this.callURL();
            } catch(InterruptedException e) {
                this.logWrapper.error(e);
            } catch(Exception e) {
                this.logWrapper.error(e);
            }
        }
    }

    public void callURL() throws InterruptedException {
        SMS dsms = this.myList.take();

        URL url = null;
        HttpURLConnection conn = null;
        int responseCode;
        String response = "", link = "";
        try {
            long now = System.currentTimeMillis() / 1000;
            if(dsms.nextTryTime > now && dsms.deliveryRetried < AppConstant.DeliveryMaxRetry)
            {
                this.myList.put(dsms);
                return ;
            }
            link = dsms.DeliveryNotificationURL;
            if(dsms.refID==null)
                dsms.refID = "";
            if(dsms.TransactionID==null)
                dsms.TransactionID = "";

            link = link.replace("%mn", dsms.dstMN.trim());
            link = link.replace("%sc",  URLEncoder.encode(dsms.srcMN.trim(), "UTF-8"));


            link = link.replace("%msgid", Long.toString(dsms.msgID));
            link = link.replace("%rid", dsms.refID.trim());

            link = link.replace("%tid", dsms.TransactionID);
            url = new URL(link);

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(AppConstant.ResponseTimeout);
            conn.connect();
            responseCode = conn.getResponseCode();
            if(responseCode == 200)
            {
                BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream()));
                response = br.readLine();
            }
        } catch (MalformedURLException e) {
            this.logWrapper.error("err + "+url , e);
            response = "FAILED|URL error";
        } catch (NullPointerException e) {
            response = "FAILED|null pointer error in callURL";
            this.logWrapper.error(e);
        } catch (IOException e) {
            //response = "FAILED|I/O error in callURL";
            this.logWrapper.error(e);
        } catch(Exception e) {
            //response = "FAILED|default error in callURL";
            this.logWrapper.error(e);
        }
        String remarks="", status="";
        boolean reinsert = false;

        if(!(response==null || response.isEmpty()))
        {
            String[] tokens = response.split("\\|");
            status = tokens[0];
            if (tokens.length >= 2)
                remarks = tokens[1];
            remarks = remarks.replace("'", "\\'");
            //remarks = remarks.substring(0, 50);
            if(dsms.msgStatus.equals(status))
            {
                dsms.deliveryRetried++;
                dsms.nextTryTime += (AppConstant.DeliveryRetryInterval*60);
                reinsert = true;
            }
            else if(status.toUpperCase().equals("DELIVERED") || status.toUpperCase().equals("FAILED") || status.toUpperCase().equals("SUBMITTED") || status.toUpperCase().equals("SENT"))
            {
                if(remarks.length() > 200)
                {
                    remarks = remarks.substring(0, 200);
                }
                try {
                    this.logWrapper.info("For message ID: "+dsms.msgID+" ### "+link +" ### "+ response);
                    this.finalUpdate(dsms.msgID, status, remarks);
                    reinsert = false;
                } catch (SQLException e) {
                    dsms.nextTryTime += (AppConstant.DeliveryRetryInterval*60);
                    reinsert = true;
                    this.logWrapper.error(e);
                } catch (Exception e) {
                    this.logWrapper.error(e);
                }

            }

        }
        else
        {
            dsms.deliveryRetried++;
            dsms.nextTryTime += (AppConstant.DeliveryRetryInterval*60);
            reinsert = true;
        }
        if(reinsert && dsms.deliveryRetried < AppConstant.DeliveryMaxRetry)
        {
            this.myList.put(dsms);
        }
    }

    public void finalUpdate(long id, String s, String r) throws SQLException {
        this.connection = DatabaseConnection.checkValidity(this.connection);
        if(s.equals("FAILED"))
        {
            this.query = "UPDATE "+AppConstant.Outbox_Table+" SET msgStatus = '"+
                    s + "', Remarks = '"+
                    r + "', sentTime = NOW(), schedule = DATE_ADD( schedule,INTERVAL "+
                    AppConstant.RetryInterval +" MINUTE), "+
                    "retryCount = retryCount+1 "+
                    " WHERE msgID = "+ id;
        }
        else
        {
            this.query = "UPDATE "+AppConstant.Outbox_Table+" SET msgStatus = '"+
                    s + "', Remarks = '"+
                    r + "', sentTime = NOW() "+
                    " WHERE msgID = "+ id;
        }
        this.statement = this.connection.createStatement();
        this.statement.executeUpdate(query);
        this.statement.close();
    }
}
