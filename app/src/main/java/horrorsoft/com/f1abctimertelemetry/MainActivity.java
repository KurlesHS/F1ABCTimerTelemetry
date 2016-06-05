package horrorsoft.com.f1abctimertelemetry;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import horrorsoft.com.f1abctimertelemetry.bluetooth.DeviceListActivity;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothStatusListener;
import org.androidannotations.annotations.*;

@EActivity(R.layout.main_layout)
public class MainActivity extends Activity implements IBluetoothStatusListener {

    private static final int REQUEST_BT_DEVICE_MAC_ADDRESS = 0;

    private ProgressDialog progressDialog = null;

    @Bean
    TelemetryModel mModel;


    @ViewById(R.id.button_connect_disconnect)
    Button mConnectDisconnectButton;

    @Override
    protected void onPause() {
        super.onPause();
        mModel.removeBlueToothStatusListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mModel.addBlueToothStatusListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Click(R.id.button_connect_disconnect)
    void onConnectDisconnectButtonClicked() {
        if (mModel.isOpen()) {
            mModel.close();
        } else {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(intent, REQUEST_BT_DEVICE_MAC_ADDRESS);
        }
    }

    @Click(R.id.buttonMap)
    void onMapButton() {
        Intent intent = new Intent(this, GoogleMapsActivity_.class);
        startActivity(intent);
    }

    @Click(R.id.button_telemetry)
    void onTelemetryButtonClicked() {
        Intent intent = new Intent(this, TelemetryActivity_.class);
        startActivity(intent);
    }

    @OnActivityResult(REQUEST_BT_DEVICE_MAC_ADDRESS)
    void onRequestDeviceMacAddressDone(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String macAddress = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            // Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
            progressDialog = ProgressDialog.show(this, "Connecting", "Please wait...", true);
            mModel.open(macAddress);

        } else {
            Toast.makeText(this, "cancel paired with bluetooth device", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }
    }

    @Override
    public void connected() {
        mConnectDisconnectButton.setText(getResources().getText(R.string.disconnect));
        dismissProgressDialog();
    }

    @Override
    public void disconnected() {
        mConnectDisconnectButton.setText(getResources().getText(R.string.connect));
        dismissProgressDialog();
    }
}
