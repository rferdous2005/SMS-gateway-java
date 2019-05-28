import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
/**
 * Created by Raiyan on 21-Aug-17.
 */
public class SMS {
    String srcMN, dstMN, msg, msgStatus, MsgType, schedule, Remarks, TransactionID, ServiceID;
    String DeliveryNotify, DeliveryNotificationURL, refID;
    long msgID, nextTryTime;
    int retryCount,destAccount, deliveryRetried = 0;

    public void wrapSMS(ResultSet r) throws SQLException
    {
        this.dstMN = r.getString("dstMN");
        this.srcMN = r.getString("srcMN");
        this.msg = r.getString("msg");
        this.msgStatus = r.getString("msgStatus");
        this.MsgType = r.getString("MsgType");
        this.TransactionID = r.getString("TransactionID");
        this.schedule = r.getString("schedule");
        this.Remarks = r.getString("Remarks");
        this.ServiceID = r.getString("ServiceID");
        this.DeliveryNotify = r.getString("DeliveryNotify");
        this.DeliveryNotificationURL = r.getString("DeliveryNotificationURL");
        this.refID = r.getString("refID");

        this.msgID =  r.getLong("msgID") ;
        this.retryCount =  r.getInt("retryCount");
        this.destAccount = Integer.parseInt( r.getString("destAccount") );
        this.nextTryTime = System.currentTimeMillis() / 1000;

    }
}
