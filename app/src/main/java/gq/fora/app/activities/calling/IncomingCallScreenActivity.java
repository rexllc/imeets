package gq.fora.app.activities.calling;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.sinch.android.rtc.video.VideoCallListener;

import gq.fora.app.R;
import gq.fora.app.activities.calling.video.VideoCallScreenActivity;
import gq.fora.app.models.AudioPlayer;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.Utils;

import java.util.List;

public class IncomingCallScreenActivity extends BaseActivity {

    static final String TAG = IncomingCallScreenActivity.class.getSimpleName();
    private String mCallId;
    private AudioPlayer mAudioPlayer;

    private FirebaseFirestore database = FirebaseFirestore.getInstance();

    public static final String ACTION_ANSWER = "answer";
    public static final String ACTION_IGNORE = "ignore";
    public static final String EXTRA_ID = "EXTRA_ID";
    public static final String NOTIF_ID = "id";
    public static int MESSAGE_ID = 14;
    private String mAction;
    private ImageView answer;
    private ImageView decline, muteButton, speakerButton;
    private TextView display_name, call_state;
    private ImageView profile_pic;
    private TextView mCallDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        call_state = (TextView) findViewById(R.id.call_state);

        answer = (ImageView) findViewById(R.id.accept_icon);
        answer.setOnClickListener(mClickListener);
        decline = (ImageView) findViewById(R.id.decline_icon);
        decline.setOnClickListener(mClickListener);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);
        mCallDuration = findViewById(R.id.call_duration);

        display_name = findViewById(R.id.display_name);
        profile_pic = findViewById(R.id.profile_pic);

        mAudioPlayer = new AudioPlayer(this);
        mAudioPlayer.playRingtone();

        Intent intent = getIntent();
        mCallId = intent.getStringExtra(SinchService.CALL_ID);
        mAction = "";

        Utils.rippleRoundStroke(decline, "#f44336", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(answer, "#3f61dc", "#ffffff", 360, 0, "#3f61dc");

        muteButton.setVisibility(View.GONE);
        speakerButton.setVisibility(View.GONE);
        mCallDuration.setVisibility(View.GONE);
        Window window = getWindow();
        View view = window.getDecorView();
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);
        insets.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insets.setAppearanceLightStatusBars(false);
        window.setStatusBarColor(0xFF000000);
        window.setNavigationBarColor(0xFF000000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getStringExtra(SinchService.CALL_ID) != null) {
                mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
            }
            final int id = intent.getIntExtra(EXTRA_ID, -1);
            if (id > 0) {
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(id);
            }
            mAction = intent.getAction();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudioPlayer != null) {
            mAudioPlayer.stopRingtone();
        }
    }

    @Override
    protected void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (call.getDetails().isVideoOffered()) {
                call_state.setText("Video Call");
                call.addCallListener(new SinchVideoCallListener());
            } else {
                call_state.setText("Voice Call");
                call.addCallListener(new SinchCallListener());
            }

            database.collection("users")
                    .document(call.getHeaders().get("caller_id"))
                    .get()
                    .addOnCompleteListener(
                            (task) -> {
                                display_name.setText(task.getResult().getString("displayName"));
                                Glide.with(IncomingCallScreenActivity.this)
                                        .load(task.getResult().getString("userPhoto"))
                                        .skipMemoryCache(true)
                                        .thumbnail(0.1f)
                                        .into(profile_pic);
                            });

            if (ACTION_ANSWER.equals(mAction)) {
                mAction = "";
                answerClicked();
            } else if (ACTION_IGNORE.equals(mAction)) {
                mAction = "";
                declineClicked();
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting");
            finish();
        }
    }

    private void answerClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            Log.d(TAG, "Answering call");
            if (call.getDetails().isVideoOffered()) {
                Intent intent = new Intent(this, VideoCallScreenActivity.class);
                intent.putExtra(SinchService.CALL_ID, mCallId);
                intent.putExtra(SinchService.EXTRA_ID, call.getHeaders().get("caller_id"));
                startActivity(intent);
                call.answer();
            } else {
                Intent intent = new Intent(this, CallScreenActivity.class);
                intent.putExtra(SinchService.CALL_ID, mCallId);
                intent.putExtra(SinchService.EXTRA_ID, call.getHeaders().get("caller_id"));
                startActivity(intent);
                call.answer();
            }
        } else {
            finish();
        }
    }

    private void declineClicked() {
        mAudioPlayer.stopRingtone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            mAudioPlayer.stopRingtone();
            setStatusBar();
            finish();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
        }

        public void onShouldSendPushNotification(Call arg0, List<PushPair> arg1) {}
    }

    private class SinchVideoCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended, cause: " + cause.toString());
            switch (cause) {
                case FAILURE:
                    break;
                case CANCELED:
                    break;
                case NO_ANSWER:
                    break;
                case HUNG_UP:
                    break;
                case DENIED:
                    break;
                case OTHER_DEVICE_ANSWERED:
                    break;
                case TIMEOUT:
                    break;
                case TRANSFERRED:
                    break;
            }
            mAudioPlayer.stopRingtone();
            finish();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onVideoTrackAdded(Call call) {
            // Display some kind of icon showing it's a video call
        }

        @Override
        public void onVideoTrackPaused(Call call) {
            // Display some kind of icon showing it's a video call
        }

        @Override
        public void onVideoTrackResumed(Call call) {
            // Display some kind of icon showing it's a video call
        }
    }

    private OnClickListener mClickListener =
            new OnClickListener() {
                @Override
                public void onClick(View v) {
                    switch (v.getId()) {
                        case R.id.accept_icon:
                            answerClicked();
                            break;
                        case R.id.decline_icon:
                            declineClicked();
                            break;
                    }
                }
            };

    public void setStatusBar() {
        View decorView = getWindow().getDecorView();
        // Show the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
        decorView.setSystemUiVisibility(uiOptions);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(0xFFFFFFFF);
    }

    public void callEndedCallback(String id) {}
}
