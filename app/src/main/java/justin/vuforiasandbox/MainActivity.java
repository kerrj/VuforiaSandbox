package justin.vuforiasandbox;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    private MySurfaceView surfaceView;
    private static MainActivity context;

    public static MainActivity getActivity(){
        return context;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=this;
        setContentView(R.layout.activity_main);
        surfaceView=(MySurfaceView)findViewById(R.id.surfaceView);
    }
    public void takeSnapshot(View view){
        surfaceView.takeSnapshot();
    }
}
