package ntu.mil.grpckebbi.Utils;

import android.app.Activity;
import android.view.View;

public class WindowUtils {
    public static void updateUI(Activity activity){
        View decorView = activity.getWindow().getDecorView();
        setUIVisibility(decorView);
        decorView.setOnSystemUiVisibilityChangeListener(visibility -> setUIVisibility(decorView));
    }

    private static void setUIVisibility(View decorView){
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }
}