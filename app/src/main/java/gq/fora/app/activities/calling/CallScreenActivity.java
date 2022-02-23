package gq.fora.app.activities.calling;

import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.view.WindowInsetsControllerCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import gq.fora.app.R;
import gq.fora.app.models.AudioPlayer;
import gq.fora.app.models.CallType;
import gq.fora.app.models.Message;
import gq.fora.app.models.MessageType;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.models.sessions.Session;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.Utils;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CallScreenActivity extends BaseActivity {

    static final String TAG = CallScreenActivity.class.getSimpleName();

    private AudioPlayer mAudioPlayer;
    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;
    private TimerTask timer;
    private Timer _timer = new Timer();
    private AudioController audioController;

    private String mCallId;

    private TextView mCallDuration;
    private TextView mCallState;
    private TextView mCallerName;
    private TextView display_name;
    private ImageView endCallButton, answerCallButton, muteButton, speakerButton;
    private ImageView profile_pic;
    private boolean isMute = true;
    private boolean isSpeakerOn = true;
    private int type;

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private DatabaseReference messages = _firebase.getReference("messages");
    private DatabaseReference conversations = _firebase.getReference("conversations");

    private class UpdateCallDurationTask extends TimerTask {

        @Override
        public void run() {
            CallScreenActivity.this.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            updateCallDuration();
                        }
                    });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calling);

        mAudioPlayer = new AudioPlayer(this);
        mCallDuration = (TextView) findViewById(R.id.call_duration);
        mCallerName = (TextView) findViewById(R.id.display_name);
        mCallState = (TextView) findViewById(R.id.call_state);
        endCallButton = (ImageView) findViewById(R.id.decline_icon);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);
        answerCallButton = (ImageView) findViewById(R.id.accept_icon);
        display_name = findViewById(R.id.display_name);
        profile_pic = findViewById(R.id.profile_pic);

        endCallButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        endCall();
                    }
                });
        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);

        Utils.rippleRoundStroke(endCallButton, "#f44336", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(muteButton, "#424242", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(speakerButton, "#424242", "#ffffff", 360, 0, "#f44336");
        answerCallButton.setVisibility(View.GONE);
        mCallDuration.setVisibility(View.GONE);
        Window window = getWindow();
        View view = window.getDecorView();
        WindowInsetsControllerCompat insets = new WindowInsetsControllerCompat(window, view);
        insets.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        insets.setAppearanceLightStatusBars(false);
        window.setStatusBarColor(0xFF000000);
		window.setNavigationBarColor(0xFF000000);

        muteButton.setOnClickListener(
                (View v) -> {
                    if (isMute) {
                        audioController.mute();
                        muteButton.setImageResource(R.drawable.cmd_microphone_off);
                        isMute = false;
                    } else {
                        muteButton.setImageResource(R.drawable.cmd_microphone);
                        audioController.unmute();
                        isMute = true;
                    }
                });

        speakerButton.setOnClickListener(
                (View v) -> {
                    if (isSpeakerOn) {
                        speakerButton.setImageResource(R.drawable.cmd_volume_off);
                        audioController.disableSpeaker();
                        isSpeakerOn = false;
                    } else {
                        speakerButton.setImageResource(R.drawable.cmd_volume_high);
                        audioController.enableSpeaker();
                        isSpeakerOn = true;
                    }
                });

        muteButton.setEnabled(false);
        speakerButton.setEnabled(false);
    }

    @Override
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.addCallListener(new SinchCallListener());
            mCallState.setText(call.getState().toString());
            users.child(getIntent().getStringExtra(SinchService.EXTRA_ID))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot arg0) {
                                    User user = arg0.getValue(User.class);

                                    if (user != null) {
                                        display_name.setText(user.displayName);
                                        Glide.with(CallScreenActivity.this)
                                                .load(user.userPhoto)
                                                .skipMemoryCache(true)
                                                .thumbnail(0.1f)
                                                .into(profile_pic);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError arg0) {}
                            });
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
            Toast.makeText(CallScreenActivity.this, "Invalid call id", Toast.LENGTH_LONG).show();
        }
        audioController = getSinchServiceInterface().getAudioController();
        muteButton.setEnabled(true);
        speakerButton.setEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        mDurationTask.cancel();
        mTimer.cancel();
    }

    @Override
    public void onResume() {
        super.onResume();
        mTimer = new Timer();
        mDurationTask = new UpdateCallDurationTask();
        mTimer.schedule(mDurationTask, 0, 500);
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
        endCall();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        endCall();
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private String formatTimespan(int totalSeconds) {
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private void updateCallDuration() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            mCallDuration.setText(formatTimespan(call.getDetails().getDuration()));
        }
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended: " + call.getDetails().toString();
            // Toast.makeText(CallScreenActivity.this, endMsg, Toast.LENGTH_LONG).show();
            Utils copy = new Utils(CallScreenActivity.this);
            copy.copyText(cause.toString() + " : " + endMsg);
            mCallDuration.setVisibility(View.GONE);
            endCallButton.setVisibility(View.GONE);
            muteButton.setVisibility(View.GONE);
            speakerButton.setVisibility(View.GONE);
            mCallState.setText("Call Ended");
            callEndedCallback(getIntent().getStringExtra(SinchService.EXTRA_ID), cause.toString());
            timer =
                    new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            endCall();
                                            View decorView = getWindow().getDecorView();
                                            // Show the status bar.
                                            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
                                            decorView.setSystemUiVisibility(uiOptions);
                                            getWindow()
                                                    .getDecorView()
                                                    .setSystemUiVisibility(
                                                            View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                                            getWindow().setStatusBarColor(0xFFFFFFFF);
                                        }
                                    });
                        }
                    };
            _timer.schedule(timer, (1500));
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            mAudioPlayer.stopProgressTone();
            mCallState.setVisibility(View.GONE);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            AudioController audioController = getSinchServiceInterface().getAudioController();
            audioController.disableSpeaker();
            mCallDuration.setVisibility(View.VISIBLE);
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mAudioPlayer.playProgressTone();
        }

        public void onShouldSendPushNotification(Call arg0, List<PushPair> arg1) {}
    }

    public void callEndedCallback(String id, String CALL_TYPE) {
        String msg_id = messages.push().getKey();
        switch (CALL_TYPE) {
            case "FAILURE":
                type = CallType.FAILURE;
                break;
            case "CANCELLED":
                type = CallType.CANCELLED;
                break;
            case "NO_ANSWER":
                type = CallType.NO_ANSWER;
                break;
            case "HUNG_UP":
                type = CallType.HUNG_UP;
                break;
            case "TIMEOUT":
                type = CallType.TIMEOUT;
                break;
            case "NONE":
                type = CallType.NONE;
                break;
            case "OTHER_DEVICE_ANSWERED":
                type = CallType.OTHER_DEVICE_ANSWERED;
                break;
            case "TRANSFERRED":
                type = CallType.TRANSFERRED;
                break;
            case "DENIED":
                type = CallType.DENIED;
                break;
        }
    }
}
