package horrorsoft.com.f1abctimertelemetry;

import android.graphics.Color;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
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
public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback, IGpsDataListener {


    private GoogleMap map = null;

    private GpsData mLastPos = null;

    private Marker mMarker = null;
    private Polyline mRoute = null;
    private List<GpsData> mRouteData = new LinkedList<>();

    private boolean followPlane = false;

    @ViewById(R.id.textViewCoords)
    TextView textViewForCoords;

    @Bean
    TelemetryModel mModel;

    @AfterViews
    void afterViews() {
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView)).getMapAsync(this);

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
        // Log.d("1", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mRouteData = mModel.route();
        mModel.setGpsDataListener(this);
        mLastPos = mModel.lastGpsPoint();
        updateMarker(mLastPos);
        // Log.d("1", "onResume");
    }
}
