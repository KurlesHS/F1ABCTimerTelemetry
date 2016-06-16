package horrorsoft.com.f1abctimertelemetry;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import horrorsoft.com.f1abctimertelemetry.bluetooth.IBluetoothStatusListener;
import org.androidannotations.annotations.*;

import java.util.ArrayList;
import java.util.ListIterator;

@Fullscreen
@EActivity(R.layout.activity_telemetry)
public class TelemetryActivity extends Activity implements IBluetoothStatusListener, ITelemetryDataListener {

    @Bean
    TelemetryModel mModel;

    @ViewById(R.id.imageViewBluetoothStatus)
    ImageView imageViewBluetoothStatus;

    @ViewById(R.id.tm_alt_digit_1_view)
    ImageView tmAltDigit1;
    @ViewById(R.id.tm_alt_digit_2_view)
    ImageView tmAltDigit2;
    @ViewById(R.id.tm_alt_digit_3_view)
    ImageView tmAltDigit3;
    @ViewById(R.id.tm_alt_digit_4_view)
    ImageView tmAltDigit4;
    @ViewById(R.id.tm_alt_digit_5_view)
    ImageView tmAltDigit5;

    private ArrayList<ImageView> altitudeDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_timvol_digit_1_view)
    ImageView tmTimVolDigit1;
    @ViewById(R.id.tm_timvol_digit_2_view)
    ImageView tmTimVolDigit2;
    @ViewById(R.id.tm_timvol_digit_3_view)
    ImageView tmTimVolDigit3;
    @ViewById(R.id.tm_timvol_digit_4_view)
    ImageView tmTimVolDigit4;
    @ViewById(R.id.tm_timvol_digit_5_view)
    ImageView tmTimVolDigit5;

    private ArrayList<ImageView> timerVoltageDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_time_digit_1_view)
    ImageView timeDigit1;
    @ViewById(R.id.tm_time_digit_2_view)
    ImageView timeDigit2;
    @ViewById(R.id.tm_time_digit_3_view)
    ImageView timeDigit3;
    @ViewById(R.id.tm_time_digit_4_view)
    ImageView timeDigit4;
    @ViewById(R.id.tm_time_digit_5_view)
    ImageView timeDigit5;

    private ArrayList<ImageView> timeDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_nodat_digit_1_view)
    ImageView noDataDigit1;
    @ViewById(R.id.tm_nodat_digit_2_view)
    ImageView noDataDigit2;
    @ViewById(R.id.tm_nodat_digit_3_view)
    ImageView noDataDigit3;
    @ViewById(R.id.tm_nodat_digit_4_view)
    ImageView noDataDigit4;
    @ViewById(R.id.tm_nodat_digit_5_view)
    ImageView noDataDigit5;

    private ArrayList<ImageView> noDataDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_dtvol_digit_1_view)
    ImageView tmDtVoltageDigit1;
    @ViewById(R.id.tm_dtvol_digit_2_view)
    ImageView tmDtVoltageDigit2;
    @ViewById(R.id.tm_dtvol_digit_3_view)
    ImageView tmDtVoltageDigit3;
    @ViewById(R.id.tm_dtvol_digit_4_view)
    ImageView tmDtVoltageDigit4;
    @ViewById(R.id.tm_dtvol_digit_5_view)
    ImageView tmDtVoltageDigit5;

    private ArrayList<ImageView> dtVoltageDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_speed_digit_1_view)
    ImageView tmSpeedDigit1;
    @ViewById(R.id.tm_speed_digit_2_view)
    ImageView tmSpeedDigit2;
    @ViewById(R.id.tm_speed_digit_3_view)
    ImageView tmSpeedDigit3;
    @ViewById(R.id.tm_speed_digit_4_view)
    ImageView tmSpeedDigit4;
    @ViewById(R.id.tm_speed_digit_5_view)
    ImageView tmSpeedDigit5;

    private ArrayList<ImageView> speedDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_temp_digit_1_view)
    ImageView tmTempDigit1;
    @ViewById(R.id.tm_temp_digit_2_view)
    ImageView tmTempDigit2;
    @ViewById(R.id.tm_temp_digit_3_view)
    ImageView tmTempDigit3;
    @ViewById(R.id.tm_temp_digit_4_view)
    ImageView tmTempDigit4;
    @ViewById(R.id.tm_temp_digit_5_view)
    ImageView tmTempDigit5;

    private ArrayList<ImageView> temperatureDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_prgn_digit_1_view)
    ImageView tmPredictDigit1;
    @ViewById(R.id.tm_prgn_digit_2_view)
    ImageView tmPredictDigit2;
    @ViewById(R.id.tm_prgn_digit_3_view)
    ImageView tmPredictDigit3;
    @ViewById(R.id.tm_prgn_digit_4_view)
    ImageView tmPredictDigit4;
    @ViewById(R.id.tm_prgn_digit_5_view)
    ImageView tmPredictDigit5;

    private ArrayList<ImageView> predictDigitImages = new ArrayList<>();

    @ViewById(R.id.tm_sign_digit_1_view)
    ImageView tmSignalDigit1;
    @ViewById(R.id.tm_sign_digit_2_view)
    ImageView tmSignalDigit2;
    @ViewById(R.id.tm_sign_digit_3_view)
    ImageView tmSignalDigit3;
    @ViewById(R.id.tm_sign_digit_4_view)
    ImageView tmSignalDigit4;
    @ViewById(R.id.tm_sign_digit_5_view)
    ImageView tmSignalDigit5;

    @ViewById(R.id.tm_flag_servo_view)
    ImageView servoFlagImage;

    @ViewById(R.id.tm_flag_blink_view)
    ImageView blinkFlagImage;

    @ViewById(R.id.tm_flag_dt_view)
    ImageView dtFlagImage;

    @ViewById(R.id.tm_flag_rdt_view)
    ImageView rdtFlagImage;

    private ArrayList<ImageView> signalDigitImages = new ArrayList<>();

    private ArrayList<Drawable> digitsImages = new ArrayList<>();
    private Drawable plusImg;
    private Drawable minusImg;
    private Drawable dotImg;
    private Drawable emptyImg;

    private Drawable tmServoFlagOnImg;

    private Drawable tmServoFlagOffImg;

    private Drawable tmBlinkFlagOnImg;
    private Drawable tmBlinkFlagOffImg;

    private Drawable tmDtFlagOnImg;
    private Drawable tmDtFlagOffImg;

    private Drawable tmRdtFlagOnImg;
    private Drawable tmRdtFlagOffImg;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mModel.addBlueToothStatusListener(this);
        mModel.setTelemetryDataListener(this);
        mModel.setCurrentActivity(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mModel.removeBlueToothStatusListener(this);
        mModel.setTelemetryDataListener(null);
        mModel.setCurrentActivity(null);
    }

    @AfterViews
    void init() {
        altitudeDigitImages.add(tmAltDigit1);
        altitudeDigitImages.add(tmAltDigit2);
        altitudeDigitImages.add(tmAltDigit3);
        altitudeDigitImages.add(tmAltDigit4);
        altitudeDigitImages.add(tmAltDigit5);

        timerVoltageDigitImages.add(tmTimVolDigit1);
        timerVoltageDigitImages.add(tmTimVolDigit2);
        timerVoltageDigitImages.add(tmTimVolDigit3);
        timerVoltageDigitImages.add(tmTimVolDigit4);
        timerVoltageDigitImages.add(tmTimVolDigit5);

        timeDigitImages.add(timeDigit1);
        timeDigitImages.add(timeDigit2);
        timeDigitImages.add(timeDigit3);
        timeDigitImages.add(timeDigit4);
        timeDigitImages.add(timeDigit5);

        noDataDigitImages.add(noDataDigit1);
        noDataDigitImages.add(noDataDigit2);
        noDataDigitImages.add(noDataDigit3);
        noDataDigitImages.add(noDataDigit4);
        noDataDigitImages.add(noDataDigit5);

        dtVoltageDigitImages.add(tmDtVoltageDigit1);
        dtVoltageDigitImages.add(tmDtVoltageDigit2);
        dtVoltageDigitImages.add(tmDtVoltageDigit3);
        dtVoltageDigitImages.add(tmDtVoltageDigit4);
        dtVoltageDigitImages.add(tmDtVoltageDigit5);

        speedDigitImages.add(tmSpeedDigit1);
        speedDigitImages.add(tmSpeedDigit2);
        speedDigitImages.add(tmSpeedDigit3);
        speedDigitImages.add(tmSpeedDigit4);
        speedDigitImages.add(tmSpeedDigit5);

        temperatureDigitImages.add(tmTempDigit1);
        temperatureDigitImages.add(tmTempDigit2);
        temperatureDigitImages.add(tmTempDigit3);
        temperatureDigitImages.add(tmTempDigit4);
        temperatureDigitImages.add(tmTempDigit5);

        predictDigitImages.add(tmPredictDigit1);
        predictDigitImages.add(tmPredictDigit2);
        predictDigitImages.add(tmPredictDigit3);
        predictDigitImages.add(tmPredictDigit4);
        predictDigitImages.add(tmPredictDigit5);

        signalDigitImages.add(tmSignalDigit1);
        signalDigitImages.add(tmSignalDigit2);
        signalDigitImages.add(tmSignalDigit3);
        signalDigitImages.add(tmSignalDigit4);
        signalDigitImages.add(tmSignalDigit5);

        plusImg = ResourcesCompat.getDrawable(getResources(), R.drawable.digit_plus, null);
        minusImg = ResourcesCompat.getDrawable(getResources(), R.drawable.digit_minus, null);
        emptyImg = ResourcesCompat.getDrawable(getResources(), R.drawable.digit_empty, null);
        dotImg = ResourcesCompat.getDrawable(getResources(), R.drawable.digit_point, null);


        tmBlinkFlagOffImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_blink_flag_0, null);
        tmBlinkFlagOnImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_blink_flag_1, null);

        tmServoFlagOffImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_servo_flag_0, null);
        tmServoFlagOnImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_servo_flag_1, null);

        tmDtFlagOffImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_dt_flag_0, null);
        tmDtFlagOnImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_dt_flag_1, null);

        tmRdtFlagOffImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_rdt_flag_0, null);
        tmRdtFlagOnImg = ResourcesCompat.getDrawable(getResources(), R.drawable.tm_rdt_flag_1, null);

        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_0, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_1, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_2, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_3, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_4, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_5, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_6, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_7, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_8, null));
        digitsImages.add(ResourcesCompat.getDrawable(getResources(), R.drawable.digit_9, null));
        updateBlueToothStatusImage(mModel.isOpen());
    }

    private void updateBlueToothStatusImage(boolean status) {
        imageViewBluetoothStatus.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                status ? R.drawable.bt_con : R.drawable.bt_discon, null));
    }

    @Override
    public void connected() {
        updateBlueToothStatusImage(true);
    }

    @Override
    public void disconnected() {
        updateBlueToothStatusImage(false);
    }

    private void drawText(ArrayList<ImageView> imageViews, String text) {
        ListIterator<ImageView> li = imageViews.listIterator(imageViews.size());
        String reversedText = new StringBuilder(text).reverse().toString();
        int stringLen = reversedText.length();
        int i = 0;
        while (li.hasPrevious()) {
            ImageView nextDigitImage = li.previous();
            Drawable srcForThisPos = emptyImg;
            if (i < stringLen) {
                char ch = reversedText.charAt(i);
                if (ch == '.' || ch == ',') {
                    srcForThisPos = dotImg;
                } else if (ch == '-') {
                    srcForThisPos = minusImg;
                } else if (ch == '+') {
                    srcForThisPos = plusImg;
                } else if (ch >= '0' && ch <= '9') {
                    int idx = ch - '0';
                    srcForThisPos = digitsImages.get(idx);
                }
            }
            ++i;
            nextDigitImage.setImageDrawable(srcForThisPos);
        }
    }

    private String appendSign(String str, float value) {
        if (value < 0) {
            return "-" + str;
        } else if (value > 0) {
            return "+" + str;
        }
        return str;
    }

    @UiThread
    protected void setNewTelemetryData(TelemetryData data) {
        String altitudeStr = String.format("%04d", Math.abs(data.height));
        altitudeStr = appendSign(altitudeStr, data.height);
        String voltageStr = String.format("%.2f", data.voltage);
        String timeDtStr = String.format("%04d", data.timeToDt);
        String noDataStr = String.format("%5d", data.act & 0xff);
        String voltage2Str = String.format("%.2f", data.pwr);
        String speedStr = String.format("%.2f", data.speed);
        String temperatureStr = String.format("%.1f", Math.abs(data.temperature));
        temperatureStr = appendSign(temperatureStr, data.temperature);
        String predictStr = String.format("%04d", data.prediction);
        String signalStr = String.format("%5d", data.rssi & 0xff);

        drawText(altitudeDigitImages, altitudeStr);
        drawText(timerVoltageDigitImages, voltageStr);
        drawText(timeDigitImages, timeDtStr);
        drawText(noDataDigitImages, noDataStr);
        drawText(dtVoltageDigitImages, voltage2Str);
        drawText(speedDigitImages, speedStr);
        drawText(temperatureDigitImages, temperatureStr);
        drawText(predictDigitImages, predictStr);
        drawText(signalDigitImages, signalStr);

        servoFlagImage.setImageDrawable(data.servoOnFlag ? tmServoFlagOnImg : tmServoFlagOffImg);
        blinkFlagImage.setImageDrawable(data.blinkerOnFlag ? tmBlinkFlagOnImg : tmBlinkFlagOffImg);
        dtFlagImage.setImageDrawable(data.dtFlag ? tmDtFlagOnImg : tmDtFlagOffImg);
        rdtFlagImage.setImageDrawable(data.rdtFlag ? tmRdtFlagOnImg : tmRdtFlagOffImg);
    }


    @Override
    public void result(TelemetryData telemetryData) {
        if (!telemetryData.hasError) {
            setNewTelemetryData(telemetryData);
        }
    }
}
