package horrorsoft.com.f1abctimertelemetry;

import android.content.Context;
import android.widget.Toast;
import horrorsoft.com.f1abctimertelemetry.bluetooth.BluetoothDevice;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothDataListener;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothStatusListener;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Alexey on 01.04.2016.
 *
 */
@EBean(scope = EBean.Scope.Singleton)
class TelemetryModel implements IBluetoothDataListener, IBluetoothStatusListener {
    private static final int IDLE = 0;
    private static final int TELEMETRY_RESPONSE = 1;
    private static final int GPS_RESPONSE = 2;

    private List<GpsData> mRoute = new LinkedList<>();

    private int mCurrentMode = IDLE;
    private IGpsDataListener mGpsDataListener = null;
    private ITelemetryDataListener mTelemetryDataListener = null;

    private List<IBluetoothStatusListener> mBluetToothListeners = new LinkedList<>();

    @RootContext
    protected Context context;

    private BluetoothDevice device = null;

    private GpsData mLastGpsPoint = null;

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
                telemetryData.pwr = ((float)(buff[0x0f] & 0xff) + 100f) * 0.01f;
                telemetryData.rdtFlag = (firstInt & 0x01) != 0;
                telemetryData.dtFlag = (firstInt & 0x02) != 0;
                telemetryData.temperature = (float)t * 0.1f - 20f;
                telemetryData.height = h - 0x40;
                telemetryData.speed = (float)v * 0.01f;
                telemetryData.voltage = (float)((secondInt >>> 22) & 0x03ff) * 0.01f;
                telemetryData.timeToDt = (secondInt >>> 12) & 0x03ff;
                telemetryData.prediction = (secondInt >>> 2) & 0x03ff;
                telemetryData.blinkerOnFlag = (secondInt & 0x0002) != 0;
                telemetryData.servoOnFlag = (secondInt & 0x0001) != 0;
            }
        }

        if (mTelemetryDataListener != null) {
            mTelemetryDataListener.result(telemetryData);
        }

        sendGetGpsDataCommand();
    }

    private void handleGpsDataResponse(byte[] buff) {

        byte crc8 = calculateCrc8(buff, 0x0b);
        if (crc8 != buff[0x0b]) {
            return;
        }

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

        sendGetTelemetryDataCommand();
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
                    break;
                case GPS_RESPONSE:
                    handleGpsDataResponse(buff);
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
}
