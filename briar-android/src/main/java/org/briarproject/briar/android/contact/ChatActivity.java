package org.briarproject.briar.android.contact;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.support.v7.widget.Toolbar;
import android.support.v4.widget.SwipeRefreshLayout;


import com.firebase.client.Firebase;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.Query;

import org.briarproject.briar.R;
import org.briarproject.briar.android.activity.ActivityComponent;
import org.briarproject.briar.android.activity.BriarActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ChatActivity extends BriarActivity {
	private LinearLayout layout;
	private ImageView sendButton;
	private EditText messageArea;
	private ScrollView scrollView;
	private Firebase reference;
	private DatabaseReference mRootRef;
	private RecyclerView mMessagesList;
	private final List<Message> messageList = new ArrayList<>();
	private LinearLayoutManager mLinearLayout;
	private MessageAdapter mAdapter;
	private Toolbar toolbar;
	private TextView toolbarContactName;
	private TextView toolbarTitle;
	private SwipeRefreshLayout mRefreshLayout;
	private static final int  TOTAL_ITEMS_TO_LOAD = 10;
	private int mCurrentPage = 1;
	private int itemPos = 0;
	private String mLastKey = "";
	private String mPrevKey = "";

	@Override
	public void injectActivity(ActivityComponent component) {
		component.inject(this);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);

		FirebaseApp.initializeApp(this);
		Firebase.setAndroidContext(this);

		layout = (LinearLayout) findViewById(R.id.layout1);
		sendButton = (ImageView) findViewById(R.id.sendButton);
		messageArea = (EditText) findViewById(R.id.messageArea);
		//scrollView = (ScrollView) findViewById(R.id.scrollView);

		sendButton.setEnabled(false);

		mRootRef = FirebaseDatabase.getInstance().getReference();

		mAdapter = new MessageAdapter(messageList);

		mMessagesList = (RecyclerView) findViewById(R.id.messages_list);
		mLinearLayout = new LinearLayoutManager(this);
		mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.message_swipe_layout);

		mMessagesList.setHasFixedSize(true);
		mMessagesList.setLayoutManager(mLinearLayout);
		mMessagesList.setAdapter(mAdapter);

		mMessagesList.scrollToPosition(messageList.size() - 1);

		// Custom Toolbar
		toolbar = setUpCustomToolbar(true);
		if (toolbar != null) {
			//toolbarAvatar = toolbar.findViewById(R.id.contactAvatar);
			//toolbarStatus = toolbar.findViewById(R.id.contactStatus);
			toolbarTitle = toolbar.findViewById(R.id.contactName);
		}
		toolbarTitle.setText(UserDetails.chatWith);

		loadMessages();

		TextWatcher tw = new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				enableOrDisableSendButton();
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		};

		messageArea.addTextChangedListener(tw);

		sendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendMessage();
			}
		});

		mRefreshLayout.setOnRefreshListener(
				new SwipeRefreshLayout.OnRefreshListener() {
					@Override
					public void onRefresh() {
						mCurrentPage++;
						itemPos = 0;
						loadMoreMessages();
					}
				});
	}

	private void enableOrDisableSendButton() {
		if (messageArea != null) {
			sendButton.setEnabled(true);
		}
	}

	private void sendMessage() {
		String message = messageArea.getText().toString();

		if(!TextUtils.isEmpty(message)){

			String current_user_ref = "messages/" + UserDetails.username + "/" + UserDetails.chatWith;
			String chat_user_ref = "messages/" + UserDetails.chatWith + "/" + UserDetails.username;

			DatabaseReference user_message_push = mRootRef.child("messages")
					.child(UserDetails.username).child(UserDetails.chatWith).push();

			String push_id = user_message_push.getKey();

			Map messageMap = new HashMap();
			messageMap.put("message", message);
			messageMap.put("seen", false);
			messageMap.put("type", "text");
			messageMap.put("time", ServerValue.TIMESTAMP);
			messageMap.put("from", UserDetails.username);

			Map messageUserMap = new HashMap();
			messageUserMap.put(current_user_ref + "/" + push_id, messageMap);
			messageUserMap.put(chat_user_ref + "/" + push_id, messageMap);

			messageArea.setText("");

			mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
				@Override
				public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
					if(databaseError != null){
						Log.d("CHAT_LOG", databaseError.getMessage().toString());
					}
				}
			});
		}
	}
	private void loadMoreMessages() {

		DatabaseReference messageRef = mRootRef.child("messages").child(UserDetails.username).child(UserDetails.chatWith);
		Query messageQuery = messageRef.orderByKey().endAt(mLastKey).limitToLast(10);
		messageQuery.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot dataSnapshot, String s) {
				Message message = dataSnapshot.getValue(Message.class);
				String messageKey = dataSnapshot.getKey();

				if(!mPrevKey.equals(messageKey)){
					messageList.add(itemPos++, message);
				} else {
					mPrevKey = mLastKey;
				}
				if(itemPos == 1) {
					mLastKey = messageKey;
				}

				Log.d("TOTALKEYS", "Last Key : " + mLastKey + " | Prev Key : " + mPrevKey + " | Message Key : " + messageKey);
				mAdapter.notifyDataSetChanged();
				mRefreshLayout.setRefreshing(false);
				mLinearLayout.scrollToPositionWithOffset(10, 0);
			}

			@Override
			public void onChildChanged(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onChildRemoved(DataSnapshot dataSnapshot) {

			}

			@Override
			public void onChildMoved(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});

	}


	private void loadMessages() {

		DatabaseReference messageRef = mRootRef.child("messages").child(UserDetails.username).child(UserDetails.chatWith);
		Query messageQuery = messageRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);
		messageQuery.addChildEventListener(new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot dataSnapshot, String s) {
				Message message = dataSnapshot.getValue(Message.class);
				itemPos++;
				if(itemPos == 1){

					String messageKey = dataSnapshot.getKey();

					mLastKey = messageKey;
					mPrevKey = messageKey;

				}
				messageList.add(message);
				mAdapter.notifyDataSetChanged();
				mMessagesList.scrollToPosition(messageList.size() - 1);
				mRefreshLayout.setRefreshing(false);
			}

			@Override
			public void onChildChanged(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onChildRemoved(DataSnapshot dataSnapshot) {

			}

			@Override
			public void onChildMoved(DataSnapshot dataSnapshot, String s) {

			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});
	}

	//For testing purposes
	public void addtToMessagesList(Message message) {
		messageList.add(message);
	}
}