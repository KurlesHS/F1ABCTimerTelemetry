package horrorsoft.com.f1abctimertelemetry;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;

import java.util.LinkedList;


@EActivity(R.layout.activity_google_maps)
public class GoogleMapsActivity extends FragmentActivity implements OnMapReadyCallback, IGpsDataListener {


    private GoogleMap map = null;

    private GpsData mLastPos = null;

    private Marker mMarker = null;
    private Polyline mRoute = null;
    LinkedList<GpsData> mRouteData = new LinkedList<>();

    @Bean
    TelemetryModel mModel;

    @AfterViews
    void afterViews() {
        ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView)).getMapAsync(this);
        Log.d("1", "afterViews");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        /*
        PolylineOptions line = new PolylineOptions();
        line.add(new LatLng(59.146593, 37.887522));
        line.add(new LatLng(59.125421, 37.931468));
        line.add(new LatLng(59.11121, 37.960135));
        line.add(new LatLng(59.107682, 38.083119));
        line.color(Color.BLUE);
        line.width(3);
        line.geodesic(true);
        map.addPolyline(line);

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(59.125421, 37.931468), 14));
        */

    }

    @Override
    public void newPosition(GpsData newPos) {
        Log.d("1", "on new pos " + String.valueOf(newPos.isFlightMode) );
        if (map == null) {
            return;
        }

        if (mLastPos != null && mLastPos.longitude == newPos.longitude && mLastPos.latitude == mLastPos.latitude) {
            // те же данные - не засоряем данные. Хотя наверное это стоит делать в модели раз и не заморачиваться
            return;
        }

        if (mMarker != null) {
            mMarker.remove();
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(newPos.latitude, newPos.longitude), 14));
        }

        MarkerOptions opt = new MarkerOptions();
        opt.position(new LatLng(newPos.latitude, newPos.longitude));
        mMarker = map.addMarker(opt);

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

    @Override
    protected void onPause() {
        super.onPause();
        mModel.setGpsDataListener(null);
        Log.d("1", "onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mModel.setGpsDataListener(this);
        Log.d("1", "onResume");

    }
}
