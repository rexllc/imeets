package gq.fora.app.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.github.ybq.android.spinkit.sprite.Sprite;
import com.github.ybq.android.spinkit.style.WanderingCubes;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.sinch.android.rtc.calling.Call;

import dev.shreyaspatil.MaterialDialog.MaterialDialog;
import dev.shreyaspatil.MaterialDialog.interfaces.DialogInterface;

import gq.fora.app.R;
import gq.fora.app.activities.ChatActivity;
import gq.fora.app.activities.StoryView;
import gq.fora.app.activities.meeting.RoomActivity;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.activities.surface.SearchActivity;
import gq.fora.app.adapter.ChatListAdapter;
import gq.fora.app.adapter.StoryListAdapter;
import gq.fora.app.initializeApp;
import gq.fora.app.listener.onclick.RecyclerItemClickListener;
import gq.fora.app.models.Constants;
import gq.fora.app.models.Conversation;
import gq.fora.app.models.Room;
import gq.fora.app.models.RoomBuilder;
import gq.fora.app.models.RoomParticipant;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.Story;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.models.story.ContentType;
import gq.fora.app.models.story.StoryBuilder;
import gq.fora.app.notify.Notify;
import gq.fora.app.service.SinchService;
import gq.fora.app.utils.FileUtils;
import gq.fora.app.utils.ForaUtil;
import gq.fora.app.utils.Utils;
import gq.fora.app.widgets.NestedHorizontalScrollView;

import kotlin.Unit;

import omari.hamza.storyview.model.MyStory;
import omari.hamza.storyview.utils.StoryViewHeaderInfo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatsFragment extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private CollectionReference inboxRef = database.collection("inbox");
    private ConstraintLayout add_story;
    private ImageView search_icon, add_story_pic;
    private CoordinatorLayout coordinator;
    private RecyclerView rv_chat_list;
    private RecyclerView rv_user_list;
    private ArrayList<Story> userList = new ArrayList<>();
    private ArrayList<Conversation> convoList = new ArrayList<>();
    private ArrayList<User> user = new ArrayList<>();
    private String TAG = "FORA";
    private SwipeRefreshLayout swiperefreshlayout1;
    private SharedPreferences data;
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference users = _firebase.getReference("users");
    private DatabaseReference messages =
            _firebase.getReference("conversations/" + UserConfig.getInstance().getUid());
    private DatabaseReference chats =
            _firebase.getReference("messages/" + UserConfig.getInstance().getUid());
    private DatabaseReference stories = _firebase.getReference("stories");
    private DatabaseReference rooms = _firebase.getReference("rooms");
    private DatabaseReference participants = _firebase.getReference("participants");
    private ChildEventListener _users_child_listener;
    private FirebaseStorage storage = FirebaseStorage.getInstance("gs://fora-store.appspot.com");
    private StorageReference storageRef = storage.getReference();
    private UploadTask uploadTask;
    private ProgressBar loader, progressbar1;
    private boolean loading;
    private int currentPage = 20;
    private long totalPages = 20;
    private LinearLayoutManager layoutManager;
    private Query query;
    private ChildEventListener childEventListener;
    private ArrayList<MyStory> myStories;
    private ArrayList<StoryViewHeaderInfo> headerInfoArrayList;
    private Calendar cal = Calendar.getInstance();
    private User userInfo;
    private ExecutorService executor;
    private Handler handler;
    private ChatListAdapter layoutAdapter;
    private NestedHorizontalScrollView story_list;
    private TimerTask timer;
    private Timer _timer = new Timer();
    private StoryListAdapter adapter;
    private LinearLayout search_layout, holder;
    private TextView search_text, add_story_text, textview1;

    private String mAccountType = "gq.fora.app";
    private double position = 0;
    private SharedPreferences sharedPreferences;
    private static final int PICKER_REQUEST_IMAGE = 100;
    private Notify notify;

    private ActivityResultLauncher<Intent> launcher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    (ActivityResult result) -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Uri uri = result.getData().getData();
                            // Use the uri to load the image
                            executor.execute(
                                    () -> {
                                        // Background work here
                                        uploadStory(uri);
                                        handler.post(
                                                () -> {
                                                    // UI Thread work here

                                                });
                                    });
                        } else if (result.getResultCode() == ImagePicker.RESULT_ERROR) {
                            // Use ImagePicker.Companion.getError(result.getData()) to show an error
                        }
                    });

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.fragment_chats, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        return _view;
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {

        rv_chat_list = view.findViewById(R.id.rv_chat_list);
        rv_user_list = view.findViewById(R.id.rv_user_list);
        search_layout = view.findViewById(R.id.search_layout);
        search_icon = view.findViewById(R.id.search_icon);
        add_story = view.findViewById(R.id.add_story);
        add_story_text = view.findViewById(R.id.add_story_text);
        loader = view.findViewById(R.id.loader);
        swiperefreshlayout1 = view.findViewById(R.id.swiperefreshlayout1);
        add_story_pic = view.findViewById(R.id.add_story_pic);
        holder = view.findViewById(R.id.holder);
        progressbar1 = view.findViewById(R.id.progressbar1);
        textview1 = view.findViewById(R.id.textview1);
        story_list = view.findViewById(R.id.story_list);
        coordinator = view.findViewById(R.id.coordinator);
        search_text = view.findViewById(R.id.search_text);

        sharedPreferences = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);

        rv_chat_list.setNestedScrollingEnabled(false);

        executor = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        story_list.setHorizontalScrollBarEnabled(false);

        notify = new Notify(getActivity());
        notify.setTitle("Syncing Messages");
        notify.setContent("iMeets is syncing your messages.");
        notify.setProgress(100, 100, true);
        notify.setChannelId("Updates");
        notify.setChannelName("Updates");
        notify.setId(10);
        notify.setAutoCancel(true);
        notify.setImportance(Notify.Importance.MIN);
        notify.setChannelDescription("Checking for updates");
        notify.enableVibration(true);
        notify.setColor(R.color.primary);
        notify.show();

        data = getActivity().getSharedPreferences("token", Activity.MODE_PRIVATE);

        if (convoList.size() > 0) {
            progressbar1.setVisibility(View.VISIBLE);
            textview1.setVisibility(View.GONE);
        } else {
            holder.setVisibility(View.VISIBLE);
            progressbar1.setVisibility(View.GONE);
            textview1.setVisibility(View.VISIBLE);
            story_list.setVisibility(View.GONE);
            rv_chat_list.setVisibility(View.GONE);
        }

        textview1.setText("Loading Messages...");
        textview1.setTextColor(0xFFF43159);

        layoutManager = new LinearLayoutManager(getActivity());

        layoutAdapter = new ChatListAdapter();
        rv_chat_list.setAdapter(layoutAdapter);
        rv_chat_list.setHasFixedSize(true);
        rv_chat_list.setLayoutManager(layoutManager);

        search_layout.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new SearchActivity())
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .addToBackStack(null)
                            .commit();
                });

        swiperefreshlayout1.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        swiperefreshlayout1.setRefreshing(false);
                    }
                });
        add_story.setOnClickListener(
                (View v) -> {
                    openBottomSheet();
                });

        layoutAdapter.setOnItemClickListener(
                new ChatListAdapter.ItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Fragment chatFragment = new ChatActivity();
                        Bundle bundle = new Bundle();
                        getActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .setCustomAnimations(R.anim.slide_in, R.anim.slide_out)
                                .add(android.R.id.content, chatFragment)
                                .addToBackStack(null)
                                .commit();
                        bundle.putString("id", convoList.get(position).chatId);
                        chatFragment.setArguments(bundle);
                    }

                    @Override
                    public void onItemLongClick(View view, int position) {
                        openMenu(convoList.get(position).chatId, position);
                    }
                });

        rv_user_list.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        getActivity(),
                        rv_user_list,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                Fragment storyFragment = new StoryView();
                                Bundle bundle = new Bundle();
                                getActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .add(android.R.id.content, storyFragment)
                                        .addToBackStack(null)
                                        .commit();
                                bundle.putString("position", String.valueOf(position));
                                storyFragment.setArguments(bundle);
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                // do whatever
                            }
                        }));

        myStories = new ArrayList<>();
        headerInfoArrayList = new ArrayList<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");

        database.collection("stories")
                .limitToLast(20)
                .orderBy("timestamp")
                .addSnapshotListener(
                        new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(
                                    QuerySnapshot value, FirebaseFirestoreException error) {
                                if (error != null) return;

                                if (value != null) {
                                    for (DocumentChange docs : value.getDocumentChanges()) {
                                        Story story = new Story();
                                        story.userId = docs.getDocument().getString("userId");
                                        story.imageUrl = docs.getDocument().getString("imageUrl");
                                        story.timestamp = docs.getDocument().getLong("timestamp");
                                        userList.add(story);
                                    }
                                }

                                adapter = new StoryListAdapter(userList);
                                rv_user_list.setAdapter(adapter);
                                rv_user_list.setHasFixedSize(true);
                                rv_user_list.setLayoutManager(
                                        new LinearLayoutManager(
                                                getActivity(),
                                                LinearLayoutManager.HORIZONTAL,
                                                false));
                                if (userList.size() == 0) {
                                    adapter.notifyDataSetChanged();
                                } else {
                                    adapter.notifyItemInserted(0);
                                }
                            }
                        });

        layoutManager = new LinearLayoutManager(getActivity());

        loading = true;

        getInboxList(20);

        rv_chat_list.addOnScrollListener(
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);

                        int totalItemCount = layoutManager.getItemCount();
                        int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                        if (!loading && totalItemCount <= (lastVisibleItem + 1)) {
                            // End of the items
                            if (currentPage < totalPages) {
                                getInboxList(currentPage + 3);
                                loading = true;
                                loader.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });
    }

    public void initializeLogic() {
        Utils.rippleRoundStroke(search_layout, "#EEEEEE", "#E0E0E0", 60, 0, "#E0E0E0");
        Utils.rippleRoundStroke(add_story_pic, "#EEEEEE", "#E0E0E0", 360, 0, "#E0E0E0");
        search_icon.setColorFilter(0xFF9E9E9E);
        checkTheme();
    }

    public void uploadStory(Uri uri) {
        StorageReference picRef =
                storageRef.child(
                        "story/images/"
                                + UserConfig.getInstance().getUid()
                                + "/"
                                + uri.getLastPathSegment());
        uploadTask = picRef.putFile(uri);

        uploadTask
                .addOnProgressListener(
                        new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                double progress =
                                        (100.0 * taskSnapshot.getBytesTransferred())
                                                / taskSnapshot.getTotalByteCount();
                                int currentprogress = (int) progress;
                                showNotification(
                                        "Fora", "Uploading Story...", 100, currentprogress);
                            }
                        })
                .addOnPausedListener(
                        new OnPausedListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                                System.out.println("Upload is paused");
                            }
                        })
                .continueWithTask(
                        new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(Task<UploadTask.TaskSnapshot> task)
                                    throws Exception {
                                return picRef.getDownloadUrl();
                            }
                        })
                .addOnCompleteListener(
                        new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    StoryBuilder myStory =
                                            new StoryBuilder(
                                                    UserConfig.getInstance().getUid(),
                                                    task.getResult().toString(),
                                                    stories.push().getKey(),
                                                    ContentType.IMAGE);
                                    stories.child(UserConfig.getInstance().getUid())
                                            .setValue(myStory)
                                            .addOnSuccessListener(
                                                    new OnSuccessListener() {

                                                        @Override
                                                        public void onSuccess(Object arg0) {
                                                            showNotification(
                                                                    "Fora", "Story updated.", 0, 0);
                                                            Toast.makeText(
                                                                            initializeApp.context,
                                                                            "Upload complete.",
                                                                            Toast.LENGTH_SHORT)
                                                                    .show();
                                                        }
                                                    });
                                } else {
                                    // Handle failures
                                    // ...
                                }
                            }
                        });
    }

    public void showNotification(String title, String body, int progress, int max) {
        try {
            Notify.create(initializeApp.context)
                    .setTitle(title)
                    .setContent(body)
                    .setProgress(max, progress, false)
                    .setChannelId("1")
                    .setChannelName("Upload Task")
                    .setChannelDescription("Notification for uploading.")
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setImportance(Notify.Importance.HIGH)
                    .setAutoCancel(true)
                    .enableVibration(true)
                    .setId(100)
                    .setColor(R.color.primary)
                    .show();
        } catch (Exception ex) {
            ex.printStackTrace();
            FileUtils.writeFile(
                    ex.toString(),
                    FileUtils.getPublicDir(Environment.DIRECTORY_DOCUMENTS)
                            + "/"
                            + "logs-"
                            + System.currentTimeMillis()
                            + ".txt");
        }
    }

    public void openBottomSheet() {
        BottomSheetDialog sheet =
                new BottomSheetDialog(getActivity(), R.style.AppBottomSheetDialogTheme);
        View sheetView =
                getActivity().getLayoutInflater().inflate(R.layout.bottom_sheet_create, null);
        sheet.setContentView(sheetView);

        final LinearLayout create_story = (LinearLayout) sheetView.findViewById(R.id.create_story);
        final LinearLayout create_room = (LinearLayout) sheetView.findViewById(R.id.create_room);
        final LinearLayout join_room = (LinearLayout) sheetView.findViewById(R.id.join_room);
        final LinearLayout line = (LinearLayout) sheetView.findViewById(R.id.line);
        final ImageView story = (ImageView) sheetView.findViewById(R.id.story);
        final ImageView room = (ImageView) sheetView.findViewById(R.id.room);
        final ImageView room_1 = (ImageView) sheetView.findViewById(R.id.room_1);
        final TextView create_story_text =
                (TextView) sheetView.findViewById(R.id.create_story_text);
        final TextView create_room_text = (TextView) sheetView.findViewById(R.id.create_room_text);
        final TextView join_room_text = (TextView) sheetView.findViewById(R.id.join_room_text);

        Utils.rippleRoundStroke(create_story, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(create_room, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(join_room, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");

        story.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        room.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        room_1.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        line.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(60, 0xFFE0E0E0));

        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            sheetView.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.rounded_dialog_night));
            story.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(360, 0xFF424242));

            room.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(360, 0xFF424242));

            room_1.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(360, 0xFF424242));

            line.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(60, 0xFF424242));

            story.setColorFilter(0xFFFFFFFF);
            room.setColorFilter(0xFFFFFFFF);
            room_1.setColorFilter(0xFFFFFFFF);
            create_story_text.setTextColor(0xFFFFFFFF);
            create_room_text.setTextColor(0xFFFFFFFF);
            join_room_text.setTextColor(0xFFFFFFFF);
            Utils.rippleRoundStroke(create_story, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(create_room, "#212121", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(join_room, "#212121", "#e0e0e0", 0, 0, "#ffffff");
        } else {
            sheetView.setBackground(
                    ContextCompat.getDrawable(getActivity(), R.drawable.rounded_dialog));
            story.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(360, 0xFFEEEEEE));

            room.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(360, 0xFFEEEEEE));

            room_1.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(360, 0xFFEEEEEE));

            line.setBackground(
                    new GradientDrawable() {
                        public GradientDrawable getIns(int a, int b) {
                            this.setCornerRadius(a);
                            this.setColor(b);
                            return this;
                        }
                    }.getIns(60, 0xFFE0E0E0));

            story.setColorFilter(0xFF000000);
            room.setColorFilter(0xFF000000);
            room_1.setColorFilter(0xFF000000);
            create_story_text.setTextColor(0xFF000000);
            create_room_text.setTextColor(0xFF000000);
            join_room_text.setTextColor(0xFF000000);
            Utils.rippleRoundStroke(create_story, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(create_room, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
            Utils.rippleRoundStroke(join_room, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        }

        create_story.setOnClickListener(
                (View v) -> {
                    ImagePicker.with(getActivity())
                            .crop() // Crop image(Optional), Check Customization for more option
                            .compress(524)
                            .galleryOnly() // We have to define what image provider we want to use
                            .galleryMimeTypes(new String[] {"image/png", "image/jpg", "image/jpeg"})
                            .maxResultSize(
                                    1080, 1080) // Final image resolution will be less than 1080 x
                            // 1080(Optional)
                            .createIntent(
                                    it -> {
                                        launcher.launch(it);
                                        return Unit.INSTANCE;
                                    });
                    sheet.dismiss();
                });

        join_room.setOnClickListener(
                (View v) -> {
                    AlertDialog.Builder dialog2 =
                            new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
                    View dialogView2 =
                            getActivity()
                                    .getLayoutInflater()
                                    .inflate(R.layout.dialog_edittext, null);
                    final EditText edittext1 = (EditText) dialogView2.findViewById(R.id.edittext1);
                    final ImageView imageview1 =
                            (ImageView) dialogView2.findViewById(R.id.imageview1);

                    imageview1.setImageResource(R.drawable.cmd_account_multiple);

                    edittext1.setHint("Enter a Room ID");

                    edittext1.setBackground(
                            new GradientDrawable() {
                                public GradientDrawable getIns(int a, int b) {
                                    this.setCornerRadius(a);
                                    this.setColor(b);
                                    return this;
                                }
                            }.getIns(10, 0xFFEEEEEE));

                    imageview1.setBackground(
                            new GradientDrawable() {
                                public GradientDrawable getIns(int a, int b) {
                                    this.setCornerRadius(a);
                                    this.setColor(b);
                                    return this;
                                }
                            }.getIns(360, 0xFFEEEEEE));

                    dialog2.setTitle("Join Room");
                    dialog2.setView(dialogView2);
                    dialog2.setPositiveButton(
                            "Join Room",
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface _dialog, int _which) {
                                    if (!edittext1.getText().toString().isEmpty()) {
                                        rooms.orderByChild("room_id")
                                                .equalTo(edittext1.getText().toString())
                                                .addListenerForSingleValueEvent(
                                                        new ValueEventListener() {

                                                            @Override
                                                            public void onDataChange(
                                                                    DataSnapshot ds) {
                                                                Room rooms =
                                                                        ds.getValue(Room.class);

                                                                if (ds.exists()) {

                                                                    if (rooms.isRoomActive()) {
                                                                        for (DataSnapshot snapShot :
                                                                                ds.getChildren()) {
                                                                            if (snapShot != null) {
                                                                                final AlertDialog
                                                                                        alertDialog =
                                                                                                new AlertDialog
                                                                                                                .Builder(
                                                                                                                getActivity())
                                                                                                        .create();
                                                                                final LayoutInflater
                                                                                        inflater =
                                                                                                getLayoutInflater();
                                                                                final View
                                                                                        convertView =
                                                                                                inflater
                                                                                                        .inflate(
                                                                                                                R.layout
                                                                                                                        .loading_view,
                                                                                                                null);
                                                                                alertDialog.setView(
                                                                                        convertView);
                                                                                final ProgressBar
                                                                                        progressbar1 =
                                                                                                (ProgressBar)
                                                                                                        convertView
                                                                                                                .findViewById(
                                                                                                                        R.id.progressbar1);
                                                                                ProgressBar
                                                                                        progressBar =
                                                                                                (ProgressBar)
                                                                                                        convertView
                                                                                                                .findViewById(
                                                                                                                        R.id.progressbar1);
                                                                                Sprite
                                                                                        doubleBounce =
                                                                                                new WanderingCubes();
                                                                                progressBar
                                                                                        .setIndeterminateDrawable(
                                                                                                doubleBounce);

                                                                                if (Build.VERSION
                                                                                                .SDK_INT
                                                                                        >= Build
                                                                                                .VERSION_CODES
                                                                                                .LOLLIPOP) {

                                                                                    progressbar1
                                                                                            .getIndeterminateDrawable()
                                                                                            .setColorFilter(
                                                                                                    0xFFFFFFFF,
                                                                                                    android
                                                                                                            .graphics
                                                                                                            .PorterDuff
                                                                                                            .Mode
                                                                                                            .SRC_IN);
                                                                                }
                                                                                alertDialog
                                                                                        .setCancelable(
                                                                                                false);
                                                                                alertDialog
                                                                                        .getWindow()
                                                                                        .setBackgroundDrawableResource(
                                                                                                android.R
                                                                                                        .color
                                                                                                        .transparent);
                                                                                alertDialog.show();
                                                                                timer =
                                                                                        new TimerTask() {
                                                                                            @Override
                                                                                            public
                                                                                            void
                                                                                                    run() {
                                                                                                getActivity()
                                                                                                        .runOnUiThread(
                                                                                                                new Runnable() {
                                                                                                                    @Override
                                                                                                                    public
                                                                                                                    void
                                                                                                                            run() {
                                                                                                                        alertDialog
                                                                                                                                .dismiss();
                                                                                                                        Map<
                                                                                                                                        String,
                                                                                                                                        String>
                                                                                                                                headers =
                                                                                                                                        new HashMap<
                                                                                                                                                String,
                                                                                                                                                String>();
                                                                                                                        headers
                                                                                                                                .put(
                                                                                                                                        "creator_id",
                                                                                                                                        snapShot.child(
                                                                                                                                                        "creator_id")
                                                                                                                                                .getValue(
                                                                                                                                                        String
                                                                                                                                                                .class));
                                                                                                                        headers
                                                                                                                                .put(
                                                                                                                                        "room_name",
                                                                                                                                        snapShot.child(
                                                                                                                                                        "room_name")
                                                                                                                                                .getValue(
                                                                                                                                                        String
                                                                                                                                                                .class));
                                                                                                                        headers
                                                                                                                                .put(
                                                                                                                                        "room_id",
                                                                                                                                        snapShot.child(
                                                                                                                                                        "room_id")
                                                                                                                                                .getValue(
                                                                                                                                                        String
                                                                                                                                                                .class));
                                                                                                                        Call
                                                                                                                                call =
                                                                                                                                        getSinchServiceInterface()
                                                                                                                                                .callConference(
                                                                                                                                                        snapShot.child(
                                                                                                                                                                        "room_id")
                                                                                                                                                                .getValue(
                                                                                                                                                                        String
                                                                                                                                                                                .class),
                                                                                                                                                        headers);
                                                                                                                        String
                                                                                                                                callId =
                                                                                                                                        call
                                                                                                                                                .getCallId();

                                                                                                                        Intent
                                                                                                                                callScreen =
                                                                                                                                        new Intent(
                                                                                                                                                getActivity(),
                                                                                                                                                RoomActivity
                                                                                                                                                        .class);
                                                                                                                        callScreen
                                                                                                                                .putExtra(
                                                                                                                                        SinchService
                                                                                                                                                .CALL_ID,
                                                                                                                                        callId);
                                                                                                                        callScreen
                                                                                                                                .putExtra(
                                                                                                                                        SinchService
                                                                                                                                                .EXTRA_ID,
                                                                                                                                        snapShot.child(
                                                                                                                                                        "room_id")
                                                                                                                                                .getValue(
                                                                                                                                                        String
                                                                                                                                                                .class));
                                                                                                                        startActivity(
                                                                                                                                callScreen);
                                                                                                                        RoomParticipant
                                                                                                                                joinRoom =
                                                                                                                                        new RoomParticipant(
                                                                                                                                                UserConfig
                                                                                                                                                        .getInstance()
                                                                                                                                                        .getUid(),
                                                                                                                                                snapShot.child(
                                                                                                                                                                "creator_id")
                                                                                                                                                        .getValue(
                                                                                                                                                                String
                                                                                                                                                                        .class));
                                                                                                                        participants
                                                                                                                                .child(
                                                                                                                                        snapShot.child(
                                                                                                                                                        "creator_id")
                                                                                                                                                .getValue(
                                                                                                                                                        String
                                                                                                                                                                .class))
                                                                                                                                .child(
                                                                                                                                        UserConfig
                                                                                                                                                .getInstance()
                                                                                                                                                .getUid())
                                                                                                                                .setValue(
                                                                                                                                        joinRoom);
                                                                                                                    }
                                                                                                                });
                                                                                            }
                                                                                        };
                                                                                _timer.schedule(
                                                                                        timer,
                                                                                        (int)
                                                                                                (2000));
                                                                            }
                                                                        }
                                                                    } else {
                                                                        showDialog(
                                                                                "Error",
                                                                                "Room not yet"
                                                                                    + " started or"
                                                                                    + " the room"
                                                                                    + " has been"
                                                                                    + " finished."
                                                                                    + " Please try"
                                                                                    + " again.");
                                                                    }

                                                                } else {
                                                                    showDialog(
                                                                            "Error",
                                                                            "Room not found. Please"
                                                                                + " ensure the room"
                                                                                + " id is correct,"
                                                                                + " no spaces"
                                                                                + " between text"
                                                                                + " and try"
                                                                                + " again.");
                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(
                                                                    DatabaseError arg0) {}
                                                        });
                                    }
                                }
                            });
                    dialog2.setNegativeButton(
                            "Cancel",
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface _dialog, int _which) {}
                            });
                    dialog2.create().show();
                    sheet.dismiss();
                });

        create_room.setOnClickListener(
                (View v) -> {
                    AlertDialog.Builder dialog =
                            new AlertDialog.Builder(getActivity(), R.style.AlertDialogTheme);
                    View dialogView =
                            getActivity()
                                    .getLayoutInflater()
                                    .inflate(R.layout.dialog_edittext, null);
                    final EditText edittext1 = (EditText) dialogView.findViewById(R.id.edittext1);
                    final ImageView imageview1 =
                            (ImageView) dialogView.findViewById(R.id.imageview1);

                    edittext1.setBackground(
                            new GradientDrawable() {
                                public GradientDrawable getIns(int a, int b) {
                                    this.setCornerRadius(a);
                                    this.setColor(b);
                                    return this;
                                }
                            }.getIns(10, 0xFFEEEEEE));

                    imageview1.setBackground(
                            new GradientDrawable() {
                                public GradientDrawable getIns(int a, int b) {
                                    this.setCornerRadius(a);
                                    this.setColor(b);
                                    return this;
                                }
                            }.getIns(360, 0xFFEEEEEE));

                    dialog.setTitle("Create Room");
                    dialog.setView(dialogView);
                    dialog.setPositiveButton(
                            "Start Room",
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface _dialog, int _which) {
                                    if (!edittext1.getText().toString().isEmpty()) {
                                        String key = UserConfig.getInstance().getUid();
                                        RoomBuilder room =
                                                new RoomBuilder(
                                                        UserConfig.getInstance().getUid(),
                                                        edittext1.getText().toString(),
                                                        key);
                                        rooms.child(key)
                                                .setValue(room)
                                                .addOnSuccessListener(
                                                        new OnSuccessListener() {

                                                            @Override
                                                            public void onSuccess(Object arg0) {
                                                                Map<String, String> headers =
                                                                        new HashMap<
                                                                                String, String>();
                                                                headers.put(
                                                                        "creator_id",
                                                                        UserConfig.getInstance()
                                                                                .getUid());
                                                                headers.put(
                                                                        "room_name",
                                                                        edittext1
                                                                                .getText()
                                                                                .toString());
                                                                headers.put("room_id", key);
                                                                Call call =
                                                                        getSinchServiceInterface()
                                                                                .callConference(
                                                                                        key,
                                                                                        headers);
                                                                String callId = call.getCallId();

                                                                Intent callScreen =
                                                                        new Intent(
                                                                                getActivity(),
                                                                                RoomActivity.class);
                                                                callScreen.putExtra(
                                                                        SinchService.CALL_ID,
                                                                        callId);
                                                                callScreen.putExtra(
                                                                        SinchService.EXTRA_ID, key);
                                                                startActivity(callScreen);
                                                                RoomParticipant joinRoom =
                                                                        new RoomParticipant(
                                                                                UserConfig
                                                                                        .getInstance()
                                                                                        .getUid(),
                                                                                UserConfig
                                                                                        .getInstance()
                                                                                        .getUid());
                                                                participants
                                                                        .child(
                                                                                UserConfig
                                                                                        .getInstance()
                                                                                        .getUid())
                                                                        .child(
                                                                                UserConfig
                                                                                        .getInstance()
                                                                                        .getUid())
                                                                        .setValue(joinRoom);
                                                            }
                                                        });
                                    }
                                }
                            });
                    dialog.setNegativeButton(
                            "Cancel",
                            new android.content.DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(
                                        android.content.DialogInterface _dialog, int _which) {}
                            });
                    dialog.create().show();
                    sheet.dismiss();
                });

        sheet.show();
    }

    public void showDialog(String title, String message) {
        MaterialDialog mDialog =
                new MaterialDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton(
                                "Okay",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        // Delete Operation
                                        dialogInterface.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                "Report",
                                new MaterialDialog.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface, int which) {
                                        dialogInterface.dismiss();
                                    }
                                })
                        .build();

        // Show Dialog
        mDialog.show();
    }

    public void openMenu(String id, int position) {
        BottomSheetDialog bs = new BottomSheetDialog(getActivity());
        View sheetView =
                LayoutInflater.from(getActivity()).inflate(R.layout.convo_bottom_sheet_menu, null);
        bs.setContentView(sheetView);

        final LinearLayout line = (LinearLayout) sheetView.findViewById(R.id.line);
        final LinearLayout delete_chat = (LinearLayout) sheetView.findViewById(R.id.delete_chat);
        final LinearLayout mute_chat = (LinearLayout) sheetView.findViewById(R.id.mute_chat);
        final LinearLayout mark_as_read = (LinearLayout) sheetView.findViewById(R.id.mark_as_read);
        final LinearLayout report_a_problem =
                (LinearLayout) sheetView.findViewById(R.id.report_a_problem);

        final ImageView delete = (ImageView) sheetView.findViewById(R.id.delete);
        final ImageView mute = (ImageView) sheetView.findViewById(R.id.mute);
        final ImageView read = (ImageView) sheetView.findViewById(R.id.read);
        final ImageView report = (ImageView) sheetView.findViewById(R.id.report);

        Utils.rippleRoundStroke(delete_chat, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(mute_chat, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(mark_as_read, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");
        Utils.rippleRoundStroke(report_a_problem, "#ffffff", "#e0e0e0", 0, 0, "#ffffff");

        delete.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        mute.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        report.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        read.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(360, 0xFFEEEEEE));

        line.setBackground(
                new GradientDrawable() {
                    public GradientDrawable getIns(int a, int b) {
                        this.setCornerRadius(a);
                        this.setColor(b);
                        return this;
                    }
                }.getIns(60, 0xFFE0E0E0));

        delete_chat.setOnClickListener(
                (View v) -> {
                    MaterialAlertDialogBuilder builder =
                            new MaterialAlertDialogBuilder(getActivity())
                                    .setTitle("Delete Conversation")
                                    .setMessage(
                                            "Do you really want to delete this copy of your"
                                                    + " conversation?")
                                    .setPositiveButton(
                                            "Delete",
                                            (dialog, which) -> {
                                                database.collection("conversation")
                                                        .document(UserConfig.getInstance().getUid())
                                                        .collection("inbox")
                                                        .document(id)
                                                        .delete()
                                                        .addOnCompleteListener(
                                                                (task) -> {
                                                                    ForaUtil.showMessage(
                                                                            getActivity(),
                                                                            "Conversation"
                                                                                + " deleted.");
                                                                    layoutAdapter.notifyItemRemoved(
                                                                            position);
                                                                    dialog.dismiss();
                                                                });
                                            })
                                    .setNegativeButton("Cancel", (dialog, whick) -> {});
                    builder.show();
                    bs.dismiss();
                });

        mute_chat.setOnClickListener((View v) -> {});

        mark_as_read.setOnClickListener((View v) -> {});

        report_a_problem.setOnClickListener((View v) -> {});

        bs.show();
    }

    public void checkTheme() {
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            darkMode(true);
        } else {
            darkMode(false);
        }
    }

    public void darkMode(boolean isNight) {
        if (isNight) {
            Utils.rippleRoundStroke(search_layout, "#424242", "#E0E0E0", 60, 0, "#E0E0E0");
            Utils.rippleRoundStroke(add_story_pic, "#424242", "#E0E0E0", 360, 0, "#E0E0E0");
            search_icon.setColorFilter(0xFFFFFFFF);
            textview1.setTextColor(0xFFF43159);
            add_story_text.setTextColor(0xFFFFFFFF);
            search_text.setTextColor(0xFFFFFFFF);
            add_story_pic.setColorFilter(0xFFFFFFFF);
            coordinator.setBackgroundColor(0xFF212121);
        } else {
            Utils.rippleRoundStroke(search_layout, "#EEEEEE", "#E0E0E0", 60, 0, "#E0E0E0");
            Utils.rippleRoundStroke(add_story_pic, "#EEEEEE", "#E0E0E0", 360, 0, "#E0E0E0");
            search_icon.setColorFilter(0xFF9E9E9E);
            textview1.setTextColor(0xFFF43159);
            add_story_text.setTextColor(0xFF000000);
            search_text.setTextColor(0xFF9E9E9E);
            add_story_pic.setColorFilter(0xFF000000);
            coordinator.setBackgroundColor(0xFFFFFFFF);
        }
    }

    private void getInboxList(int limit) {
        database.collection("conversations")
                .document(UserConfig.getInstance().getUid())
                .collection("inbox")
                .orderBy(Constants.KEY_TIMESTAMP)
                .addSnapshotListener(eventListener);
    }

    private EventListener<QuerySnapshot> eventListener =
            (value, error) -> {
                if (error != null) return;

                if (value != null) {
                    for (DocumentChange docs : value.getDocumentChanges()) {
                        if (docs.getType() == DocumentChange.Type.ADDED) {
                            Conversation convo = new Conversation();
                            convo.chatId = docs.getDocument().getString("chatId");
                            convo.senderId = docs.getDocument().getString("senderId");
                            convo.chatTime = docs.getDocument().getLong("chatTime");
                            convoList.add(convo);
                        }
                    }
                }

                Collections.reverse(convoList);
                rv_chat_list.smoothScrollToPosition(0);
                layoutAdapter.setItems(convoList);
                if (convoList.size() == 0) {
                    layoutAdapter.notifyDataSetChanged();
                } else {
                    layoutAdapter.notifyItemRangeChanged(convoList.size(), convoList.size());
                }
                totalPages = convoList.size();
                loader.setVisibility(View.GONE);
                loading = false;
                holder.setVisibility(View.GONE);
                story_list.setVisibility(View.VISIBLE);
                rv_chat_list.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper())
                        .postDelayed(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        notify.cancel(getContext(), 10);
                                    }
                                },
                                3000);
            };
}
