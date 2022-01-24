package horrorsoft.com.f1abctimertelemetry;

import java.io.Serializable;

/**
 * Created by Alexey on 01.04.2016.
 *
 */
public class GpsData implements Serializable {
    public GpsData(float latitude, float longitude, boolean isFlightMode){
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFlightMode = isFlightMode;
    }

    public float latitude;
    public float longitude;
    public boolean isFlightMode;

}
