/**
 * Created by Raiyan on 21-Aug-17.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class Configuration
{
    Properties prop = new Properties();

    public Configuration()
    {
        try {

            prop.load(new FileInputStream("config.cfg"));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public String getProperty(String key)
    {
        String value = this.prop.getProperty(key);
        return value;
    }
}
