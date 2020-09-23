package horrorsoft.com.f1abctimertelemetry;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.CheckedChange;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.ViewById;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import static horrorsoft.com.f1abctimertelemetry.GpsPointFile.readFileLastPointGps;

@EActivity(R.layout.activity_google_maps)
public class GoogleMapsActivity extends FragmentActivity implements SensorEventListener, OnMapReadyCallback, IGpsDataListener, IPhoneGpsLocationListener {

    private GoogleMap map = null;

    private GpsData mLastPos = null;

    private Marker mMarker = null;
    private Marker mPhoneMarker = null;
    private Polyline mRoute = null;

    private Polyline mLineBetweenPlaneAndPhoneLocation = null;
    private List<GpsData> mRouteData = new LinkedList<>();

    private boolean followPlane = false;

    private Location mPhoneLocation = null;

    private float mCurrentDegree = 0.f;

    private SensorManager mSensorManager;
    private float[] mGData = new float[3];
    private float[] mMData = new float[3];
    private float[] mR = new float[16];
    private float[] mI = new float[16];
    private float[] mOrientation = new float[3];

    private int mCompassAngleInDegrees = 0;

    private Bitmap mIconBitmap;
    private int saveFileRequestCode = 1;
    private int openFileRequestCode = 2;

    long mLastTime;

    @ViewById(R.id.checkBoxShowArrow)
    CheckBox mCheckBoxShowArrow;

    @ViewById(R.id.imageViewForArrow)
    ImageView mImageViewForArrow;

    @ViewById(R.id.textViewCoords)
    TextView textViewForCoords;

    @Bean
    TelemetryModel mModel;

    @AfterViews
    void afterViews() {
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView)).getMapAsync(this);
        mIconBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.arrow_up_icon);
        mCheckBoxShowArrow.setChecked(mModel.isShowDirectionArrow());
        updateCompassIcon();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        mPhoneLocation = mModel.lastKnownPhoneLocation();
        if (mLastPos == null) {
            loadPoint(readFileLastPointGps(mModel.context));
        }
        updateMarker(mLastPos);
        updatePhoneLocationMarker();
    }

    @Click(R.id.buttonCenterOnCurrentPlanerPos)
    void centerOnCurrentPlanePos() {
        if (mLastPos != null && map != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastPos.latitude, mLastPos.longitude),
                    map.getCameraPosition().zoom));
        }
    }

    private void loadPoint(GpsData loadPoint){
        if (loadPoint != null) {
            mModel.setMLastGpsPoint(loadPoint);
            mRouteData.clear();
            loadPoint.isFlightMode=true;
            newPosition(loadPoint);
        }
    }

    @CheckedChange(R.id.checkBoxShowArrow)
    void showArrow(boolean isChecked) {
        Log.d("1", String.format("is checked: %d", isChecked ? 1 : 0));
        mModel.setShowDirectionArrow(isChecked);
        updateCompassIcon();
        // mImageViewForArrow.setVisibility(isChecked ? View.VISIBLE : View.GONE);
    }

    @CheckedChange(R.id.checkBoxFollowPlane)
    void followPlaneStateChanged(boolean isChecked) {
        Log.d("1", String.format("is checked: %d", isChecked ? 1 : 0));
        followPlane = isChecked;
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Click(R.id.button_save_point_file)
    void onSavePointFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); //not needed, but maybe usefull
        intent.putExtra(Intent.EXTRA_TITLE, "default.ser");
        startActivityForResult(intent, saveFileRequestCode);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Click(R.id.button_load_point_file)
    void onLoadPointFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*"); //not needed, but maybe usefull
        startActivityForResult(intent, openFileRequestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == saveFileRequestCode && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                assert uri != null;
                OutputStream output = mModel.context.getContentResolver().openOutputStream(uri);
                ObjectOutputStream oos = new ObjectOutputStream(output);
                oos.writeObject(mLastPos);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == openFileRequestCode && resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                assert uri != null;
                FileInputStream in = (FileInputStream) getContentResolver().openInputStream(uri);
                ObjectInputStream ois = new ObjectInputStream(in);
                loadPoint((GpsData) ois.readObject());
            }
            catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void newPosition(GpsData newPos) {
        // Log.d("1", "on new pos " + String.valueOf(newPos.isFlightMode) );
        if (map == null) {
            return;
        }

        updateMarker(newPos);

        if (mLastPos != null && !mLastPos.isFlightMode && newPos.isFlightMode) {
            // начало полёта
            mRouteData.clear();
        }
        if (newPos.isFlightMode) {
            mRouteData.add(newPos);
        }

        mLastPos = newPos;
        if (mRoute != null) {
            mRoute.remove();
        }
        if (mRouteData.size() > 1) {
            PolylineOptions line = new PolylineOptions();
            for (GpsData data : mRouteData) {
                line.add(new LatLng(data.latitude, data.longitude));
            }

            line.color(Color.BLUE);
            line.width(3);
            line.geodesic(true);
            mRoute = map.addPolyline(line);
        }
        updateLineBetweenPhoneAndPlane();
    }

    private void updateLineBetweenPhoneAndPlane() {
        if (mModel.hasAzimuth()) {

            if (mLineBetweenPlaneAndPhoneLocation != null) {
                mLineBetweenPlaneAndPhoneLocation.remove();
            }
            PolylineOptions line = new PolylineOptions();

            line.add(new LatLng(mModel.lastKnownPhoneLocation().getLatitude(), mModel.lastKnownPhoneLocation().getLongitude()));
            line.add(new LatLng(mModel.lastGpsPoint().latitude , mModel.lastGpsPoint().longitude));

            line.color(Color.RED);
            line.width(3);
            line.geodesic(true);
            mLineBetweenPlaneAndPhoneLocation = map.addPolyline(line);
        }
    }

    private void updatePhoneLocationMarker() {
        if (map == null) {
            return;
        }

        if (mPhoneMarker != null) {
            mPhoneMarker.remove();
            mPhoneMarker = null;
        }

        if (mPhoneLocation != null) {
            MarkerOptions opt = new MarkerOptions();
            opt.position(new LatLng(mPhoneLocation.getLatitude(), mPhoneLocation.getLongitude()));
            opt.title(String.format("%s, %s", mPhoneLocation.getLatitude(), mPhoneLocation.getLongitude()));
            mPhoneMarker = map.addMarker(opt);
            updateLineBetweenPhoneAndPlane();
        }
    }

    private void updateMarker(GpsData pos) {
        if (map == null || pos == null) {
            return;
        }

        textViewForCoords.setText(String.format("%f, %f  -  %s", pos.latitude, pos.longitude, mModel.distanceToModel()));

        if (mMarker != null) {
            mMarker.remove();
        }

        if (mMarker == null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(pos.latitude, pos.longitude), 14));
         } else if (followPlane) {
            centerOnCurrentPlanePos();
        }

        MarkerOptions opt = new MarkerOptions();
        opt.position(new LatLng(pos.latitude, pos.longitude));
        opt.title(String.format("%s, %s", pos.latitude, pos.longitude));
        mMarker = map.addMarker(opt);
        // mMarker.showInfoWindow();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mModel.setGpsDataListener(null);
        mModel.setCurrentActivity(null);
        mModel.setPhoneGpsLocationListener(null);
        // Log.d("1", "onPause");

        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLastTime = System.currentTimeMillis();

        Sensor gSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, gSensor, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);

        mRouteData = mModel.route();
        mModel.setGpsDataListener(this);
        mModel.setCurrentActivity(this);
        mModel.setPhoneGpsLocationListener(this);
        mLastPos = mModel.lastGpsPoint();
        mPhoneLocation = mModel.lastKnownPhoneLocation();
        updateMarker(mLastPos);
        updatePhoneLocationMarker();

        // Log.d("1", "onResume");
    }

    @Override
    public void newPhoneLocation(Location location) {
        mPhoneLocation = location;
        updatePhoneLocationMarker();
    }

    private void updateCompassIcon() {
        double azimuth = mModel.azimuth();
        if (!mModel.isShowDirectionArrow()) {
            if (mImageViewForArrow.getDrawable() != null) {
                mImageViewForArrow.setImageDrawable(null);
            }
            return;
        } else {
            if (mImageViewForArrow.getDrawable() == null) {
                mImageViewForArrow.setImageBitmap(mIconBitmap);
            }
        }

        long currentTime = System.currentTimeMillis();
        long diff = currentTime - mLastTime;
        // обновление стрелки не чаще раза в секунду
        if (diff < 1000) {
            return;
        }
        mLastTime = currentTime;
        updateMarker(mLastPos);

        float rotateTo;
        if (mModel.hasAzimuth()) {
            rotateTo = (float) (mCompassAngleInDegrees - azimuth);
        } else {
            rotateTo = (float)mCompassAngleInDegrees;
        }

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                mCurrentDegree,
                -rotateTo,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        // how long the animation will take place
        ra.setDuration(220);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        mImageViewForArrow.startAnimation(ra);
        mCurrentDegree = -rotateTo;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        float[] data;
        if (type == Sensor.TYPE_ACCELEROMETER) {
            data = mGData;
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            data = mMData;
        } else {
            // we should not be here.
            return;
        }

        System.arraycopy(event.values, 0, data, 0, 3);
        SensorManager.getRotationMatrix(mR, mI, mGData, mMData);
// some test code which will be used/cleaned up before we ship this.
//        SensorManager.remapCoordinateSystem(mR,
//                SensorManager.AXIS_X, SensorManager.AXIS_Z, mR);
//        SensorManager.remapCoordinateSystem(mR,
//                SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mR);

        SensorManager.getOrientation(mR, mOrientation);
        final float rad2deg = (float) (180.0f / Math.PI);

        mCompassAngleInDegrees = (int) (mOrientation[0] * rad2deg);
        updateCompassIcon();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
