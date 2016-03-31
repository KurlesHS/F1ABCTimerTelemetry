package horrorsoft.com.f1abctimertelemetry.bluetooth;

/**
 * Created by Alexey on 31.03.2016.
 * Следит за изменеим состояния блютух сокета и за приходом новых данных
 */
public interface IBluetoothDataListener {
    void readyRead();
    void connected();
    void disconnected();
}
