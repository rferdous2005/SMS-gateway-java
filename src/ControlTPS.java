/**
 * Created by Raiyan on 06-Sep-17.
 */
public class ControlTPS {
    long second = System.currentTimeMillis()/1000;
    int TPScount = 0;

    public void updateCount(long time)
    {
        if(second < time)
        {
            second = time;
            TPScount = 1;
        }
        else
        {
            TPScount++;
        }
    }

    public int getCount(long time)
    {
        if(second < time)
        {
            updateCount(time);
        }
        return TPScount;
    }
}
