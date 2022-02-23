package gq.fora.app.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import gq.fora.app.R;

public class AgreementsActivity extends Fragment {

    private WebView wv;
    private MaterialButton previous_button, next_button;

    OnBackPressedCallback callback =
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new SplashActivity())
                            .addToBackStack(null)
                            .commit();
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_agreements, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle savedInstanceState, View view) {
        wv = (WebView) view.findViewById(R.id.wv);
        previous_button = (MaterialButton) view.findViewById(R.id.previous_button);
        next_button = (MaterialButton) view.findViewById(R.id.next_button);

        next_button.setOnClickListener(
                (View v) -> {
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .add(android.R.id.content, new SplashActivity())
                            .addToBackStack(null)
                            .commit();
                });

        previous_button.setOnClickListener(
                (View v) -> {
                    MaterialAlertDialogBuilder dialog =
                            new MaterialAlertDialogBuilder(getActivity());
                    dialog.setTitle("Exit");
                    dialog.setMessage("Do you want to continue or exit the app?");
                    dialog.setPositiveButton(
                            "Continue",
                            (d, r) -> {
                                getActivity()
                                        .getSupportFragmentManager()
                                        .beginTransaction()
                                        .add(android.R.id.content, new SplashActivity())
                                        .addToBackStack(null)
                                        .commit();
                            });

                    dialog.setNegativeButton(
                            "Exit",
                            (y, w) -> {
                                getActivity().getSupportFragmentManager().popBackStack();
                            });
                    dialog.show();
                });
    }

    public void initializeLogic() {
        wv.loadUrl("file:///android_asset/policies/terms.html");
    }
}
