package gq.fora.app.activities;

import android.graphics.Camera;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.viewpager2.widget.ViewPager2;

import com.amrdeveloper.reactbutton.ReactButton;
import com.amrdeveloper.reactbutton.Reaction;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.r0adkll.slidr.Slidr;
import com.r0adkll.slidr.model.SlidrConfig;
import com.r0adkll.slidr.model.SlidrInterface;
import com.r0adkll.slidr.model.SlidrListener;
import com.r0adkll.slidr.model.SlidrPosition;
import com.vanniktech.emoji.EmojiPopup;
import com.vanniktech.emoji.EmojiTextView;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.fragments.CommentsFragment;
import gq.fora.app.models.ReactConstants;
import gq.fora.app.models.ReactionMode;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.Story;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.models.story.reactions.ReactionType;
import gq.fora.app.utils.Utils;

import omari.hamza.storyview.progress.StoriesProgressView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StoryView extends BaseFragment {

    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference stories = firebase.getReference("stories");
    private DatabaseReference users = firebase.getReference("users");
    private DatabaseReference comments = firebase.getReference("comments");
    private DatabaseReference reactions = firebase.getReference("reactions");

    private StoriesProgressView progressView;
    private TextView display_name, username, reactors;
    private ImageView options, close, avatar;
    private LinearLayout storyHeader, comment, comment_layout;
    private ViewPager2 storyPager;
    private ArrayList<Story> storyList = new ArrayList<>();
    private StoryAdapter adapter;
    private FrameLayout frameLayout;
    private int currentPosition;
    private SlidrInterface slidrInterface;
    private ReactButton reactButton;
    private ArrayList<ReactionType> reactType = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> commentsList = new ArrayList<>();

    private EmojiPopup emojiPopup;
    private String TAG = "FORA";
    private EmojiTextView emojiText;

    private BottomSheetDialogFragment commentsFragment;
    private Bundle data;
    private FragmentManager fg;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstance) {
        View _view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.activity_story_view, parent, false);
        init(savedInstance, _view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (slidrInterface == null) {
            slidrInterface =
                    Slidr.replace(
                            getView().findViewById(R.id.content_container),
                            new SlidrConfig.Builder().position(SlidrPosition.TOP).build());
        }
    }

    public void init(Bundle savedInstance, View view) {
        progressView = view.findViewById(R.id.progressView);
        storyPager = view.findViewById(R.id.storyPager);
        options = view.findViewById(R.id.options);
        close = view.findViewById(R.id.close);
        comment = view.findViewById(R.id.comment);
        display_name = view.findViewById(R.id.display_name);
        username = view.findViewById(R.id.username);
        avatar = view.findViewById(R.id.avatar);
        storyHeader = view.findViewById(R.id.storyHeader);
        frameLayout = view.findViewById(R.id.frameLayout);
        comment_layout = view.findViewById(R.id.comment_layout);
        reactButton = view.findViewById(R.id.reactButton);
        reactors = view.findViewById(R.id.reactors);

        Bundle bundle = this.getArguments();
        fg = getActivity().getSupportFragmentManager();
        commentsFragment = new CommentsFragment();
        data = new Bundle();

        storyHeader.bringToFront();
        progressView.bringToFront();
        storyHeader.setBackgroundColor(android.R.color.transparent);
        comment_layout.setBackgroundColor(android.R.color.transparent);

        close.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        options.setOnClickListener((View v) -> {});

        storyPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        storyPager.setPageTransformer(new DrawFromBackTransformer());

        stories.limitToLast((int) 50)
                .orderByChild("timestamp")
                .addChildEventListener(
                        new ChildEventListener() {

                            @Override
                            public void onChildAdded(DataSnapshot arg0, String arg1) {
                                Story user = arg0.getValue(Story.class);
                                storyList.add(user);
                                Collections.reverse(storyList);
                                adapter = new StoryAdapter(storyList);
                                storyPager.setAdapter(adapter);
                                adapter.notifyItemInserted(0);
                                storyPager.post(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                storyPager.setCurrentItem(
                                                        Integer.parseInt(
                                                                bundle.getString("position")),
                                                        true);
                                                reactType.clear();
                                            }
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

        view.setOnTouchListener(onTouchListener);

        progressView.setStoriesCount(1);
        progressView.setStoryDuration(5000L);
        progressView.setStoriesListener(
                new StoriesProgressView.StoriesListener() {
                    @Override
                    public void onNext() {}

                    @Override
                    public void onPrev() {}

                    @Override
                    public void onComplete() {
                        if (currentPosition == 50) {
                            getActivity().getSupportFragmentManager().popBackStack();
                        } else {
                            storyPager.setCurrentItem((currentPosition + 1));
                        }
                    }
                });
        progressView.startStories();

        storyPager.registerOnPageChangeCallback(
                new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageScrolled(
                            int position, float positionOffset, int positionOffsetPixels) {
                        super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                        reactType.clear();
                    }

                    @Override
                    public void onPageSelected(int position) {
                        super.onPageSelected(position);
                        currentPosition = position;
                        Log.e("Selected_Page", String.valueOf(position));

                        String key = storyList.get(position).getUserId();

                        users.child(storyList.get(position).getUserId())
                                .addListenerForSingleValueEvent(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot snapShot) {
                                                User user = snapShot.getValue(User.class);
                                                if (user != null) {
                                                    display_name.setText(user.displayName);
                                                    username.setText(
                                                            DateUtils.getRelativeTimeSpanString(
                                                                    getContext(),
                                                                    storyList
                                                                            .get(position)
                                                                            .getTimestamp()));
                                                    Glide.with(getActivity())
                                                            .load(user.userPhoto)
                                                            .skipMemoryCache(true)
                                                            .thumbnail(0.1f)
                                                            .circleCrop()
                                                            .into(avatar);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError arg0) {}
                                        });

                        progressView.setStoriesCount(1);
                        progressView.setStoryDuration(5000L);
                        progressView.setStoriesListener(
                                new StoriesProgressView.StoriesListener() {
                                    @Override
                                    public void onNext() {}

                                    @Override
                                    public void onPrev() {}

                                    @Override
                                    public void onComplete() {
                                        if (position == 50) {
                                            getActivity()
                                                    .getSupportFragmentManager()
                                                    .popBackStack();
                                        } else {
                                            storyPager.setCurrentItem((currentPosition + 1));
                                        }
                                    }
                                });
                        progressView.startStories();
                        comment.setOnClickListener(
                                (View v) -> {
                                    if (!key.isEmpty()) {
                                        showComment(key);
                                    }
                                });
                        reactions
                                .child(storyList.get(position).getUserId())
                                .addChildEventListener(
                                        new ChildEventListener() {
                                            @Override
                                            public void onChildAdded(
                                                    DataSnapshot arg0, String arg1) {
                                                ReactionType react =
                                                        arg0.getValue(ReactionType.class);
                                                reactType.add(react);
                                                if (react.getType().equals("Default")) {
                                                    reactors.setVisibility(View.GONE);
                                                } else {
                                                    reactors.setText(
                                                            reactType.size() + " reactions.");
                                                    reactors.setVisibility(View.VISIBLE);
                                                }
                                            }

                                            @Override
                                            public void onChildChanged(
                                                    DataSnapshot arg0, String arg1) {}

                                            @Override
                                            public void onChildMoved(
                                                    DataSnapshot arg0, String arg1) {}

                                            @Override
                                            public void onChildRemoved(DataSnapshot arg0) {}

                                            @Override
                                            public void onCancelled(DatabaseError arg0) {}
                                        });

                        reactions
                                .child(storyList.get(position).getUserId())
                                .child(UserConfig.getInstance().getUid())
                                .addListenerForSingleValueEvent(
                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot arg0) {
                                                if (arg0.exists()) {
                                                    ReactionType react =
                                                            arg0.getValue(ReactionType.class);
                                                    reactType.add(react);
                                                    if (react != null) {
                                                        if (react.getType().equals("Like")) {
                                                            reactButton.setCurrentReaction(
                                                                    new Reaction(
                                                                            ReactConstants.LIKE,
                                                                            ReactConstants.BLUE,
                                                                            R.drawable.ic_like));
                                                        } else if (react.getType().equals("Love")) {
                                                            reactButton.setCurrentReaction(
                                                                    new Reaction(
                                                                            ReactConstants.LOVE,
                                                                            ReactConstants.RED_LOVE,
                                                                            R.drawable.ic_heart));
                                                        } else if (react.getType()
                                                                .equals("Smile")) {
                                                            reactButton.setCurrentReaction(
                                                                    new Reaction(
                                                                            ReactConstants.SMILE,
                                                                            ReactConstants
                                                                                    .YELLOW_WOW,
                                                                            R.drawable.ic_happy));
                                                        } else if (react.getType().equals("Wow")) {
                                                            reactButton.setCurrentReaction(
                                                                    new Reaction(
                                                                            ReactConstants.WOW,
                                                                            ReactConstants
                                                                                    .YELLOW_WOW,
                                                                            R.drawable
                                                                                    .ic_surprise));
                                                        } else if (react.getType().equals("Sad")) {
                                                            reactButton.setCurrentReaction(
                                                                    new Reaction(
                                                                            ReactConstants.SAD,
                                                                            ReactConstants
                                                                                    .YELLOW_HAHA,
                                                                            R.drawable.ic_sad));
                                                        } else if (react.getType()
                                                                .equals("Angry")) {
                                                            reactButton.setCurrentReaction(
                                                                    new Reaction(
                                                                            ReactConstants.ANGRY,
                                                                            ReactConstants
                                                                                    .RED_ANGRY,
                                                                            R.drawable.ic_angry));
                                                        } else {
                                                            reactButton.setDefaultReaction(
                                                                    ReactionMode.defaultReact);
                                                        }
                                                    }
                                                } else {
                                                    reactButton.setDefaultReaction(
                                                            ReactionMode.defaultReact);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError arg0) {}
                                        });

                        storyPager.post(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        reactButton.setOnReactionChangeListener(
                                                new ReactButton.OnReactionChangeListener() {
                                                    @Override
                                                    public void onReactionChange(
                                                            Reaction reaction) {
                                                        Log.d(
                                                                "StoryView",
                                                                "onReactionChange: "
                                                                        + reaction.getReactText());
                                                        Map<String, Object> map = new HashMap<>();
                                                        map.put(
                                                                "reaction_type",
                                                                reaction.getReactText());
                                                        map.put(
                                                                "reactor_id",
                                                                UserConfig.getInstance().getUid());
                                                        Reaction currentReaction =
                                                                reactButton.getDefaultReaction();
                                                        if (!currentReaction.equals("Default")) {
                                                            reactions
                                                                    .child(
                                                                            storyList
                                                                                    .get(position)
                                                                                    .getUserId())
                                                                    .child(
                                                                            UserConfig.getInstance()
                                                                                    .getUid())
                                                                    .updateChildren(map);
                                                        }
                                                    }
                                                });
                                    }
                                });

                        reactButton.setOnReactionDialogStateListener(
                                new ReactButton.OnReactionDialogStateListener() {
                                    @Override
                                    public void onDialogOpened() {
                                        Log.d("StoryView", "onDialogOpened");
                                        progressView.pause();
                                    }

                                    @Override
                                    public void onDialogDismiss() {
                                        Log.d("StoryView", "onDialogDismiss");
                                        progressView.resume();
                                    }
                                });
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                        reactType.clear();
                    }
                });

        Utils.rippleEffects(close, "#e0e0e0");
        Utils.rippleEffects(options, "#e0e0e0");
        Utils.rippleRoundStroke(comment, "#65EEEEEE", "#E0E0E0", 60, 0, "#E0E0E0");

        SlidrConfig config =
                new SlidrConfig.Builder()
                        .primaryColor(getActivity().getResources().getColor(R.color.colorPrimary))
                        .secondaryColor(
                                getActivity().getResources().getColor(R.color.colorPrimaryDark))
                        .sensitivity(1f)
                        .scrimColor(Color.BLACK)
                        .scrimStartAlpha(0.8f)
                        .scrimEndAlpha(0f)
                        .velocityThreshold(2400)
                        .distanceThreshold(0.25f)
                        .edge(true)
                        .edgeSize(10)
                        .listener(
                                new SlidrListener() {
                                    @Override
                                    public void onSlideStateChanged(int arg0) {}

                                    @Override
                                    public void onSlideChange(float arg0) {}

                                    @Override
                                    public void onSlideOpened() {}

                                    @Override
                                    public boolean onSlideClosed() {
                                        getActivity().getSupportFragmentManager().popBackStack();
                                        return true;
                                    }
                                })
                        .build();

        Slidr.attach(getActivity(), config);

        Utils.rippleEffects(reactButton, "#eeeeee");

        reactButton.setReactions(ReactionMode.reactions);
        reactButton.setDefaultReaction(ReactionMode.defaultReact);
        reactButton.setEnableReactionTooltip(true);
        reactButton.setReactionTooltipShape(R.drawable.react_dialog_shape);

        reactButton.setOnReactionDialogStateListener(
                new ReactButton.OnReactionDialogStateListener() {
                    @Override
                    public void onDialogOpened() {
                        Log.d("StoryView", "onDialogOpened");
                        progressView.pause();
                    }

                    @Override
                    public void onDialogDismiss() {
                        Log.d("StoryView", "onDialogDismiss");
                        progressView.resume();
                    }
                });
    }

    long pressTime = 0L;
    long limit = 500L;

    private View.OnTouchListener onTouchListener =
            new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressTime = System.currentTimeMillis();
                            progressView.pause();
                            return false;
                        case MotionEvent.ACTION_UP:
                            long now = System.currentTimeMillis();
                            progressView.resume();
                            return limit < now - pressTime;
                    }
                    return false;
                }
            };

    @Override
    public void onDestroy() {
        progressView.destroy();
        super.onDestroy();
    }

    public class StoryAdapter extends RecyclerView.Adapter<StoryAdapter.ViewHolder> {

        private ArrayList<Story> story;

        public StoryAdapter(ArrayList<Story> story) {
            this.story = story;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View _view =
                    LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.fora_story_view, parent, false);
            return new ViewHolder(_view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Glide.with(getActivity())
                    .load(story.get(position).getImageUrl())
                    .skipMemoryCache(true)
                    .thumbnail(0.1f)
                    .fitCenter()
                    .into(holder.storyImage);
        }

        @Override
        public int getItemCount() {
            return story.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            private ImageView storyImage;

            public ViewHolder(View view) {
                super(view);
                storyImage = view.findViewById(R.id.storyImage);
            }
        }
    }

    public void showComment(String key) {
        progressView.pause();
        data.putString("comment_id", key);
        commentsFragment.setArguments(data);
        commentsFragment.setStyle(BottomSheetDialogFragment.STYLE_NORMAL, R.style.DialogStyle);

        /*To show BottomSheetDialogFragment.*/
        if (!commentsFragment.isAdded()) {
            commentsFragment.show(fg, "Comments");
        } else {
            commentsFragment.getDialog().dismiss();
        }

        fg.executePendingTransactions();
        commentsFragment
                .getDialog()
                .setOnDismissListener(
                        dialog -> {
                            progressView.resume();
                            commentsFragment.dismiss();
                        });
        commentsFragment
                .getDialog()
                .setOnShowListener(
                        dialog -> {
                            progressView.pause();
                        });
    }

    public abstract static class BaseTransformer implements ViewPager2.PageTransformer {

        protected abstract void onTransform(View view, float position);

        @Override
        public void transformPage(View view, float position) {
            onPreTransform(view, position);
            onTransform(view, position);
            onPostTransform(view, position);
        }

        protected boolean hideOffscreenPages() {
            return true;
        }

        protected boolean isPagingEnabled() {
            return false;
        }

        protected void onPreTransform(View view, float position) {
            final float width = view.getWidth();
            view.setRotationX(0);
            view.setRotationY(0);
            view.setRotation(0);
            view.setScaleX(1);
            view.setScaleY(1);
            view.setPivotX(0);
            view.setPivotY(0);
            view.setTranslationY(0);
            view.setTranslationX(isPagingEnabled() ? 0f : -width * position);
            if (hideOffscreenPages()) {
                view.setAlpha(position <= -1f || position >= 1f ? 0f : 1f);
            } else {
                view.setAlpha(1f);
            }
        }

        protected void onPostTransform(View view, float position) {}
    }

    public static class AccordionTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            view.setPivotX(position < 0 ? 0 : view.getWidth());
            view.setScaleX(position < 0 ? 1f + position : 1f - position);
        }
    }

    public static class BackgroundToForegroundTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            final float height = view.getHeight();
            final float width = view.getWidth();
            final float scale = min(position < 0 ? 1f : Math.abs(1f - position), 0.5f);
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setPivotX(width * 0.5f);
            view.setPivotY(height * 0.5f);
            view.setTranslationX(position < 0 ? width * position : -width * position * 0.25f);
        }

        private static final float min(float val, float min) {
            return val < min ? min : val;
        }
    }

    public static class CubeInTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            view.setPivotX(position > 0 ? 0 : view.getWidth());
            view.setPivotY(0);
            view.setRotationY(-90f * position);
        }

        @Override
        public boolean isPagingEnabled() {
            return true;
        }
    }

    public static class CubeOutTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            view.setPivotX(position < 0f ? view.getWidth() : 0f);
            view.setPivotY(view.getHeight() * 0.5f);
            view.setRotationY(90f * position);
        }

        @Override
        public boolean isPagingEnabled() {
            return true;
        }
    }

    public static class DefaultTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {}

        @Override
        public boolean isPagingEnabled() {
            return true;
        }
    }

    public static class DepthPageTransformer extends BaseTransformer {
        private static final float MIN_SCALE = 0.75f;

        @Override
        protected void onTransform(View view, float position) {
            if (position <= 0f) {
                view.setTranslationX(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);
            } else if (position <= 1f) {
                final float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setAlpha(1 - position);
                view.setPivotY(0.5f * view.getHeight());
                view.setTranslationX(view.getWidth() * -position);
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        }

        @Override
        protected boolean isPagingEnabled() {
            return true;
        }
    }

    public static class ZoomOutTranformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            final float scale = 1f + Math.abs(position);
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(view.getHeight() * 0.5f);
            view.setAlpha(position < -1f || position > 1f ? 0f : 1f - (scale - 1f));
            if (position == -1) {
                view.setTranslationX(view.getWidth() * -1);
            }
        }
    }

    public static class StackTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            view.setTranslationX(position < 0 ? 0f : -view.getWidth() * position);
        }
    }

    public static class TabletTransformer extends BaseTransformer {
        private static final Matrix OFFSET_MATRIX = new Matrix();
        private static final Camera OFFSET_CAMERA = new Camera();
        private static final float[] OFFSET_TEMP_FLOAT = new float[2];

        @Override
        protected void onTransform(View view, float position) {
            final float rotation = (position < 0 ? 30f : -30f) * Math.abs(position);
            view.setTranslationX(
                    getOffsetXForRotation(rotation, view.getWidth(), view.getHeight()));
            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(0);
            view.setRotationY(rotation);
        }

        protected static final float getOffsetXForRotation(float degrees, int width, int height) {
            OFFSET_MATRIX.reset();
            OFFSET_CAMERA.save();
            OFFSET_CAMERA.rotateY(Math.abs(degrees));
            OFFSET_CAMERA.getMatrix(OFFSET_MATRIX);
            OFFSET_CAMERA.restore();
            OFFSET_MATRIX.preTranslate(-width * 0.5f, -height * 0.5f);
            OFFSET_MATRIX.postTranslate(width * 0.5f, height * 0.5f);
            OFFSET_TEMP_FLOAT[0] = width;
            OFFSET_TEMP_FLOAT[1] = height;
            OFFSET_MATRIX.mapPoints(OFFSET_TEMP_FLOAT);
            return (width - OFFSET_TEMP_FLOAT[0]) * (degrees > 0.0f ? 1.0f : -1.0f);
        }
    }

    public static class ZoomInTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            final float scale = position < 0 ? position + 1f : Math.abs(1f - position);
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(view.getHeight() * 0.5f);
            view.setAlpha(position < -1f || position > 1f ? 0f : 1f - (scale - 1f));
        }
    }

    public static class ZoomOutSlideTransformer extends BaseTransformer {
        private static final float MIN_SCALE = 0.85f;
        private static final float MIN_ALPHA = 0.5f;

        @Override
        protected void onTransform(View view, float position) {
            if (position >= -1 || position <= 1) {
                final float height = view.getHeight();
                final float scaleFactor = Math.max(MIN_SCALE, 1 - Math.abs(position));
                final float vertMargin = height * (1 - scaleFactor) / 2;
                final float horzMargin = view.getWidth() * (1 - scaleFactor) / 2;
                view.setPivotY(0.5f * height);
                if (position < 0) {
                    view.setTranslationX(horzMargin - vertMargin / 2);
                } else {
                    view.setTranslationX(-horzMargin + vertMargin / 2);
                }
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
                view.setAlpha(
                        MIN_ALPHA + (scaleFactor - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA));
            }
        }
    }

    public static class ForegroundToBackgroundTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            final float height = view.getHeight();
            final float width = view.getWidth();
            final float scale = min(position > 0 ? 1f : Math.abs(1f + position), 0.5f);
            view.setScaleX(scale);
            view.setScaleY(scale);
            view.setPivotX(width * 0.5f);
            view.setPivotY(height * 0.5f);
            view.setTranslationX(position > 0 ? width * position : -width * position * 0.25f);
        }

        private static final float min(float val, float min) {
            return val < min ? min : val;
        }
    }

    public static class ParallaxPageTransformer extends BaseTransformer {
        private final int viewToParallax;

        public ParallaxPageTransformer(final int viewToParallax) {
            this.viewToParallax = viewToParallax;
        }

        @Override
        protected void onTransform(View view, float position) {
            int pageWidth = view.getWidth();
            if (position < -1) {
                view.setAlpha(1);
            } else if (position <= 1) {
                view.findViewById(viewToParallax).setTranslationX(-position * (pageWidth / 2));
            } else {
                view.setAlpha(1);
            }
        }
    }

    public static class RotateDownTransformer extends BaseTransformer {
        private static final float ROT_MOD = -15f;

        @Override
        protected void onTransform(View view, float position) {
            final float width = view.getWidth();
            final float height = view.getHeight();
            final float rotation = ROT_MOD * position * -1.25f;
            view.setPivotX(width * 0.5f);
            view.setPivotY(height);
            view.setRotation(rotation);
        }

        @Override
        protected boolean isPagingEnabled() {
            return true;
        }
    }

    public static class RotateUpTransformer extends BaseTransformer {
        private static final float ROT_MOD = -15f;

        @Override
        protected void onTransform(View view, float position) {
            final float width = view.getWidth();
            final float rotation = ROT_MOD * position;
            view.setPivotX(width * 0.5f);
            view.setPivotY(0f);
            view.setTranslationX(0f);
            view.setRotation(rotation);
        }

        @Override
        protected boolean isPagingEnabled() {
            return true;
        }
    }

    public static class DrawFromBackTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        @Override
        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();
            if (position < -1 || position > 1) {
                view.setAlpha(0);
                return;
            }
            if (position <= 0) {
                view.setAlpha(1 + position);
                view.setTranslationX(pageWidth * -position);
                float scaleFactor = MIN_SCALE + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
                return;
            }
            if (position > 0.5 && position <= 1) {
                view.setAlpha(0);
                view.setTranslationX(pageWidth * -position);
                return;
            }
            if (position > 0.3 && position <= 0.5) {
                view.setAlpha(1);
                view.setTranslationX(pageWidth * position);
                float scaleFactor = MIN_SCALE;
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
                return;
            }
            if (position <= 0.3) {
                view.setAlpha(1);
                view.setTranslationX(pageWidth * position);
                float v = (float) (0.3 - position);
                v = v >= 0.25f ? 0.25f : v;
                float scaleFactor = MIN_SCALE + v;
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);
            }
        }
    }

    public static class FlipHorizontalTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            final float rotation = 180f * position;
            view.setVisibility(rotation > 90f || rotation < -90f ? View.INVISIBLE : View.VISIBLE);
            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(view.getHeight() * 0.5f);
            view.setRotationY(rotation);
        }
    }

    public static class FlipVerticalTransformer extends BaseTransformer {
        @Override
        protected void onTransform(View view, float position) {
            final float rotation = -180f * position;
            view.setVisibility(rotation > 90f || rotation < -90f ? View.INVISIBLE : View.VISIBLE);
            view.setPivotX(view.getWidth() * 0.5f);
            view.setPivotY(view.getHeight() * 0.5f);
            view.setRotationX(rotation);
        }
    }
}
