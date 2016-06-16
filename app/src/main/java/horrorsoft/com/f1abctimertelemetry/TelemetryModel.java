package horrorsoft.com.f1abctimertelemetry;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;
import horrorsoft.com.f1abctimertelemetry.bluetooth.BluetoothDevice;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothDataListener;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothStatusListener;
import org.androidannotations.annotations.AfterInject;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alexey on 01.04.2016.
 * !
 */
@EBean(scope = EBean.Scope.Singleton)
class TelemetryModel implements IBluetoothDataListener, IBluetoothStatusListener, IGpsDataListener, LocationListener {
    private static final int IDLE = 0;
    private static final int TELEMETRY_RESPONSE = 1;
    private static final int GPS_RESPONSE = 2;

    private List<GpsData> mRoute = new LinkedList<>();

    private int mCurrentMode = IDLE;
    private IGpsDataListener mGpsDataListener = null;
    private ITelemetryDataListener mTelemetryDataListener = null;
    private Location mLastKnownPhoneLocation = null;

    private List<IBluetoothStatusListener> mBluetToothListeners = new LinkedList<>();

    public IPhoneGpsLocationListener getPhoneGpsLocationListener() {
        return mPhoneGpsLocationListener;
    }

    public void setPhoneGpsLocationListener(IPhoneGpsLocationListener phoneGpsLocationListener) {
        this.mPhoneGpsLocationListener = phoneGpsLocationListener;
    }

    private IPhoneGpsLocationListener mPhoneGpsLocationListener = null;

    private boolean mIsInInitializeGpsMode = false;

    @RootContext
    protected Context context;

    private BluetoothDevice device = null;

    private GpsData mLastGpsPoint = null;

    private Activity mCurrentActivity = null;

    LocationManager mLocationManager = null;

    @AfterInject
    void afterInject() {

    }

    TelemetryModel() {

    }

    void setGpsDataListener(IGpsDataListener mGpsDataListener) {
        this.mGpsDataListener = mGpsDataListener;
    }

    void setTelemetryDataListener(ITelemetryDataListener listener) {
        this.mTelemetryDataListener = listener;
    }

    public boolean isOpen() {
        return device != null && device.isOpen();
    }

    GpsData lastGpsPoint() {
        return mLastGpsPoint;
    }

    List<GpsData> route() {
        return mRoute;
    }

    boolean mIsPhoneGpsEnabled() {
        return mLocationManager != null;
    }

    void enableGpsTracking() {
        if (mLocationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, this);

            Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                mLastKnownPhoneLocation = location;
            }
        }
    }

    void disableGpsTracking() {
        if (mLocationManager != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mLocationManager.removeUpdates(this);
        }
    }

    boolean initializePhoneGps() {
        if (mCurrentActivity == null) {
            return false;
        }

        if (mLocationManager != null) {
            return true;
        }

        int permission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (!mIsInInitializeGpsMode) {
                mIsInInitializeGpsMode = true;
                ActivityCompat.requestPermissions(mCurrentActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            } else {
                mIsInInitializeGpsMode = false;
            }

            return false;
        }

        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mIsInInitializeGpsMode = false;
        Log.d("test", "location manager init ok");
        return true;
    }



    public void open(String macAddress) {
        if (device == null) {
            device = new BluetoothDevice(macAddress);
            device.addBluetoothDataListener(this);
            device.addBluetoothStatusListener(this);
            device.start();
        }
    }

    void close() {
        if (device != null) {
            device.close();
            device.removeBluetoothDataListener(this);
            device.addBluetoothStatusListener(this);
            device = null;
            disconnected();
        }
    }

    private void handleTelemetryResponse(byte[] buff) {
        int dataLen = buff.length;
        ITelemetryDataListener.TelemetryData telemetryData = new ITelemetryDataListener.TelemetryData();
        telemetryData.hasError = true;
        byte[] intBuffer = new byte[0x04];
        if (dataLen == 0x11) {
            byte crc8 = calculateCrc8(buff, 0x10);
            if (crc8 == buff[0x10]) {
                System.arraycopy(buff, 0x02, intBuffer, 0, 0x04);
                int firstInt = ByteBuffer.wrap(intBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
                System.arraycopy(buff, 0x06, intBuffer, 0, 0x04);
                int secondInt = ByteBuffer.wrap(intBuffer).order(ByteOrder.LITTLE_ENDIAN).getInt();
                int t = ((firstInt) >>> 2) & 0x3ff;
                int v = ((firstInt) >>> 12) & 0x3ff;
                int h = ((firstInt) >>> 22) & 0x3ff;
                telemetryData.hasError = false;
                telemetryData.ch1 = buff[0x00];
                telemetryData.ch2 = buff[0x01];
                telemetryData.reservedA = buff[0x0a];
                telemetryData.act = buff[0x0c];
                telemetryData.reservedD = buff[0x0d];
                telemetryData.rssi = buff[0x0e];
                telemetryData.pwr = ((float) (buff[0x0f] & 0xff) + 100f) * 0.01f;
                telemetryData.rdtFlag = (firstInt & 0x01) != 0;
                telemetryData.dtFlag = (firstInt & 0x02) != 0;
                telemetryData.temperature = (float) t * 0.1f - 20f;
                telemetryData.height = h - 0x40;
                telemetryData.speed = (float) v * 0.01f;
                telemetryData.voltage = (float) ((secondInt >>> 22) & 0x03ff) * 0.01f;
                telemetryData.timeToDt = (secondInt >>> 12) & 0x03ff;
                telemetryData.prediction = (secondInt >>> 2) & 0x03ff;
                telemetryData.blinkerOnFlag = (secondInt & 0x0002) != 0;
                telemetryData.servoOnFlag = (secondInt & 0x0001) != 0;
            }
        }

        if (mTelemetryDataListener != null) {
            mTelemetryDataListener.result(telemetryData);
        }
    }

    private void handleGpsDataResponse(byte[] buff) {

        byte crc8 = calculateCrc8(buff, 0x0b);
        if (crc8 == buff[0x0b]) {
            byte bytes[] = new byte[4];
            System.arraycopy(buff, 3, bytes, 0, 4);
            float latitude = (float) (ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() / 1000000.);
            System.arraycopy(buff, 7, bytes, 0, 4);
            float longitude = (float) (ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() / 1000000.);
            boolean isFlightMode = (int) buff[2] != 0;
            GpsData newPos = new GpsData(latitude, longitude, isFlightMode);

            if (mLastGpsPoint != null && mLastGpsPoint.longitude == newPos.longitude && mLastGpsPoint.latitude == newPos.latitude) {
                // те же данные - не засоряем данные. Хотя наверное это стоит делать в модели раз и не заморачиваться
                return;
            }

            if (mLastGpsPoint != null && !mLastGpsPoint.isFlightMode && newPos.isFlightMode) {
                // начало полёта
                mRoute.clear();
            }

            if (newPos.isFlightMode) {
                mRoute.add(newPos);
            }
            mLastGpsPoint = newPos;

            if (mGpsDataListener != null) {
                mGpsDataListener.newPosition(mLastGpsPoint);
            }
        }
    }

    @Override
    public void readyRead() {
        int len = device.bytesAvailable();
        if (len >= 17) {
            byte buff[] = new byte[len];
            device.read(buff);
            switch (mCurrentMode) {
                case TELEMETRY_RESPONSE:
                    handleTelemetryResponse(buff);
                    sendGetGpsDataCommand();
                    break;
                case GPS_RESPONSE:
                    handleGpsDataResponse(buff);
                    sendGetTelemetryDataCommand();
                    break;
                default:
                    break;
            }
            mCurrentMode = IDLE;
        }
    }

    @Background(delay = 500)
    void sendCommand(int command, int type) {
        if (device != null) {
            mCurrentMode = type;
            device.write(getCommand((byte) command));
        }
    }

    private void sendGetGpsDataCommand() {
        sendCommand(0xfe, GPS_RESPONSE);
    }

    private void sendGetTelemetryDataCommand() {
        sendCommand(0xff, TELEMETRY_RESPONSE);
    }

    public int azimuth()
    {
        double result = 0;
        if (hasAzimuth()) {
            // инфа взята с http://wiki.gis-lab.info/w/%D0%92%D1%8B%D1%87%D0%B8%D1%81%D0%BB%D0%B5%D0%BD%D0%B8%D0%B5_%D1%80%D0%B0%D1%81%D1%81%D1%82%D0%BE%D1%8F%D0%BD%D0%B8%D1%8F_%D0%B8_%D0%BD%D0%B0%D1%87%D0%B0%D0%BB%D1%8C%D0%BD%D0%BE%D0%B3%D0%BE_%D0%B0%D0%B7%D0%B8%D0%BC%D1%83%D1%82%D0%B0_%D0%BC%D0%B5%D0%B6%D0%B4%D1%83_%D0%B4%D0%B2%D1%83%D0%BC%D1%8F_%D1%82%D0%BE%D1%87%D0%BA%D0%B0%D0%BC%D0%B8_%D0%BD%D0%B0_%D1%81%D1%84%D0%B5%D1%80%D0%B5

            final  float rad = 6372795; // радиус земли
            /* python code
            lat1 = llat1*math.pi/180.
            lat2 = llat2*math.pi/180.
            long1 = llong1*math.pi/180.
            long2 = llong2*math.pi/180.
            */
            double lat1 = (double)mLastGpsPoint.latitude * Math.PI / 180.;
            double lat2 = mLastKnownPhoneLocation.getLatitude() * Math.PI / 180.;

            double long1 = (double)mLastGpsPoint.longitude * Math.PI / 180.;
            double long2 = mLastKnownPhoneLocation.getLongitude() * Math.PI / 180.;

            /*
            #косинусы и синусы широт и разницы долгот
            cl1 = math.cos(lat1)
            cl2 = math.cos(lat2)
            sl1 = math.sin(lat1)
            sl2 = math.sin(lat2)
            delta = long2 - long1
            cdelta = math.cos(delta)
            sdelta = math.sin(delta)
            */

            double cl1 = Math.cos(lat1);
            double cl2 = Math.cos(lat2);
            double sl1 = Math.sin(lat1);
            double sl2 = Math.sin(lat2);
            double delta = long2 - long1;
            double cdelta = Math.cos(delta);
            double sdelta = Math.sin(delta);

            /*
            #вычисления длины большого круга
            y = math.sqrt(math.pow(cl2*sdelta,2)+math.pow(cl1*sl2-sl1*cl2*cdelta,2))
            x = sl1*sl2+cl1*cl2*cdelta
            ad = math.atan2(y,x)
            dist = ad*rad
            */

            // вычисления длины большого круга
            double y = Math.sqrt(Math.pow(cl2*sdelta,2)+Math.pow(cl1*sl2-sl1*cl2*cdelta,2));
            double x = sl1*sl2+cl1*cl2*cdelta;
            double ad = Math.atan2(y,x);
            double dist = ad*rad;

            /*
            #вычисление начального азимута
            x = (cl1*sl2) - (sl1*cl2*cdelta)
            y = sdelta*cl2
            z = math.degrees(math.atan(-y/x))

            if (x < 0):
                z = z+180.

            z2 = (z+180.) % 360. - 180.
            z2 = - math.radians(z2)
            anglerad2 = z2 - ((2*math.pi)*math.floor((z2/(2*math.pi))) )
            angledeg = (anglerad2*180.)/math.pi
            */

            // вычисление начального азимута

            final double rad2deg = 180.0f/Math.PI;

            x = (cl1*sl2) - (sl1*cl2*cdelta);
            y = sdelta*cl2;
            double z = Math.atan(-y/x) * rad2deg;

            if (x < 0) {
                z = z + 180.;
            }

            double z2 = (z+180.) % 360. - 180.;
            z2 = - (z2 / rad2deg);

            double anglerad2 = z2 - ((2*Math.PI)*Math.floor((z2/(2*Math.PI))) );
            double angledeg = (anglerad2*180.)/Math.PI;


        }
        return (int)result;
    }

    public boolean hasAzimuth() {
        return mLastGpsPoint != null && mLastKnownPhoneLocation != null;
    }

    @Override
    public void connected() {
        Toast.makeText(context, "connected", Toast.LENGTH_SHORT).show();

        for (IBluetoothStatusListener listener : mBluetToothListeners) {
            listener.connected();
        }
        sendGetGpsDataCommand();
    }

    @Override
    public void disconnected() {
        Toast.makeText(context, "disconnected", Toast.LENGTH_SHORT).show();
        if (device != null) {
            device.removeBluetoothDataListener(this);
            device = null;
        }
        for (IBluetoothStatusListener listener : mBluetToothListeners) {
            listener.disconnected();
        }
    }

    private byte[] getCommand(byte cmd) {
        byte[] command = new byte[0x11];
        clearByteArray(command, (byte) 0x00);
        // timer_ch1 = 0x1f;
        // timer_ch2 = 0x2d;
        command[0] = (byte) 0x1f;
        command[1] = (byte) 0x2d;
        command[2] = cmd;
        command[0x10] = calculateCrc8(command, 0x10);
        return command;
    }

    private void clearByteArray(byte[] array, byte fillWith) {
        for (int i = 0; i < array.length; ++i) {
            array[i] = fillWith;
        }
    }

    private static byte calculateCrc8(byte[] array, int size) {
        if (size < 0) {
            size = array.length;
        }
        byte crc = (byte) 0xFF;
        for (int x = 0; x < size; ++x) {
            crc ^= array[x];
            for (int i = 0; i < 8; i++)
                crc = (crc & (byte) 0x80) != (byte) 0x00 ? (byte) ((crc << 1) ^ 0x31) : (byte) (crc << 1);
        }
        return crc;
    }

    void addBlueToothStatusListener(IBluetoothStatusListener listener) {
        mBluetToothListeners.add(listener);
    }

    void removeBlueToothStatusListener(IBluetoothStatusListener listener) {
        mBluetToothListeners.remove(listener);
    }

    @Override
    public void newPosition(GpsData newPos) {

    }

    public Activity getCurrentActivity() {
        return mCurrentActivity;
    }

    public Location lastKnownPhoneLocation() {
        return mLastKnownPhoneLocation;
    }

    public void setCurrentActivity(Activity mCurrentActivity) {
        this.mCurrentActivity = mCurrentActivity;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("test", String.format("%f %f", location.getLatitude(), location.getLongitude()));
        Toast.makeText(context, String.format("%f %f", location.getLatitude(), location.getLongitude()),
                Toast.LENGTH_SHORT).show();
        mLastKnownPhoneLocation = location;
        if (mPhoneGpsLocationListener != null) {
            mPhoneGpsLocationListener.newPhoneLocation(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("test", provider + status);
        Toast.makeText(context, provider + status,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("test", "enabled: " + provider);
        Toast.makeText(context, "enabled: " + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("test", "disabled: " + provider);
        Toast.makeText(context, "disabled: " + provider,
                Toast.LENGTH_SHORT).show();
    }
}
