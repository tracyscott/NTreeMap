package art.lookingup.ntreemap;

import android.app.Application;
import android.content.SharedPreferences;

public class MyApplication extends Application {
    public SharedPreferences preferences;

    public SharedPreferences getSharedPrefs() {
        if (preferences == null) {
            preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        }
        return preferences;
    }
}
