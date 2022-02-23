package gq.fora.app.activities.meeting;

import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
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
import gq.fora.app.activities.calling.BaseActivity;
import gq.fora.app.adapter.RoomParticipantListAdapter;
import gq.fora.app.models.AudioPlayer;
import gq.fora.app.models.Participant;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RoomActivity extends BaseActivity {

    static final String TAG = RoomActivity.class.getSimpleName();

    private AudioPlayer mAudioPlayer;
    private Timer mTimer;
    private UpdateCallDurationTask mDurationTask;
    private TimerTask timer;
    private Timer _timer = new Timer();
    private AudioController audioController;

    private String mCallId;
    private RecyclerView rv_participant_list;
    private ArrayList<Participant> participantList = new ArrayList<>();

    private TextView mCallDuration;
    private TextView mCallState;
    private TextView mCallerName;
    private TextView display_name;
    private TextView room_name, room_id;
    private ImageView endCallButton, answerCallButton, muteButton, speakerButton;
    private ImageView profile_pic;
    private boolean isMute = true;
    private boolean isSpeakerOn = true;

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private DatabaseReference messages = _firebase.getReference("messages");
    private DatabaseReference conversations = _firebase.getReference("conversations");
    private DatabaseReference rooms = _firebase.getReference("rooms");
    private DatabaseReference participants = _firebase.getReference("participants");

    private class UpdateCallDurationTask extends TimerTask {

        @Override
        public void run() {
            RoomActivity.this.runOnUiThread(
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
        setContentView(R.layout.activity_room);

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
        room_name = findViewById(R.id.room_name);
        room_id = findViewById(R.id.room_id);
        rv_participant_list = findViewById(R.id.rv_participant_list);

        endCallButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        endCall();
                    }
                });
        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);

        room_id.setEnabled(false);

        Utils.rippleRoundStroke(endCallButton, "#f44336", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(muteButton, "#424242", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(speakerButton, "#424242", "#ffffff", 360, 0, "#f44336");
        Utils.rippleRoundStroke(room_id, "#424242", "#ffffff", 10, 0, "#f44336");
        answerCallButton.setVisibility(View.GONE);
        mCallDuration.setVisibility(View.GONE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
            Window w = RoomActivity.this.getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(0xFF000000);
        }

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

        room_id.setOnClickListener(
                (View v) -> {
                    Call call = getSinchServiceInterface().getCall(mCallId);
                    Utils text = new Utils(RoomActivity.this);
                    text.copyText(call.getHeaders().get("room_id"));
                    Toast.makeText(RoomActivity.this, "Room ID Copied.", Toast.LENGTH_SHORT).show();
                });

        muteButton.setEnabled(false);
        speakerButton.setEnabled(false);
    }

    @Override
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.addCallListener(new SinchCallListener());
            mCallState.setText("CONNECTING");
            room_name.setText(call.getHeaders().get("room_name"));
            room_id.setText(call.getHeaders().get("room_id"));
            getParticipantList();
            users.child(call.getHeaders().get("creator_id"))
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {

                                @Override
                                public void onDataChange(DataSnapshot arg0) {
                                    User user = arg0.getValue(User.class);

                                    if (user != null) {
                                        display_name.setText("Hosted by " + user.displayName);
                                        Glide.with(RoomActivity.this)
                                                .load(user.userPhoto)
                                                .skipMemoryCache(true)
                                                .thumbnail(0.1f)
                                                .into(profile_pic);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError arg0) {}
                            });

            rooms.child(call.getHeaders().get("creator_id"))
                    .addListenerForSingleValueEvent(
                            new ValueEventListener() {

                                @Override
                                public void onDataChange(DataSnapshot arg0) {
                                    room_name.setText(
                                            arg0.child("room_name").getValue(String.class));
                                }

                                @Override
                                public void onCancelled(DatabaseError arg0) {}
                            });
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }
        audioController = getSinchServiceInterface().getAudioController();
        muteButton.setEnabled(true);
        speakerButton.setEnabled(true);
        room_id.setEnabled(true);
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
        if (call.getHeaders().get("creator_id").equals(UserConfig.getInstance().getUid())) {
            Map<String, Object> map = new HashMap<>();
            map.put("isRoomActive", false);
            rooms.child(UserConfig.getInstance().getUid()).updateChildren(map);
        }
        /*Remove Participants*/
        participants
                .child(call.getHeaders().get("creator_id"))
                .child(UserConfig.getInstance().getUid())
                .removeValue();
        /*Remove Participants onDisconnect*/
        participants
                .child(call.getHeaders().get("creator_id"))
                .child(UserConfig.getInstance().getUid())
                .onDisconnect()
                .removeValue();
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
            mCallDuration.setVisibility(View.GONE);
            endCallButton.setVisibility(View.GONE);
            muteButton.setVisibility(View.GONE);
            speakerButton.setVisibility(View.GONE);
            mCallState.setText("Room Ended");
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
            _timer.schedule(timer, (int) (1500));
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            mAudioPlayer.stopProgressTone();
            mCallState.setText("LIVE");
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            AudioController audioController = getSinchServiceInterface().getAudioController();
            audioController.disableSpeaker();
            mCallDuration.setVisibility(View.VISIBLE);
        }

        @Override
        public void onCallProgressing(Call call) {
            Log.d(TAG, "Call progressing");
            mCallState.setText("Processing...");
            mAudioPlayer.playProgressTone();
        }

        public void onShouldSendPushNotification(Call arg0, List<PushPair> arg1) {}
    }

    public void getParticipantList() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        participants
                .child(call.getHeaders().get("creator_id"))
                .addChildEventListener(
                        new ChildEventListener() {

                            @Override
                            public void onChildAdded(DataSnapshot arg0, String arg1) {

                                Participant user = arg0.getValue(Participant.class);
                                participantList.add(user);
                                rv_participant_list.setAdapter(
                                        new RoomParticipantListAdapter(participantList));
                                rv_participant_list.setHasFixedSize(true);
                                rv_participant_list.setLayoutManager(
                                        new LinearLayoutManager(
                                                RoomActivity.this,
                                                LinearLayoutManager.HORIZONTAL,
                                                false));

                                users.child(user.getParticipantId())
                                        .addListenerForSingleValueEvent(
                                                new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(DataSnapshot arg0) {
														User models = arg0.getValue(User.class);
														
														if (models != null) {
															Snackbar.make(endCallButton, models.displayName + " joined.", Snackbar.LENGTH_LONG).show();
														}
													}

                                                    @Override
                                                    public void onCancelled(DatabaseError arg0) {}
                                                });
                            }

                            @Override
                            public void onChildChanged(DataSnapshot arg0, String arg1) {}

                            @Override
                            public void onChildRemoved(DataSnapshot arg0) {}

                            @Override
                            public void onChildMoved(DataSnapshot arg0, String arg1) {}

                            @Override
                            public void onCancelled(DatabaseError arg0) {}
                        });
    }
}
