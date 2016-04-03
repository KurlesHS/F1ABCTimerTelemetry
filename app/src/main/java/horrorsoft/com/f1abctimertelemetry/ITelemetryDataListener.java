package horrorsoft.com.f1abctimertelemetry;

/**
 * Created by Alexey on 02.04.2016.
 *
 */
interface ITelemetryDataListener {
    class TelemetryData {
        public byte ch1;
        public byte ch2;
        public int height;
        public float speed;
        public float temperature;
        public boolean dtFlag;
        public boolean rdtFlag;
        public float voltage;
        public int timeToDt;
        public int prediction;
        public boolean blinkerOnFlag;
        public boolean servoOnFlag;
        public byte act;
        public byte reservedA;
        public byte reservedD;
        public byte rssi;
        public float pwr;
        public boolean hasError;
    }

    void result(TelemetryData telemetryData);
}
