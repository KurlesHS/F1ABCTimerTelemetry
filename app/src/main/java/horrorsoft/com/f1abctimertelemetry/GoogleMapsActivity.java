package horrorsoft.com.f1abctimertelemetry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import org.androidannotations.annotations.*;

import java.util.LinkedList;
import java.util.List;


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

    private SensorManager mSensorManager;
    private float[] mGData = new float[3];
    private float[] mMData = new float[3];
    private float[] mR = new float[16];
    private float[] mI = new float[16];
    private float[] mOrientation = new float[3];
    private int mCount;

    private int mCompassAngleInDegrees = 0;
    private float mCompassAngleInRadians = 0;


    Bitmap mIconBitmap;

    @ViewById(R.id.imageButtonArrow)
    ImageButton mImageButtonArrow;

    @ViewById(R.id.textViewCoords)
    TextView textViewForCoords;

    @Bean
    TelemetryModel mModel;

    @AfterViews
    void afterViews() {
        mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView)).getMapAsync(this);
        mIconBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.arrow_up_icon);
        updateCompassIcon();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        updateMarker(mLastPos);
    }

    @Click(R.id.buttonCenterOnCurrentPlanerPos)
    void centerOnCurrentPlanePos() {
        if (mLastPos != null && map != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(mLastPos.latitude, mLastPos.longitude),
                    map.getCameraPosition().zoom));
        }
    }

    @CheckedChange(R.id.checkBoxFollowPlane)
    void followPlaneStateChanged(boolean isChecked) {
        Log.d("1", String.format("is checked: %d", isChecked ? 1 : 0));
        followPlane = isChecked;
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

        textViewForCoords.setText(String.format("%f, %f", pos.latitude, pos.longitude));

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

    public static Bitmap rotateImage(Bitmap src, float degree)
    {
        // create new matrix
        Matrix matrix = new Matrix();
        // setup rotation degree
        matrix.postRotate(-degree);
        Bitmap bmp = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        return bmp;
    }

    void updateCompassIcon() {
        mImageButtonArrow.setImageBitmap(rotateImage(mIconBitmap, (float) mCompassAngleInDegrees));
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
        for (int i=0 ; i<3 ; i++)
            data[i] = event.values[i];
        SensorManager.getRotationMatrix(mR, mI, mGData, mMData);
// some test code which will be used/cleaned up before we ship this.
//        SensorManager.remapCoordinateSystem(mR,
//                SensorManager.AXIS_X, SensorManager.AXIS_Z, mR);
//        SensorManager.remapCoordinateSystem(mR,
//                SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, mR);
        SensorManager.getOrientation(mR, mOrientation);
        float incl = SensorManager.getInclination(mI);
        if (mCount++ > 50) {
            final float rad2deg = (float)(180.0f/Math.PI);
            mCount = 0;

            mCompassAngleInDegrees = (int)(mOrientation[0]*rad2deg);
            //mCompassAngleInDegrees += 1;
            mCompassAngleInRadians = mOrientation[0];

            if (mCompassAngleInDegrees < 0) {
                mCompassAngleInDegrees += 360;
            }
            Log.d("Compass", "yaw: " + mCompassAngleInDegrees +
                    "  pitch: " + (int)(mOrientation[1]*rad2deg) +
                    "  roll: " + (int)(mOrientation[2]*rad2deg) +
                    "  incl: " + (int)(incl*rad2deg)
            );
            updateCompassIcon();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
