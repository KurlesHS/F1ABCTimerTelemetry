package horrorsoft.com.f1abctimertelemetry;

import android.app.Application;
import android.content.Context;
import android.support.annotation.MainThread;
import org.androidannotations.annotations.EApplication;
import org.androidannotations.annotations.EBean;
import org.androidannotations.annotations.RootContext;

/**
 * Created by Alexey on 01.04.2016.
 *
 */


@EApplication
public class F1AbcTelemetryApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }
}
