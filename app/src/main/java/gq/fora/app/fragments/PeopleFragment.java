package gq.fora.app.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import gq.fora.app.R;
import gq.fora.app.adapter.CallsAdapter;
import gq.fora.app.adapter.PeopleAdapter;
import gq.fora.app.models.UserConfig;
import gq.fora.app.models.friends.Friends;
import gq.fora.app.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class PeopleFragment extends Fragment {

    private FirebaseDatabase firebase = FirebaseDatabase.getInstance();
    private DatabaseReference friends = firebase.getReference("friends");
    private DatabaseReference messages = firebase.getReference("messages");
    private TextView calls, people;
    private LinearLayout tabLayout;
    private RecyclerView rv_calls_list;
    private RecyclerView rv_people_list;
    private LinearLayoutManager linearLayoutManager;
    private LinearLayoutManager linearLayoutManager2;
    private PeopleAdapter peopleAdapter;
    private CallsAdapter callsAdapter;

    private ArrayList<Friends> friendList = new ArrayList<>();

    @NonNull
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup parent,
            @Nullable Bundle savedInstanceState) {
        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fragment_people, parent, false);
        initialize(savedInstanceState, view);
        return view;
    }

    public void initialize(Bundle savedInstanceState, View view) {
        tabLayout = view.findViewById(R.id.tab_host);
        rv_calls_list = view.findViewById(R.id.rv_calls_list);
        rv_people_list = view.findViewById(R.id.rv_people_list);
        calls = view.findViewById(R.id.calls);
        people = view.findViewById(R.id.people);
        Utils.rippleRoundStroke(calls, "#eeeeee", "#ffffff", 60, 0, "#eeeeee");
        Utils.rippleRoundStroke(people, "#ffffff", "#eeeeee", 60, 0, "#eeeeee");
        calls.setOnClickListener(onClick);
        people.setOnClickListener(onClick);
        tabLayout.setElevation(5);

        linearLayoutManager = new LinearLayoutManager(getActivity());
        peopleAdapter = new PeopleAdapter(getActivity());
        rv_people_list.setAdapter(peopleAdapter);
        rv_people_list.setLayoutManager(linearLayoutManager);

        linearLayoutManager2 = new LinearLayoutManager(getActivity());
        callsAdapter = new CallsAdapter(getActivity());
        rv_calls_list.setAdapter(callsAdapter);
        rv_calls_list.setLayoutManager(linearLayoutManager2);

        friends.child(UserConfig.getInstance().getUid())
                .orderByChild("isFriends")
                .equalTo(true)
                .addValueEventListener(
                        new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {

                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    Friends friend = data.getValue(Friends.class);
                                    friendList.add(friend);
                                }
                                peopleAdapter.setItems(friendList);
                                peopleAdapter.notifyDataSetChanged();
                                people.setText("Friends (" + friendList.size() + ")");
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });

        messages.child(UserConfig.getInstance().getUid())
                .orderByKey()
                .addChildEventListener(
                        new ChildEventListener() {
                            @Override
                            public void onChildAdded(DataSnapshot dataSnapshot, String arg1) {
                                ArrayList<HashMap<String, Object>> chats = new ArrayList<>();
                                for (DataSnapshot data : dataSnapshot.getChildren()) {
                                    GenericTypeIndicator<HashMap<String, Object>> _ind =
                                            new GenericTypeIndicator<HashMap<String, Object>>() {};
                                    final String _childKey = data.getKey();
                                    final HashMap<String, Object> _childValue = data.getValue(_ind);
                                }
                            }

                            @Override
                            public void onChildChanged(DataSnapshot dataSnapshot, String arg1) {}

                            @Override
                            public void onChildRemoved(DataSnapshot arg0) {}

                            @Override
                            public void onChildMoved(DataSnapshot arg0, String arg1) {}

                            @Override
                            public void onCancelled(DatabaseError arg0) {}
                        });
    }

    private View.OnClickListener onClick =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    switch (view.getId()) {
                        case R.id.calls:
                            Utils.rippleRoundStroke(calls, "#eeeeee", "#ffffff", 60, 0, "#eeeeee");
                            Utils.rippleRoundStroke(people, "#ffffff", "#eeeeee", 60, 0, "#eeeeee");
                            rv_calls_list.setVisibility(View.VISIBLE);
                            rv_people_list.setVisibility(View.GONE);
							calls.setTextColor(0xFFF43159);
							people.setTextColor(0xFF9E9E9E);
                            break;
                        case R.id.people:
                            Utils.rippleRoundStroke(calls, "#ffffff", "#eeeeee", 60, 0, "#eeeeee");
                            Utils.rippleRoundStroke(people, "#eeeeee", "#ffffff", 60, 0, "#eeeeee");
                            rv_calls_list.setVisibility(View.GONE);
                            rv_people_list.setVisibility(View.VISIBLE);
							calls.setTextColor(0xFF9E9E9E);
							people.setTextColor(0xFFF43159);
                            break;
                    }
                }
            };
}
