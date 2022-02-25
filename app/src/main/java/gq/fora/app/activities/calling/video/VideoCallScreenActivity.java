package gq.fora.app.activities.calling.video;

import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallState;
import com.sinch.android.rtc.video.VideoCallListener;
import com.sinch.android.rtc.video.VideoController;

import de.hdodenhof.circleimageview.CircleImageView;

import gq.fora.app.R;
import gq.fora.app.activities.calling.BaseActivity;
import gq.fora.app.models.AudioPlayer;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.Utils;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class VideoCallScreenActivity extends BaseActivity {

    static final String TAG = VideoCallScreenActivity.class.getSimpleName();
    static final String ADDED_LISTENER = "addedListener";
    static final String VIEWS_TOGGLED = "viewsToggled";

    private AudioPlayer mAudioPlayer;
    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;
    private TimerTask timer;
    private Timer _timer = new Timer();
    private boolean isMute = true;
    private boolean isSpeakerOn = true;

    private String mCallId;
    private boolean mAddedListener = false;
    private boolean mLocalVideoViewAdded = false;
    private boolean mRemoteVideoViewAdded = false;
    private AudioController audioController;

    private TextView mCallDuration;
    private TextView mCallState;
    private TextView mCallerName;
    boolean mToggleVideoViewPositions = false;
    private ImageView endCallButton, muteButton, speakerButton;
    private CircleImageView profile_pic;

    private FirebaseFirestore database = FirebaseFirestore.getInstance();

    private class UpdateCallDurationTask extends TimerTask {

        @Override
        public void run() {
            VideoCallScreenActivity.this.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            updateCallDuration();
                        }
                    });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
        savedInstanceState.putBoolean(VIEWS_TOGGLED, mToggleVideoViewPositions);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
        mToggleVideoViewPositions = savedInstanceState.getBoolean(VIEWS_TOGGLED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_call);

        mAudioPlayer = new AudioPlayer(this);
        mCallDuration = findViewById(R.id.callDuration);
        mCallerName = findViewById(R.id.remoteUser);
        mCallState = findViewById(R.id.callState);
        endCallButton = findViewById(R.id.hangupButton);
        profile_pic = findViewById(R.id.profile_pic);
        muteButton = findViewById(R.id.muteButton);
        speakerButton = findViewById(R.id.speakerButton);

        Utils.rippleRoundStroke(endCallButton, "#f44336", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(muteButton, "#424242", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(speakerButton, "#424242", "#ffffff", 360, 0, "#f44336");

        endCallButton.setOnClickListener(v -> endCall());

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

        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
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
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (!mAddedListener) {
                call.addCallListener(new SinchCallListener());
                mAddedListener = true;
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }

        audioController = getSinchServiceInterface().getAudioController();
        muteButton.setEnabled(true);
        speakerButton.setEnabled(true);

        updateUI();
    }

    private void updateUI() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            database.collection("users")
                    .document(getIntent().getStringExtra(SinchService.EXTRA_ID))
                    .get()
                    .addOnCompleteListener(
                            (task) -> {
                                mCallerName.setText(task.getResult().getString("displayName"));
                                Glide.with(VideoCallScreenActivity.this)
                                        .load(task.getResult().getString("userPhoto"))
                                        .skipMemoryCache(true)
                                        .thumbnail(0.1f)
                                        .into(profile_pic);
                            });
            mCallState.setText("Connecting");
            if (call.getDetails().isVideoOffered()) {
                if (call.getState() == CallState.ESTABLISHED) {
                    setVideoViewsVisibility(true, true);
                } else {
                    setVideoViewsVisibility(true, false);
                }
            }
        } else {
            setVideoViewsVisibility(false, false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mDurationTask.cancel();
        mTimer.cancel();
        removeVideoViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        mTimer = new Timer();
        mDurationTask = new UpdateCallDurationTask();
        mTimer.schedule(mDurationTask, 0, 500);
        updateUI();
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
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

    private ViewGroup getVideoView(boolean localView) {
        if (mToggleVideoViewPositions) {
            localView = !localView;
        }
        return localView ? findViewById(R.id.localVideo) : findViewById(R.id.remoteVideo);
    }

    private void addLocalView() {
        if (mLocalVideoViewAdded || getSinchServiceInterface() == null) {
            return; // early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(
                    () -> {
                        ViewGroup localView = getVideoView(true);
                        localView.addView(vc.getLocalView());
                        localView.setOnClickListener(v -> vc.toggleCaptureDevicePosition());
                        mLocalVideoViewAdded = true;
                        vc.setLocalVideoZOrder(!mToggleVideoViewPositions);
                    });
        }
    }

    private void addRemoteView() {
        if (mRemoteVideoViewAdded || getSinchServiceInterface() == null) {
            return; // early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(
                    () -> {
                        ViewGroup remoteView = getVideoView(false);
                        remoteView.addView(vc.getRemoteView());
                        remoteView.setOnClickListener(
                                (View v) -> {
                                    removeVideoViews();
                                    mToggleVideoViewPositions = !mToggleVideoViewPositions;
                                    addRemoteView();
                                    addLocalView();
                                });
                        mRemoteVideoViewAdded = true;
                        vc.setLocalVideoZOrder(!mToggleVideoViewPositions);
                    });
        }
    }

    private void removeVideoViews() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(
                    () -> {
                        ((ViewGroup) (vc.getRemoteView().getParent()))
                                .removeView(vc.getRemoteView());
                        ((ViewGroup) (vc.getLocalView().getParent())).removeView(vc.getLocalView());
                        mLocalVideoViewAdded = false;
                        mRemoteVideoViewAdded = false;
                    });
        }
    }

    private void setVideoViewsVisibility(
            final boolean localVideoVisibile, final boolean remoteVideoVisible) {
        if (getSinchServiceInterface() == null) return;
        if (mRemoteVideoViewAdded == false) {
            addRemoteView();
        }
        if (mLocalVideoViewAdded == false) {
            addLocalView();
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(
                    () -> {
                        vc.getLocalView()
                                .setVisibility(localVideoVisibile ? View.VISIBLE : View.GONE);
                        vc.getRemoteView()
                                .setVisibility(remoteVideoVisible ? View.VISIBLE : View.GONE);
                    });
        }
    }

    private class SinchCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended: " + call.getDetails().toString();
            mCallDuration.setVisibility(View.GONE);
            endCallButton.setVisibility(View.GONE);
            muteButton.setVisibility(View.GONE);
            speakerButton.setVisibility(View.GONE);
            timer =
                    new TimerTask() {
                        @Override
                        public void run() {
                            runOnUiThread(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            endCall();
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
            mCallDuration.setVisibility(View.VISIBLE);
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            audioController = getSinchServiceInterface().getAudioController();
            audioController.enableSpeaker();
            speakerButton.setImageResource(R.drawable.cmd_volume_high);
            if (call.getDetails().isVideoOffered()) {
                setVideoViewsVisibility(true, true);
            }
            Log.d(TAG, "Call offered video: " + call.getDetails().isVideoOffered());
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onVideoTrackAdded(Call call) {}

        @Override
        public void onVideoTrackPaused(Call call) {}

        @Override
        public void onVideoTrackResumed(Call call) {}
    }
}
