/**
 * Created by Raiyan on 21-Aug-17.
 */

import java.util.TimeZone;

public class AppConstant {
    public static String DB_IP;
    public static String DB_User;
    public static String DB_Name;
    public static String DB_Pass;
    public static String DB_Extra_Config;
    public static String Outbox_Table;

    public static int NumberOfPickThreads;
    public static int Multiple;
    public static int PickupNo;
    public static int DB_Port;
    public static int ProcessingBatch;
    public static int RetryInterval;
    public static int DeliveryRetryInterval;
    public static int LogEnable;
    public static int LogLevel;
    public static int ResponseTimeout;
    public static int MaxRetry;
    public static int DeliveryMaxRetry;
    public static int NumberOfDeliveryThreads;
    public static int ThreadSleepTime;
    public static int Check_TPS_Enable;
    public static String[] conditions;
    public static int[] tps;
    public static boolean loadConfiguration(Configuration cfg)
    {
        try {
            AppConstant.DB_IP = cfg.getProperty("DB_IP");
            AppConstant.DB_Pass = cfg.getProperty("DB_Pass");
            AppConstant.DB_User = cfg.getProperty("DB_User");
            AppConstant.DB_Name = cfg.getProperty("DB_Name");
            AppConstant.DB_Extra_Config = cfg.getProperty("DB_Extra_Config");
            AppConstant.Outbox_Table = cfg.getProperty("Outbox_Table");
            conditions = cfg.getProperty("ThreadConditions").split("\\|");
            String[] tpsString = cfg.getProperty("MaxTPS").split("\\|");

            AppConstant.NumberOfPickThreads = Integer.parseInt(cfg.getProperty("NumberOfPickThreads"));
            AppConstant.NumberOfDeliveryThreads = Integer.parseInt(cfg.getProperty("NumberOfDeliveryThreads"));
            AppConstant.Multiple = Integer.parseInt(cfg.getProperty("Multiple"));
            AppConstant.PickupNo = Integer.parseInt(cfg.getProperty("PickupNo"));
            AppConstant.DB_Port = Integer.parseInt(cfg.getProperty("DB_Port"));
            AppConstant.ProcessingBatch = Integer.parseInt(cfg.getProperty("ProcessingBatch"));
            AppConstant.RetryInterval = Integer.parseInt(cfg.getProperty("RetryInterval"));
            AppConstant.DeliveryRetryInterval = Integer.parseInt(cfg.getProperty("DeliveryRetryInterval"));
            AppConstant.LogEnable = Integer.parseInt(cfg.getProperty("LogEnable"));
            AppConstant.LogLevel = Integer.parseInt(cfg.getProperty("LogLevel"));
            AppConstant.ResponseTimeout = Integer.parseInt(cfg.getProperty("ResponseTimeout"));
            AppConstant.MaxRetry = Integer.parseInt(cfg.getProperty("MaxRetry"));
            AppConstant.DeliveryMaxRetry = Integer.parseInt(cfg.getProperty("DeliveryMaxRetry"));
            AppConstant.ThreadSleepTime = Integer.parseInt(cfg.getProperty("ThreadSleepTime"));
            AppConstant.Check_TPS_Enable = Integer.parseInt(cfg.getProperty("Check_TPS_Enable"));
            if(conditions.length != NumberOfPickThreads)
            {
                System.err.println("Error in config: ThreadConditions: "+conditions.length);
                return false;
            }
            if(tpsString.length != NumberOfPickThreads)
            {
                System.err.println("Error in config: MaxTPS: "+tpsString.length);
                return false;
            }
            tps = new int[NumberOfPickThreads];
            for(int i = 0; i < NumberOfPickThreads; i++)
            {
                tps[i] = Integer.parseInt(tpsString[i].trim());
            }
            return true;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }
}

