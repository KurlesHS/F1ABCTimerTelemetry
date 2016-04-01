package horrorsoft.com.f1abctimertelemetry;

import android.content.Context;
import android.widget.Toast;
import horrorsoft.com.f1abctimertelemetry.bluetooth.BluetoothDevice;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothDataListener;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Alexey on 01.04.2016.
 *
 */
@EBean(scope = EBean.Scope.Singleton)
class TelemetryModel implements IBluetoothDataListener{

    @RootContext
    protected Context context;

    private BluetoothDevice device = null;

    public boolean isOpen() {
        return device != null && device.isOpen();
    }

    public void open(String macAddress) {
        if (device == null) {
            device = new BluetoothDevice(macAddress);
            device.addBluetoothListener(this);
            device.start();
        }
    }

    void close() {
        if (device != null) {
            device.close();
            device.removeBluetoothListener(this);
            device = null;
            disconnected();
        }
    }

    @Override
    public void readyRead() {
        // Toast.makeText(context, "Ready read", Toast.LENGTH_SHORT).show();
        int len = device.bytesAvailable();
        if (len >= 17) {
            byte buff[] = new byte[len];
            device.read(buff);
            byte bytes[] = new byte[4];
            System.arraycopy(buff, 3, bytes, 0, 4);
            float f = (float) (ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() / 1000000.);
            System.arraycopy(buff, 7, bytes, 0, 4);
            float f2 = (float) (ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt() / 1000000.);

            Toast.makeText(context, String.format("Ready read: %d %f, %f",(int)buff[2], f, f2), Toast.LENGTH_SHORT).show();
            // device.write(buff);
            sendCommand();
        }
    }

    @Background(delay = 2000)
    protected void sendCommand() {
        if (device != null) {
            device.write(getCommand((byte) 0xfe));
        }
    }

    @Override
    public void connected() {
        Toast.makeText(context, "connected", Toast.LENGTH_SHORT).show();
        sendCommand();

    }

    @Override
    public void disconnected() {
        Toast.makeText(context, "disconnected", Toast.LENGTH_SHORT).show();
        if (device != null) {
            device.removeBluetoothListener(this);
            device = null;
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

}
