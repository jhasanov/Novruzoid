package andir.novruzoid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class ResultActivity extends Activity {
    TextView resultView;
    String resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        resultView = (TextView) findViewById(R.id.resultView);

        resultText = getIntent().getExtras().getString("TEXT_RESULT");
    }

    @Override
    protected void onStart() {
        Log.d(getClass().toString(), "onStart called");
        super.onStart();
        resultView.setText("Recognition result:\n");
        resultView.append(resultText);
    }

}
