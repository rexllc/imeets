package gq.fora.app.activities.surface;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import gq.fora.app.R;
import gq.fora.app.activities.ProfilePageActivity;
import gq.fora.app.adapter.UserListAdapter;
import gq.fora.app.listener.onclick.RecyclerItemClickListener;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.list.viewmodel.User;
import gq.fora.app.utils.ForaUtil;
import gq.fora.app.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SearchActivity extends Fragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private LinearLayout toolbar;
    private ImageView back, search_icon;
    private AppCompatEditText search;
    private ProgressBar loader;
    private RecyclerView rv_chat_list;
    private List<User> userList = new ArrayList<>();
    private DatabaseReference users = _firebase.getReference("users");

    private boolean loading;
    private int currentPage = 20;
    private int totalPages = 20;
    private LinearLayoutManager layoutManager;
    private Query query;
    private ChildEventListener users_child_listener;

    private double n1 = 0;
    private HashMap<String, Object> cacheMap = new HashMap<>();
    private Window window;
    private View view;
    private WindowInsetsControllerCompat insetsController;
    private WindowInsetsCompat insets;

    private ArrayList<HashMap<String, Object>> itemsList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> cache = new ArrayList<>();

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                        // keyboard is opened
                        insetsController.hide(WindowInsetsCompat.Type.ime());
                    } else {
                        // keyboard is closed
                    }
                }
            };

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_search, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {
        toolbar = view.findViewById(R.id.toolbar);
        back = view.findViewById(R.id.back);
        search = view.findViewById(R.id.search);
        search_icon = view.findViewById(R.id.search_icon);
        rv_chat_list = view.findViewById(R.id.rv_chat_list);
        loader = view.findViewById(R.id.loader);

        window = getActivity().getWindow();
        view = window.getDecorView();
        insetsController = new WindowInsetsControllerCompat(window, view);
        insets = ViewCompat.getRootWindowInsets(view);

        layoutManager = new LinearLayoutManager(getActivity());

        back.setOnClickListener(
                (View v) -> {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                        // keyboard is opened
                        insetsController.hide(WindowInsetsCompat.Type.ime());
                    } else {
                        // keyboard is closed
                    }
                });

        search.setHint("Search @");

        search_icon.setOnClickListener((View v) -> {});

        search.setOnEditorActionListener(
                new AppCompatEditText.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                            search_icon.performClick();
                            return true;
                        }
                        return false;
                    }
                });
        CollectionReference users = database.collection("users");
        users.addSnapshotListener(
                new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        for (DocumentChange snapshot : value.getDocumentChanges()) {
                            if (UserConfig.getInstance()
                                    .getUid()
                                    .equals(snapshot.getDocument().getId())) {
                                continue;
                            }
                            User user = new User();
                            user.displayName = snapshot.getDocument().getString("displayName");
                            user.userPhoto = snapshot.getDocument().getString("userPhoto");
                            user.username = snapshot.getDocument().getString("username");
                            user.userId = snapshot.getDocument().getString("userId");
                            userList.add(user);
                        }

                        Collections.shuffle(userList);
                        UserListAdapter layoutAdapter = new UserListAdapter(userList);
                        rv_chat_list.setAdapter(layoutAdapter);
                        rv_chat_list.setHasFixedSize(true);
                        rv_chat_list.setLayoutManager(layoutManager);
                        if (userList.size() == 0) {
                            layoutAdapter.notifyDataSetChanged();
                        } else {
                            layoutAdapter.notifyItemRangeChanged(userList.size(), userList.size());
                        }
                        totalPages = userList.size();
                        loader.setVisibility(View.GONE);
                        loading = false;
                    }
                });

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
                                searchQuery(currentPage + 3);
                                loading = true;
                                loader.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                });

        rv_chat_list.addOnItemTouchListener(
                new RecyclerItemClickListener(
                        getActivity(),
                        rv_chat_list,
                        new RecyclerItemClickListener.OnItemClickListener() {
                            @Override
                            public void onItemClick(View view, int position) {
                                Fragment profileFragment = new ProfilePageActivity();
                                Bundle bundle1 = new Bundle();
                                getActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                        .add(android.R.id.content, profileFragment)
                                        .addToBackStack(null)
                                        .commit();
                                bundle1.putString("userId", userList.get(position).userId);
                                profileFragment.setArguments(bundle1);
                                // do whatever
                            }

                            @Override
                            public void onLongItemClick(View view, int position) {
                                // do whatever
                            }
                        }));
    }

    public void initializeLogic() {
        toolbar.setElevation((float) 3);
        Utils.rippleEffects(back, "#e0e0e0");
        ForaUtil.showKeyboard(getActivity());
        search.requestFocus();
        searchQuery(20);
    }

    public void searchQuery(int items) {
        loading = true;
    }

    public void searchInListmap(
            final String _value,
            final ArrayList<HashMap<String, Object>> _listmap,
            final String _key) {
        _listmap.clear();
        n1 = 0;
    }
}
