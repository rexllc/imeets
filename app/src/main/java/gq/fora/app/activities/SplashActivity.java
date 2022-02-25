package gq.fora.app.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
import com.google.firebase.firestore.FirebaseFirestore;

import gq.fora.app.R;
import gq.fora.app.activities.surface.BaseFragment;
import gq.fora.app.fragments.MaintenanceModeFragment;

public class SplashActivity extends BaseFragment {

    private FirebaseFirestore database = FirebaseFirestore.getInstance();
    private SharedPreferences sharedPreferences;
    private ProgressBar loader;

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
        View _view = _inflater.inflate(R.layout.activity_splash, _container, false);
        initializeBundle(_savedInstanceState, _view);
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle _savedInstanceState, View _view) {
        loader = _view.findViewById(R.id.loader);
        sharedPreferences = getActivity().getSharedPreferences("themes", Context.MODE_PRIVATE);
        if (sharedPreferences.getString("dark_mode", "").equals("true")) {
            _view.setBackgroundColor(0xFF212121);
        } else {
            _view.setBackgroundColor(0xFFFFFFFF);
        }

        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getActivity().getIntent())
                .addOnSuccessListener(
                        getActivity(),
                        new OnSuccessListener<PendingDynamicLinkData>() {
                            @Override
                            public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                                // Get deep link from result (may be null if no link is found)
                                Uri deepLink = null;
                                if (pendingDynamicLinkData != null) {
                                    deepLink = pendingDynamicLinkData.getLink();
                                } else {
                                    initializeLogic();
                                }

                                // Handle the deep link. For example, open the linked
                                // content, or apply promotional credit to the user's
                                // account.
                                // ...

                                // ...
                            }
                        })
                .addOnFailureListener(
                        getActivity(),
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("SplashActivity", "getDynamicLink:onFailure", e);
                                initializeLogic();
                            }
                        });

        loader.getIndeterminateDrawable().setTint(0xFFF43159);
    }

    public void initializeLogic() {
        if (!getChildFragmentManager().isDestroyed()) {
            new Handler(Looper.getMainLooper())
                    .postDelayed(
                            () -> {
                                if (isSystemMaintenance()) {
                                    getActivity()
                                            .getSupportFragmentManager()
                                            .beginTransaction()
                                            .add(
                                                    android.R.id.content,
                                                    new MaintenanceModeFragment())
                                            .setTransition(
                                                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();
                                } else {
                                    getActivity()
                                            .getSupportFragmentManager()
                                            .beginTransaction()
                                            .add(android.R.id.content, new ChatListActivity())
                                            .setTransition(
                                                    FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                            .commit();
                                }
                            },
                            2000);
        }
    }

    private boolean isSystemMaintenance() {
        database.collection("system")
                .document("maintenance")
                .get()
                .addOnCompleteListener(
                        (snapshot) -> {
                            if (snapshot != null) {
                                if (snapshot.getResult().contains("isMaintenance")) {
                                    if (snapshot.getResult().getBoolean("isMaintenance") == true) {
                                        return;
                                    }
                                }
                            }
                        });
        return false;
    }
}
