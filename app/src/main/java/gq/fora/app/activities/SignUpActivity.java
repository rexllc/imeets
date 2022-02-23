package gq.fora.app.activities;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import gq.fora.app.R;

public class SignUpActivity extends Fragment {

    private LinearLayout linear1;
    private EditText edittext1, edittext2;
    private MaterialButton next_button;
    private MaterialButton previous_button;
    private TextView title;
    private Vibrator vb;

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
            @NonNull LayoutInflater _inflater,
            @Nullable ViewGroup _container,
            @Nullable Bundle _savedInstanceState) {
        View _view = _inflater.inflate(R.layout.activity_signup, _container, false);
        initializeBundle(_savedInstanceState, _view);
        initializeLogic();
        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(getViewLifecycleOwner(), callback);
        return _view;
    }

    public void initializeBundle(Bundle _savedInstanceState, View _view) {
        linear1 = _view.findViewById(R.id.linear1);
        edittext1 = _view.findViewById(R.id.edittext1);
        edittext2 = _view.findViewById(R.id.edittext2);
        next_button = _view.findViewById(R.id.next_button);
        previous_button = _view.findViewById(R.id.previous_button);
        title = _view.findViewById(R.id.title);

        vb = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);

        previous_button.setOnClickListener(
                (View v) -> {
                    try {
                        InputMethodManager imm =
                                (InputMethodManager)
                                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (imm.isAcceptingText()) {
                            ((InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(edittext1.getWindowToken(), 0);
                            ((InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(edittext2.getWindowToken(), 0);
                        }
                    } catch (Exception e) {

                    }
                    getActivity().getSupportFragmentManager().popBackStack();
                });

        next_button.setOnClickListener(
                (View v) -> {
                    try {
                        InputMethodManager imm =
                                (InputMethodManager)
                                        getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

                        if (imm.isAcceptingText()) {
                            ((InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(edittext1.getWindowToken(), 0);
                            ((InputMethodManager)
                                            getContext()
                                                    .getSystemService(Context.INPUT_METHOD_SERVICE))
                                    .hideSoftInputFromWindow(edittext2.getWindowToken(), 0);
                        }
                    } catch (Exception e) {

                    }

                    if (edittext1.getText().toString().isEmpty()
                            || edittext2.getText().toString().isEmpty()) {
                        Snackbar.make(
                                        next_button,
                                        "All fields are required.",
                                        Snackbar.LENGTH_SHORT)
                                .show();
                        vb.vibrate((100));
                    } else {
                        Fragment email = new EmailSignUpActivity();
                        getActivity()
                                .getSupportFragmentManager()
                                .beginTransaction()
                                .add(android.R.id.content, email)
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                .addToBackStack(null)
                                .commit();
                        Bundle bundle = new Bundle();
                        bundle.putString("first_name", edittext1.getText().toString());
                        bundle.putString("last_name", edittext2.getText().toString());
                        email.setArguments(bundle);
                    }
                });
    }

    public void initializeLogic() {
        edittext2.setOnEditorActionListener(
                new EditText.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                            next_button.performClick();
                            return true;
                        }
                        return false;
                    }
                });
    }
}
