import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by Raiyan on 21-Aug-17.
 */
public class SendSMS implements Runnable {
    ArrayBlockingQueue<SMS> sharedQueue;
    LinkedBlockingQueue<SMS> dqList;
    int threadNo, qNo;
    String query;
    private Statement statement = null;
    private ResultSet resultSet = null;
    public Connection connection = null;
    private LogWrapper logWrapper;
    HashMap<Integer, OperatorInfo> operatorInfos;
    public static ControlTPS[] tpsList = new ControlTPS[AppConstant.NumberOfPickThreads];
    public SendSMS(int tno, ArrayBlockingQueue<SMS> q, HashMap<Integer, OperatorInfo> operators, LinkedBlockingQueue<SMS> dl) throws SQLException {
        this.threadNo = tno;
        this.sharedQueue = q;
        this.connection = new DatabaseConnection().getConnection();
        this.operatorInfos = operators;
        this.logWrapper = new LogWrapper(SendSMS.class.getName());
        this.dqList = dl;
        this.qNo = this.threadNo / AppConstant.Multiple;
        for(int i = 0; i < AppConstant.NumberOfPickThreads; i++)
        {
            if(tpsList[i] == null) tpsList[i] = new ControlTPS();
        }
    }

    @Override
    public void run() {
        while( true )
        {
            try {
                SMS info = sharedQueue.take();
                this.tryToSend(info);
            } catch (NullPointerException e) {
                this.logWrapper.error(e);
            } catch (InterruptedException e) {
                this.logWrapper.error(e);
            } catch (IOException e) {
                this.logWrapper.error(e);
            } catch (SQLException e) {
                this.logWrapper.error("err + "+ this.query, e);
            } catch (Exception e) {
                this.logWrapper.error(e);
            } catch (Throwable e) {
                this.logWrapper.error(e);
            }
        }
    }

    public void tryToSend(SMS info) throws IOException, SQLException, InterruptedException, NullPointerException {
        String link = "";
        String response = null;
        Date reqTime = new Date();
        SimpleDateFormat date = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss:SSS");
        try {
            if(this.operatorInfos.get(info.destAccount) == null)
            {
                this.updateFailed(info.msgID, "FAILED", "info not found in operator table (null returned)");
                return ;
            }
            link = this.operatorInfos.get(info.destAccount).MTURL;
            if(link==null||info.dstMN==null||info.srcMN==null||info.MsgType==null||info.msg==null||link.isEmpty()||info.dstMN.isEmpty()||info.srcMN.isEmpty()||info.MsgType.isEmpty()||info.msg.isEmpty())
            {
                this.updateFailed(info.msgID, "FAILED", "URL or some SMS info found null in SendSMS");
                return ;
            }
            link = link.replace("%mn", info.dstMN.trim());
            link = link.replace("%sc",  URLEncoder.encode(info.srcMN, "UTF-8"));


            link = link.replace("%msgid", Long.toString(info.msgID));
            link = link.replace("%st", info.MsgType.trim());
            String msg;
            msg = URLEncoder.encode(info.msg, "UTF-8");
            link = link.replace("%msg", msg);
            link = link.replace("%sd", URLEncoder.encode(info.ServiceID, "UTF-8"));

            reqTime = new Date();
            if(AppConstant.Check_TPS_Enable == 1) {
                this.checkTPS();
            }
            URL url = new URL(link);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(AppConstant.ResponseTimeout);
            conn.connect();
            int responseCode = conn.getResponseCode();
            if(responseCode == 200)
            {
                BufferedReader br = new BufferedReader(new InputStreamReader( conn.getInputStream()));
                response = br.readLine();
            }
            else
            {
                response = "FAILED|HTTP error "+ responseCode;
            }
        } catch (MalformedURLException e) {
            response = "FAILED|URL error";
            this.logWrapper.error("err + "+ link , e);
        } catch (NullPointerException e) {
            response = "FAILED|null pointer error in tryToSend()";
            link = "";
            this.logWrapper.error(e);
        } catch(Exception e) {
            this.logWrapper.error(e);
        } catch(Throwable e) {
            this.logWrapper.error(e);
        }

        Date resTime = new Date();
        if(response == null)
        {
            response = "FAILED|blank response";
        }
        else if(response.isEmpty())
        {
            response = "FAILED|blank response";
        }
        else {
            response = response.trim();
        }
        String remarks="";
        String[] tokens = response.split("\\|");
        String status=tokens[0];
        if(tokens.length >= 2)
            remarks=tokens[1];
        remarks=remarks.replace("'", "\\'");
        //throw new StackOverflowError();
        this.logWrapper.info(info.msgID+" : "+link + " ### " + date.format(reqTime) + " ### " + response + " ### " + date.format(resTime));

        if(remarks.length() > 200)
        {
            remarks = remarks.substring(0, 200);
        }

        if(status.toUpperCase().equals("SENT") || status.toUpperCase().equals("SUBMITTED") || status.toUpperCase().equals("DELIVERED"))
        {
            if(remarks.length() > 50)
            {
                remarks = remarks.substring(0, 50);
            }
            this.updateSuccess(info.msgID, status, remarks) ;
            if(info.DeliveryNotificationURL == null)
            {
                return ;
            }
            if(!status.toUpperCase().equals("DELIVERED") && info.DeliveryNotify.equals("ENABLED") && !info.DeliveryNotificationURL.isEmpty())
            {
                info.msgStatus = status;
                info.TransactionID = remarks;
                info.nextTryTime = (System.currentTimeMillis() / 1000) + 1*60; // set to now + 1 min
                this.dqList.put(info);
            }
        }
        else if(status.toUpperCase().equals("FAILED") || status.toUpperCase().equals("CHARGED") || status.toUpperCase().equals("CHARGEDFAILED"))
        {
            this.updateFailed(info.msgID, status, remarks) ;
        }
        else
        {
            status = "FAILED";
            remarks = "No valid status found!!";
            this.updateFailed(info.msgID, status, remarks) ;
        }
    }

    public void updateFailed(long msgid, String status, String remarks) throws SQLException {
        this.connection = DatabaseConnection.checkValidity(this.connection);
        query = "UPDATE "+AppConstant.Outbox_Table+" SET msgStatus = '"+
                status + "', Remarks = '"+
                remarks + "', sentTime = NOW(), schedule = DATE_ADD( schedule,INTERVAL "+
                AppConstant.RetryInterval +" MINUTE), "+
                "retryCount = retryCount+1 "+
                " WHERE msgID = "+ msgid;
        statement = connection.createStatement();

        statement.executeUpdate(query);
        statement.close();
    }

    public void updateSuccess(long msgid, String status, String tid) throws SQLException {
        this.connection = DatabaseConnection.checkValidity(this.connection);
        query = "UPDATE "+AppConstant.Outbox_Table+" SET msgStatus = '"+
                status + "', TransactionID = '"+
                tid + "',Remarks='', sentTime = NOW() "+
                " WHERE msgID = "+ msgid;
        statement = connection.createStatement();
        statement.executeUpdate(query);
        statement.close();
    }

    public void checkTPS() throws InterruptedException {
        while(true) {
            boolean overTPS = false;
            synchronized (SendSMS.tpsList[qNo]) {
                long currentSecond = System.currentTimeMillis()/1000;
                if (SendSMS.tpsList[qNo].getCount(currentSecond) > AppConstant.tps[qNo]) {
                    overTPS = true;
                }
                else
                {
                    SendSMS.tpsList[qNo].updateCount(currentSecond);
                    break;
                }
            }
            if(overTPS) Thread.sleep(10);
        }
    }

}
