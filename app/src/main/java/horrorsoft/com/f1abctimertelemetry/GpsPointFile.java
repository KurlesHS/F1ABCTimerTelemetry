package horrorsoft.com.f1abctimertelemetry;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static android.content.Context.MODE_PRIVATE;

/**
 * Created by Andrey on 18.09.2020.
 *
 */
public class GpsPointFile {
    private static String NameFile = "data.ser";
    public static void saveFileLastPointGps(Context context, GpsData mLastGpsPoint) {
        try {
            FileOutputStream out = context.openFileOutput(NameFile, MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(mLastGpsPoint);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static GpsData readFileLastPointGps(Context context) {

        try {
            FileInputStream in = context.openFileInput(NameFile);
            ObjectInputStream ois = new ObjectInputStream(in);
            return  (GpsData) ois.readObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
