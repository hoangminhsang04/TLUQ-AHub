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
        findViewById(R.id.btnCreate).setOnClickListener(v -> showCreatePostPopup());
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
    private void showCreatePostPopup() {
        createPostDialog = new Dialog(this);
        createPostDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        createPostDialog.setContentView(R.layout.dialog_create_post);
        createPostDialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        createPostDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        etPopupTitle = createPostDialog.findViewById(R.id.etTitle);
        etPopupContent = createPostDialog.findViewById(R.id.etContent);
        etPopupTags = createPostDialog.findViewById(R.id.etTags);
        rvTagSuggestionsPopup = createPostDialog.findViewById(R.id.rvTagSuggestionsPopup);
        tvPost = createPostDialog.findViewById(R.id.tvPost);
        ivClosePopup = createPostDialog.findViewById(R.id.ivClose);
        ivPopupAvatar = createPostDialog.findViewById(R.id.ivAvatar);
        tvUserNamePopup = createPostDialog.findViewById(R.id.tvUserName);
        tvReputationPopup = createPostDialog.findViewById(R.id.tvreputationScore1);

        // Hiển thị avatar và thông tin người dùng
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        String avatarName = prefs.getString("avatarName", null);
        String userName = prefs.getString("fullName", "Người dùng");
        int reputation = prefs.getInt("reputationScore", 0);

        tvUserNamePopup.setText(userName);
        tvReputationPopup.setText(reputation + " điểm");

        if (avatarName != null && avatarName.startsWith("http")) {
            Glide.with(this)
                    .load(avatarName)
                    .placeholder(R.drawable.anhdaidien)
                    .error(R.drawable.anhdaidien)
                    .circleCrop()
                    .into(ivPopupAvatar);
        } else {
            int resId = getResources().getIdentifier(avatarName, "drawable", getPackageName());
            ivPopupAvatar.setImageResource(resId != 0 ? resId : R.drawable.anhdaidien);
        }

        // Đóng popup
        ivClosePopup.setOnClickListener(v -> createPostDialog.dismiss());

        // Gợi ý tag khi nhập #
        TagSuggestionAdapter popupTagAdapter = new TagSuggestionAdapter(this, new ArrayList<>());
        rvTagSuggestionsPopup.setLayoutManager(new LinearLayoutManager(this));
        rvTagSuggestionsPopup.setAdapter(popupTagAdapter);

        etPopupTags.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                int hashIndex = text.lastIndexOf('#');

                if (hashIndex != -1) {
                    if (hashIndex == text.length() - 1) {
                        showTagSuggestionsPopup(allTags, popupTagAdapter);
                        return;
                    }
                    String keyword = text.substring(hashIndex + 1).trim().toLowerCase();
                    if (!keyword.isEmpty()) {
                        List<Tag> filtered = new ArrayList<>();
                        for (Tag tag : allTags) {
                            if (tag.getTagName().toLowerCase().contains(keyword)) {
                                filtered.add(tag);
                            }
                        }
                        showTagSuggestionsPopup(filtered, popupTagAdapter);
                    }
                } else {
                    rvTagSuggestionsPopup.setVisibility(View.GONE);
                }
            }

        });

        popupTagAdapter.setOnItemClickListener(tag -> {
            String currentText = etPopupTags.getText().toString();
            int hashIndex = currentText.lastIndexOf('#');
            if (hashIndex != -1) {
                String before = currentText.substring(0, hashIndex);
                String newText = before + "#" + tag.getTagName() + " ";
                etPopupTags.setText(newText);
                etPopupTags.setSelection(newText.length());
            }
            rvTagSuggestionsPopup.setVisibility(View.GONE);
        });

        // Kích hoạt nút đăng nếu đủ thông tin
        TextWatcher enablePostWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                boolean hasTitle = !etPopupTitle.getText().toString().trim().isEmpty();
                boolean hasContent = !etPopupContent.getText().toString().trim().isEmpty();
                tvPost.setEnabled(hasTitle && hasContent);
                tvPost.setTextColor((hasTitle && hasContent) ? getColor(R.color.black) : 0xFF9E9E9E);
            }
        };
        etPopupTitle.addTextChangedListener(enablePostWatcher);
        etPopupContent.addTextChangedListener(enablePostWatcher);

        // Đăng bài
        tvPost.setOnClickListener(v -> {
            String title = etPopupTitle.getText().toString().trim();
            String content = etPopupContent.getText().toString().trim();
            String tagText = etPopupTags.getText().toString().trim();

            if (title.isEmpty() || content.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề và nội dung.", Toast.LENGTH_SHORT).show();
                return;
            }

            DatabaseReference postRef = FirebaseDatabase.getInstance().getReference("questions").push();
            String postId = postRef.getKey();
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            String createdAt = isoFormat.format(new Date());
            String userId = prefs.getString("userId", null);

            Question newPost = new Question(postId, title, content, userId, createdAt, 0, 0, null, "Chờ duyệt");

            postRef.setValue(newPost).addOnSuccessListener(aVoid -> {
                // Tách tagText thành danh sách tag
                Set<String> tagSet = new HashSet<>();
                for (String raw : tagText.split("#")) {
                    String tagName = raw.trim().toLowerCase();
                    if (!tagName.isEmpty()) {
                        tagSet.add(tagName);
                    }
                }

                for (String tagName : tagSet) {
                    for (Tag tag : allTags) {
                        if (tag.getTagName().toLowerCase().equals(tagName)) {
                            String tagId = tag.getTagId();
                            String qtKey = postId + "_" + tagId;
                            DatabaseReference qtRef = FirebaseDatabase.getInstance().getReference("questionTags").child(qtKey);
                            Map<String, Object> qtMap = new HashMap<>();
                            qtMap.put("questionId", postId);
                            qtMap.put("tagId", tagId);
                            qtRef.setValue(qtMap);
                            break;
                        }
                    }
                }

                Toast.makeText(this, "Bài viết đã được tạo và chờ duyệt.", Toast.LENGTH_SHORT).show();
                createPostDialog.dismiss();
            });
        });



        createPostDialog.show();
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
