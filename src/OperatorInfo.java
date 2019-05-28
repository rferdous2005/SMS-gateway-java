import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Raiyan on 22-Aug-17.
 */
public class OperatorInfo {
    String MTURL, ChargingURL, ChargingType;
    int id, tps, BulkSize;

    public void setValues(ResultSet rs) throws SQLException {
        this.MTURL = rs.getString("MTURL");
        this.ChargingURL = rs.getString("ChargingURL");
        this.ChargingType = rs.getString("ChargingType");

        this.tps = rs.getInt("tps");
        this.BulkSize = rs.getInt("BulkSize");
        this.id = rs.getInt("id");
    }
}
