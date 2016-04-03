package horrorsoft.com.f1abctimertelemetry.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by Alexey on 31.03.2016.
 * Class for read / write on bluetooth
 */
public class BluetoothDevice {

    private final static int READY_READ = 1;
    private final static int CONNECTED = 2;
    private final static int DISCONNECTED = 3;

    private BluetoothDevice.BluetoothThreadWorker threadWorker = null;

    private LinkedList<IBluetoothDataListener> mBluetoothDataListeners = new LinkedList<>();
    private LinkedList<IBluetoothStatusListener> mBluetoothStatusListeners = new LinkedList<>();

    public BluetoothDevice(String deviceMacAddress) {
        BluetoothDevice.MyHandler handler = new BluetoothDevice.MyHandler(this);
        threadWorker = new BluetoothDevice.BluetoothThreadWorker(deviceMacAddress, handler);
    }

    public void addBluetoothDataListener(IBluetoothDataListener listener) {
        mBluetoothDataListeners.add(listener);
    }

    public void removeBluetoothDataListener(IBluetoothDataListener listener) {
        mBluetoothDataListeners.remove(listener);
    }

    public void addBluetoothStatusListener(IBluetoothStatusListener listener) {
        mBluetoothStatusListeners.add(listener);
    }

    public void removeBluetoothStatusListener(IBluetoothStatusListener listener) {
        mBluetoothStatusListeners.remove(listener);
    }

    public void start() {
        threadWorker.start();
    }

    public boolean isOpen() {
        return threadWorker.isOpen();
    }

    public void write(byte[] bytes) {
        threadWorker.write(bytes);
    }

    public int read(byte[] bytes) {
        return threadWorker.read(bytes);
    }

    public int bytesAvailable() {
        return threadWorker.bytesAvailable();
    }

    public void close() {
        threadWorker.close();
    }


    private void handleMessage(Message msg) {
        switch (msg.what) {
            case CONNECTED:
                informAboutConnectedState();
                break;
            case DISCONNECTED:
                informAboutDisconnectedState();
                break;
            case READY_READ:
                informAboutReadyReadState();
                break;
            default:
                break;
        }
    }

    private void informAboutConnectedState() {
        for (IBluetoothStatusListener listener : mBluetoothStatusListeners) {
            listener.connected();
        }
    }

    private void informAboutDisconnectedState() {
        for (IBluetoothStatusListener listener : mBluetoothStatusListeners) {
            listener.disconnected();
        }
    }

    private void informAboutReadyReadState() {
        for (IBluetoothDataListener listener : mBluetoothDataListeners) {
            listener.readyRead();
        }
    }

    private static class MyHandler extends Handler {

        //Using a weak reference means you won't prevent garbage collection
        private final WeakReference<BluetoothDevice> myClassWeakReference;

        MyHandler(BluetoothDevice myClassInstance) {
            super(Looper.getMainLooper());
            myClassWeakReference = new WeakReference<>(myClassInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            BluetoothDevice myClass = myClassWeakReference.get();
            if (myClass != null) {
                myClass.handleMessage(msg);
            }
            super.handleMessage(msg);
        }
    }

    private class BluetoothThreadWorker extends Thread {
        private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private String mDeviceMacAddress;
        private BluetoothDevice.MyHandler mHandler;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothSocket mBtSocket = null;
        private boolean connectionStatus = true;
        private OutputStream mOutStream = null;
        private InputStream mInputStream = null;
        private byte[] mReadBuffer = new byte[1024];
        private byte[] mWriteBuffer = new byte[1024];
        private int mReadOffset = 0;
        private int mWriteOffset = 0;
        private boolean mExitRequest;
        private boolean mIsConnected;
        private final Object mWriteSync = new Object();
        private final Object mReadSync = new Object();


        BluetoothThreadWorker(String deviceMacAddress, BluetoothDevice.MyHandler handler) {
            mHandler = handler;
            mDeviceMacAddress = deviceMacAddress;
            mBluetoothAdapter = null;
            mIsConnected = false;
            mExitRequest = false;
        }

        boolean isOpen() {
            synchronized (mReadSync) {
                return mIsConnected;
            }
        }

        void write(byte[] bytes) {
            synchronized (mWriteSync) {
                System.arraycopy(bytes, 0, mWriteBuffer, mWriteOffset, bytes.length);
                mWriteOffset += bytes.length;
            }
        }

        int read(byte[] bytes) {
            synchronized (mReadSync) {
                int bytesToRead = Math.min(mReadOffset, bytes.length);
                System.arraycopy(mReadBuffer, 0, bytes, 0, bytesToRead);
                mReadOffset -= bytesToRead;
                if (mReadOffset > 0) {
                    System.arraycopy(mReadBuffer, bytesToRead, mReadBuffer, 0, mReadOffset);
                }
                return bytesToRead;
            }
        }

        int bytesAvailable() {
            synchronized (mReadSync) {
                return mReadOffset;
            }
        }

        void close() {
            synchronized (mReadSync) {
                mExitRequest = true;
            }
        }

        @Override
        public void run() {
            synchronized (mReadSync) {
                mExitRequest = false;
                mIsConnected = false;
            }
            try {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                android.bluetooth.BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceMacAddress);

                // We need two things before we can successfully connect
                // (authentication issues aside): a MAC address, which we
                // already have, and an RFCOMM channel.
                // Because RFCOMM channels (aka ports) are limited in
                // number, Android doesn't allow you to use them directly;
                // instead you request a RFCOMM mapping based on a service
                // ID. In our case, we will use the well-known SPP Service
                // ID. This ID is in UUID (GUID to you Microsofties)
                // format. Given the UUID, Android will handle the
                // mapping for you. Generally, this will return RFCOMM 1,
                // but not always; it depends what other BlueTooth services
                // are in use on your Android device.
                try {
                    mBtSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                } catch (IOException e) {
                    connectionStatus = false;
                }
            } catch (IllegalArgumentException e) {
                connectionStatus = false;
            }
            mBluetoothAdapter.cancelDiscovery();

            try {
                mBtSocket.connect();
            } catch (IOException e1) {
                connectionStatus = false;
                try {
                    mBtSocket.close();
                } catch (IOException e2) {
                    connectionStatus = false;
                }
            }

            try {
                mOutStream = mBtSocket.getOutputStream();
                mInputStream = mBtSocket.getInputStream();
            } catch (IOException e2) {
                connectionStatus = false;
            }

            if (!connectionStatus) {
                try {
                    mBtSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mHandler.obtainMessage(DISCONNECTED).sendToTarget();
                return;
            }

            try {
                synchronized (mReadSync) {
                    mIsConnected = true;
                }
                mHandler.obtainMessage(CONNECTED).sendToTarget();
                byte[] buffer = new byte[1024];
                byte[] buffer2 = new byte[1024];
                while (true) {
                    synchronized (mReadSync) {
                        if (mExitRequest) {
                            break;
                        }
                    }

                    if (mInputStream.available() > 0) {
                        int read = mInputStream.read(buffer);
                        boolean readyRead = false;
                        synchronized (mReadSync) {
                            int maxBytes = mReadBuffer.length - mReadOffset;
                            int toRead = Math.min(read, maxBytes);
                            if (toRead > 0) {
                                System.arraycopy(buffer, 0, mReadBuffer, mReadOffset, toRead);
                                mReadOffset += toRead;
                                readyRead = true;
                            }
                        }
                        if (readyRead) {
                            mHandler.obtainMessage(READY_READ).sendToTarget();
                        }
                    }

                    int bytesToWrite = 0;
                    synchronized (mWriteSync) {
                        if (mWriteOffset > 0) {
                            System.arraycopy(mWriteBuffer, 0, buffer2, 0, mWriteOffset);
                            bytesToWrite = mWriteOffset;
                            mWriteOffset = 0;
                        }

                    }
                    if (bytesToWrite > 0) {
                        mOutStream.write(buffer2, 0, bytesToWrite);
                    }
                }
                Thread.sleep(1);


                mBtSocket.close();
                synchronized (mReadSync) {
                    mIsConnected = false;
                }
                mHandler.obtainMessage(DISCONNECTED).sendToTarget();


            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                try {
                    mBtSocket.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                synchronized (mReadSync) {
                    mIsConnected = false;
                }
                mHandler.obtainMessage(DISCONNECTED).sendToTarget();
            }
        }
    }
}
