package gq.fora.app.activities.surface.video;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.universalvideoview.UniversalMediaController;
import com.universalvideoview.UniversalVideoView;
import gq.fora.app.R;

public class VideoPlayerActivity extends Fragment {

    private FrameLayout video_view;
    private UniversalVideoView videoView;
    private UniversalMediaController mediaController;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().finishAffinity();
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_video_player, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {
        video_view = view.findViewById(R.id.video_view);
    }

    public void initializeLogic() {
        View player = getActivity().getLayoutInflater().inflate(R.layout.video_player_view, null);
        video_view.addView(player);

        videoView = player.findViewById(R.id.videoView);
        mediaController = player.findViewById(R.id.mediaController);

        Bundle bundle = this.getArguments();
        if (bundle != null) {
            mediaController.setTitle(bundle.getString("videoUrl"));
            videoView.setVideoURI(Uri.parse(bundle.getString("videoUrl")));
        }
        mediaController.setKeepScreenOn(true);
        mediaController.setTextAlignment(UniversalMediaController.TEXT_ALIGNMENT_TEXT_START);
        mediaController.setSaveEnabled(true);

        videoView.setOnPreparedListener(
                new MediaPlayer.OnPreparedListener() {

                    @Override
                    public void onPrepared(MediaPlayer arg0) {}
                });

        videoView.setOnCompletionListener(
                new MediaPlayer.OnCompletionListener() {

                    @Override
                    public void onCompletion(MediaPlayer arg0) {}
                });

        videoView.setOnErrorListener(
                new MediaPlayer.OnErrorListener() {

                    @Override
                    public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
                        return false;
                    }
                });
    }
}
