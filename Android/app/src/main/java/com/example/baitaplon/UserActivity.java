package com.example.baitaplon;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.baitaplon.adapters.PostAdapter;
import com.example.baitaplon.adapters.TagSuggestionAdapter;
import com.example.baitaplon.models.Question;
import com.example.baitaplon.models.QuestionTag;
import com.example.baitaplon.models.Tag;
import com.google.firebase.database.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import android.app.Dialog;
import android.widget.TextView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Comparator;


import com.example.baitaplon.models.User;

public class UserActivity extends AppCompatActivity {
    RecyclerView rv, rvTagSuggestions;
    PostAdapter adapter;
    TagSuggestionAdapter tagSuggestionAdapter;
    List<Question> questions = new ArrayList<>();
    List<Question> allQuestions = new ArrayList<>();
    List<Tag> allTags = new ArrayList<>();
    DatabaseReference qRef, tagsRef, questionTagsRef;
    EditText etSearch;
    ImageView imgAvatar;

    Dialog createPostDialog;
    EditText etPopupTitle, etPopupContent, etPopupTags;
    TextView tvPost;
    RecyclerView rvTagSuggestionsPopup;
    ImageView ivClosePopup, ivPopupAvatar;
    TextView tvUserNamePopup, tvReputationPopup;

    private boolean questionsLoaded = false;
    private boolean tagsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user);

        etSearch = findViewById(R.id.etSearch);
        rv = findViewById(R.id.recyclerViewPosts);
        imgAvatar = findViewById(R.id.ivAvatar);

        adapter = new PostAdapter(this, questions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        rvTagSuggestions = findViewById(R.id.rvTagSuggestions);
        tagSuggestionAdapter = new TagSuggestionAdapter(this, new ArrayList<>());
        rvTagSuggestions.setLayoutManager(new LinearLayoutManager(this));
        rvTagSuggestions.setAdapter(tagSuggestionAdapter);

        tagSuggestionAdapter.setOnItemClickListener(tag -> {
            etSearch.setText("#" + tag.getTagName());
            etSearch.setSelection(etSearch.getText().length());
            rvTagSuggestions.setVisibility(View.GONE);
            filterQuestions("#" + tag.getTagName());
        });

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        qRef = db.getReference("questions");
        tagsRef = db.getReference("tags");
        questionTagsRef = db.getReference("questionTags");

        loadAllQuestions();
        loadAllTags();
        loadUserAvatar();

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (questionsLoaded && tagsLoaded) {
                    String text = s.toString();
                    if (text.equals("#")) {
                        showTagSuggestions(allTags);
                    } else if (text.startsWith("#")) {
                        String keyword = text.substring(1).toLowerCase(Locale.getDefault());
                        List<Tag> filtered = new ArrayList<>();
                        for (Tag tag : allTags) {
                            if (tag.getTagName().toLowerCase(Locale.getDefault()).contains(keyword)) {
                                filtered.add(tag);
                            }
                        }
                        showTagSuggestions(filtered);
                    } else {
                        rvTagSuggestions.setVisibility(View.GONE);
                    }

                    filterQuestions(text);
                }
            }
        });

        findViewById(R.id.btnHome).setOnClickListener(v -> Log.d("UserActivity", "Home button clicked"));
        findViewById(R.id.btnProfile).setOnClickListener(v -> Log.d("UserActivity", "Profile button clicked"));
    }

    private void loadAllQuestions() {
        qRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                allQuestions.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    Question q = ds.getValue(Question.class);
                    if (q != null && "Đã duyệt".equalsIgnoreCase(q.getStatus())) {
                        allQuestions.add(q);
                    }
                }
                Collections.sort(allQuestions, new Comparator<Question>() {
                    @Override
                    public int compare(Question q1, Question q2) {
                        Date d1 = parseISODate(q1.getCreatedAt());
                        Date d2 = parseISODate(q2.getCreatedAt());
                        return d2.compareTo(d1); // mới nhất lên đầu
                    }
                });
                questionsLoaded = true;
                if (tagsLoaded) {
                    questions.clear();
                    questions.addAll(allQuestions);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override public void onCancelled(DatabaseError e) {
                Log.e("UserActivity", "Failed to load questions: " + e.getMessage());
            }
        });
    }

    private void loadAllTags() {
        tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                allTags.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    Tag tag = ds.getValue(Tag.class);
                    if (tag != null) {
                        allTags.add(tag);
                    }
                }
                tagsLoaded = true;
                if (questionsLoaded) {
                    questions.clear();
                    questions.addAll(allQuestions);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override public void onCancelled(DatabaseError e) {
                Log.e("UserActivity", "Failed to load tags: " + e.getMessage());
            }
        });
    }

    private void filterQuestions(String searchText) {
        final String lowerCaseSearchText = searchText.toLowerCase(Locale.getDefault());

        if (searchText.isEmpty()) {
            questions.clear();
            questions.addAll(allQuestions);
            adapter.notifyDataSetChanged();
            return;
        }

        if (lowerCaseSearchText.startsWith("#")) {
            final String tagSearchQuery = lowerCaseSearchText.substring(1).trim();

            if (tagSearchQuery.isEmpty()) {
                questions.clear();
                questions.addAll(allQuestions);
                adapter.notifyDataSetChanged();
                return;
            }

            List<String> matchingTagIds = new ArrayList<>();
            for (Tag tag : allTags) {
                if (tag.getTagName().toLowerCase(Locale.getDefault()).contains(tagSearchQuery)) {
                    matchingTagIds.add(tag.getTagId());
                }
            }

            if (matchingTagIds.isEmpty()) {
                questions.clear();
                adapter.notifyDataSetChanged();
                return;
            }

            questionTagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snap) {
                    HashSet<String> matchedQuestionIds = new HashSet<>();
                    for (DataSnapshot ds : snap.getChildren()) {
                        QuestionTag qt = ds.getValue(QuestionTag.class);
                        if (qt != null && matchingTagIds.contains(qt.getTagId())) {
                            matchedQuestionIds.add(qt.getQuestionId());
                        }
                    }

                    questions.clear();
                    for (Question q : allQuestions) {
                        if (matchedQuestionIds.contains(q.getQuestionId())) {
                            questions.add(q);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override public void onCancelled(DatabaseError e) {
                    Log.e("UserActivity", "Lỗi khi lọc questionTags: " + e.getMessage());
                }
            });

        } else {
            questions.clear();
            for (Question q : allQuestions) {
                if (q.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseSearchText)) {
                    questions.add(q);
                }
            }
            adapter.notifyDataSetChanged();
        }
    }

    private void showTagSuggestions(List<Tag> tagsToShow) {
        if (tagsToShow.isEmpty()) {
            rvTagSuggestions.setVisibility(View.GONE);
        } else {
            tagSuggestionAdapter.updateTags(tagsToShow);
            rvTagSuggestions.setVisibility(View.VISIBLE);
        }
    }

    private void loadUserAvatar() {
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String avatarName = prefs.getString("avatarName", null);

        if (avatarName != null && !avatarName.isEmpty()) {
            if (avatarName.startsWith("http")) {
                Glide.with(this)
                        .load(avatarName)
                        .placeholder(R.drawable.anhdaidien)
                        .error(R.drawable.anhdaidien)
                        .circleCrop()
                        .into(imgAvatar);
            } else {
                int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
                imgAvatar.setImageResource(resId != 0 ? resId : R.drawable.anhdaidien);
            }
        } else {
            imgAvatar.setImageResource(R.drawable.anhdaidien);
        }
    }

    private void showTagSuggestionsPopup(List<Tag> tags, TagSuggestionAdapter adapter) {
        if (tags.isEmpty()) {
            rvTagSuggestionsPopup.setVisibility(View.GONE);
        } else {
            adapter.updateTags(tags);
            rvTagSuggestionsPopup.setVisibility(View.VISIBLE);
        }
    }
    private Date parseISODate(String isoString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(isoString);
        } catch (ParseException e) {
            e.printStackTrace();
            return new Date(0); // fallback nếu lỗi
        }
    }

}
