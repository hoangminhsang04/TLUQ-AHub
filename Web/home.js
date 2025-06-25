import { get, set, ref, update, push, remove } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-database.js";
import { db } from './firebase-config.js';

let allQuestions = {};
let allUsers = {};
let allTags = {};
let allQuestionTags = {};
let deletingPostId = null;
let allAnswers = {};
let allVotes = {};
let currentEditingCommentId = null;
let deletingCommentId = null;

const postList = document.getElementById("post-list");

function getCurrentUser() {
    try {
        return JSON.parse(localStorage.getItem("currentUser"));
    } catch {
        return null;
    }
}

function getTimeAgo(createdAt) {
    const now = new Date();
    const created = new Date(createdAt);
    const diffDays = Math.floor((now - created) / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return "Hôm nay";
    if (diffDays === 1) return "1 ngày trước";
    return `${diffDays} ngày trước`;
}
function renderPost(q, author, answers, tags, questionTags, votes, currentUser) {
    const tagNames = Object.values(questionTags)
        .filter(qt => qt.questionId === q.questionId)
        .map(qt => tags[qt.tagId]?.tagName || "");

    const voteCount = Math.max(0, q.voteCount || 0);

    let currentUserVote = null;
    if (currentUser) {
        currentUserVote = Object.values(votes).find(v => v.userId === currentUser.userId && v.questionId === q.questionId);
    }

    const relatedAnswers = Object.values(answers).filter(ans => ans.questionId === q.questionId);

    const commentsHtml = relatedAnswers.map(ans => {
        const user = allUsers[ans.authorUserId];
        const answerVote = currentUser && Object.values(votes).find(v => v.userId === currentUser.userId && v.answerId === ans.answerId);
        const liked = answerVote?.voteType === 1 ? 'selected' : '';
        const disliked = answerVote?.voteType === -1 ? 'selected' : '';

        return `
            <div class="comment">
                <div class="post-avatar">
                    <img src="${user?.avatar || 'https://i.imgur.com/uIgDDDd.png'}" alt="avatar" />
                </div>
                <div class="comment-body">
                    <div class="comment-content">
                        <div class="comment-user">
                            <span class="user-name">${user?.fullName || 'Ẩn danh'}</span>
                            <span class="user-reputation"><iconify-icon icon="ph:medal"></iconify-icon> ${user?.reputationScore || 0} điểm</span>
                        </div>
                        <div class="comment-text post-content">${ans.content}</div>
                    </div>
                    <div class="comment-actions">
                        <div class="comment-time">${getTimeAgo(ans.createdAt)}</div>
                        
                        <div class="action like-action ${liked}" onclick="toggleVote(this, 'like', '${ans.answerId}', 'answer')">
                            <iconify-icon icon="iconamoon:like-light" class="like-icon"></iconify-icon>
                            <span class="like-text">${Math.max(0, ans.voteCount || 0)} thích</span>
                        </div>

                        <div class="action dislike-action ${disliked}" onclick="toggleVote(this, 'dislike', '${ans.answerId}', 'answer')">
                            <iconify-icon icon="iconamoon:like-light" class="dislike-icon" style="transform: rotate(180deg);"></iconify-icon>
                        </div>
                    </div>
                </div>
                <div class="comment-header-options">
                    <iconify-icon icon="ph:dots-three-vertical-bold" class="more-options-icon" onclick="toggleCommentOptions(this)" style="transform: rotate(90deg);"></iconify-icon>
                    <div class="more-options-dropdown hidden">
                        <div class="option"onclick=" openEditCommentModal('${ans.answerId}')">Sửa</div>
                        <div class="option" onclick="openDeleteCommentPopup('${ans.answerId}')">Xóa</div>
                    </div>
                </div>
            </div>`;
    }).join('');

    const liked = currentUserVote?.voteType === 1 ? 'selected' : '';
    const disliked = currentUserVote?.voteType === -1 ? 'selected' : '';

    return `
    <div class="post-card" style="grid-column: span 8;">
        <div class="post-header">
            <div class="post-avatar">
                <img src="${author?.avatar || 'https://i.imgur.com/uIgDDDd.png'}" alt="avatar">
            </div>
            <div class="post-user-info">
                <div class="post-name-rep">
                    <span class="user-name">${author?.fullName}</span>
                    <span class="user-reputation"><iconify-icon icon="ph:medal"></iconify-icon> ${author?.reputationScore || 0} điểm</span>
                </div>
                <div class="post-time">${getTimeAgo(q.createdAt)}</div>
            </div>
            <div class="post-header-options">
                <iconify-icon icon="ph:dots-three-vertical-bold" class="more-options-icon" onclick="togglePostOptions(this)" style="transform: rotate(90deg);"></iconify-icon>
                <div class="more-options-dropdown hidden">
                    <div class="option" onclick="openEditPostModal('${q.questionId}')">Sửa</div>
                    <div class="option" onclick="openDeletePopup('${q.questionId}')">Xóa</div>
                </div>
            </div>
        </div>

        <div class="post-body">
            <div class="post-title">${q.title}</div>
            <div class="post-content">${q.content}</div>
            <div class="post-tags">${tagNames.map(t => `<span class="post-tag">#${t}</span>`).join(' ')}</div>
        </div>

        <div class="post-actions">
            <div class="action like-action ${liked}" onclick="toggleVote(this, 'like', '${q.questionId}', 'question')">
                <iconify-icon icon="iconamoon:like-light" class="like-icon"></iconify-icon>
                <span class="like-text">${voteCount} thích</span>
            </div>
            <div class="action dislike-action ${disliked}" onclick="toggleVote(this, 'dislike', '${q.questionId}', 'question')">
                <iconify-icon icon="iconamoon:like-light" class="dislike-icon" style="transform: rotate(180deg);"></iconify-icon>
            </div>
            <div class="action comment-action comment-toggle-btn" onclick="toggleCommentSection(this)">
                <iconify-icon icon="iconamoon:comment" class="comment-icon"></iconify-icon>
                <span>${relatedAnswers.length} bình luận</span>
            </div>
        </div>

        <div class="comments-section hidden">
            ${commentsHtml || "<i>Chưa có bình luận.</i>"}
            <div class="comment-input-wrapper">
                <input type="text" class="comment-input" placeholder="Nhập bình luận..." />
                <button class="send-comment-btn" onclick="submitComment('${q.questionId}', this)">Gửi</button>
            </div>
        </div>
    </div>`;
}

async function loadPosts() {
    const snapshot = await get(ref(db));
    const data = snapshot.val();
    const questions = data.questions || {};
    const users = data.users || {};
    const answers = data.answers || {};
    const tags = data.tags || {};
    const questionTags = data.questionTags || {};
    const votes = data.votes || {};
    const currentUser = getCurrentUser();


    allQuestions = questions;
    allUsers = users;
    allTags = tags;
    allQuestionTags = questionTags;
    allAnswers = answers;
    allVotes = votes;


    postList.innerHTML = "";

    Object.values(questions).forEach(q => {
        if (q.status !== "Đã duyệt") return;

        const author = users[q.authorUserId];
        const html = renderPost(q, author, answers, tags, questionTags, votes, currentUser);
        postList.innerHTML += html;
    });
}

window.toggleVote = async function (element, type, itemId, itemType) {
    const user = getCurrentUser();
    if (!user) return alert("Bạn chưa đăng nhập");

    const voteType = type === "like" ? 1 : -1;
    const oppositeType = -voteType;
    const snapshot = await get(ref(db, 'votes'));
    const votes = snapshot.exists() ? snapshot.val() : {};

    let voteId = null;
    let existedVote = null;

    Object.entries(votes).forEach(([id, vote]) => {
        const isTarget = vote.userId === user.userId &&
            (itemType === 'question' ? vote.questionId === itemId : vote.answerId === itemId);
        if (isTarget) {
            voteId = id;
            existedVote = vote;
        }
    });

    const itemRef = ref(db, `${itemType}s/${itemId}`);
    const itemSnap = await get(itemRef);
    if (!itemSnap.exists()) return;
    let voteCount = itemSnap.val().voteCount || 0;

    let likeBtn, dislikeBtn, likeText;

    if (itemType === 'question') {
        const postCard = element.closest(".post-card");
        likeBtn = postCard.querySelector(`.like-action`);
        dislikeBtn = postCard.querySelector(`.dislike-action`);
        likeText = likeBtn.querySelector(".like-text");
    } else {
        const comment = element.closest(".comment");
        likeBtn = comment.querySelector(`.like-action`);
        dislikeBtn = comment.querySelector(`.dislike-action`);
        likeText = likeBtn.querySelector(".like-text");
    }


    if (!existedVote) {
        const newVoteRef = push(ref(db, 'votes'));
        await update(newVoteRef, {
            userId: user.userId,
            voteType,
            [`${itemType}Id`]: itemId
        });
        voteCount += voteType;
    } else if (existedVote.voteType === voteType) {
        await remove(ref(db, `votes/${voteId}`));
        voteCount -= voteType;
    } else {
        await update(ref(db, `votes/${voteId}`), { voteType });
        voteCount += voteType * 2;
    }

    await update(itemRef, { voteCount });

    // Cập nhật UI
    likeBtn.classList.toggle("selected", voteType === 1 && (!existedVote || existedVote.voteType !== 1));
    dislikeBtn.classList.toggle("selected", voteType === -1 && (!existedVote || existedVote.voteType !== -1));

    likeText.textContent = `${Math.max(0, voteCount)} thích`;
};

window.togglePostOptions = function (iconElement) {
    const dropdown = iconElement.nextElementSibling;
    dropdown.classList.toggle("hidden");
};

window.toggleCommentOptions = function (iconElement) {
    const dropdown = iconElement.nextElementSibling;
    dropdown.classList.toggle("hidden");
};

window.toggleCommentSection = function (btn) {
    const postCard = btn.closest(".post-card");
    const commentSection = postCard.querySelector(".comments-section");
    commentSection?.classList.toggle("hidden");
};

window.openEditPostModal = function (questionId) {
    console.log("📢 Đã nhấn sửa với ID:", questionId);

    const post = allQuestions[questionId];
    const author = allUsers[post.authorUserId];

    document.getElementById("editAvatar").src = author.avatar;
    document.getElementById("editUserName").textContent = author.fullName;
    document.getElementById("editUserScore").textContent = `${author.reputationScore || 0} điểm`;

    document.getElementById("editTitle").value = post.title;
    document.getElementById("editContent").value = post.content;

    const editSelectedTagsContainer = document.getElementById("editSelectedTags");
    editSelectedTagsContainer.innerHTML = '';
    const currentPostTagIds = Object.values(allQuestionTags)
        .filter(qt => qt.questionId === questionId)
        .map(qt => qt.tagId);

    currentPostTagIds.forEach(tagId => {
        const tagInfo = allTags[tagId];
        if (tagInfo) {
            const tagElement = document.createElement('span');
            tagElement.className = 'tag-item';
            tagElement.dataset.tagId = tagId;
            tagElement.innerHTML = `
                <span>#${tagInfo.tagName}</span>
                <button class="remove-tag-btn" onclick="removeTagFromEditModal('${questionId}', '${tagId}', this)">✕</button>
            `;
            editSelectedTagsContainer.appendChild(tagElement);
        }
    });

    window.currentEditingPostId = questionId;
    document.getElementById("editPostModalOverlay").classList.remove("hidden");
};

window.closeEditPostModal = function () {
    document.getElementById("editPostModalOverlay").classList.add("hidden");
};

window.submitEditPost = async function () {
    const title = document.getElementById("editTitle").value.trim();
    const content = document.getElementById("editContent").value.trim();
    const id = window.currentEditingPostId;

    if (!id || !title || !content) return alert("Thiếu thông tin!");
    await update(ref(db, `questions/${id}`), { title, content });
    alert("✅ Đã cập nhật bài viết!");
    location.reload();
};

window.openDeletePopup = function (questionId) {
    if (confirm("Bạn có chắc muốn xóa bài viết này?")) {
        remove(ref(db, `questions/${questionId}`));
        alert("✅ Đã xóa bài viết");
        location.reload();
    }
};

document.addEventListener("DOMContentLoaded", () => {
    const user = getCurrentUser();
    const avatarImg = document.getElementById("avatarImg");
    if (avatarImg && user) {
        avatarImg.src = user.avatar || "https://api.dicebear.com/7.x/thumbs/svg?seed=user";
    }
    loadPosts();
});
document.addEventListener("click", function (event) {
    const isOptionIcon = event.target.closest(".more-options-icon");
    const isDropdown = event.target.closest(".more-options-dropdown");

    if (!isOptionIcon && !isDropdown) {
        document.querySelectorAll(".more-options-dropdown").forEach(dropdown => {
            dropdown.classList.add("hidden");
        });
    }
});
window.openDeletePopup = function (questionId) {
    deletingPostId = questionId;
    document.getElementById("deleteModal").classList.remove("hidden");
};

window.closeDeleteModal = function () {
    deletingPostId = null;
    document.getElementById("deleteModal").classList.add("hidden");
};

window.confirmDelete = async function () {
    if (!deletingPostId) return;

    await remove(ref(db, `questions/${deletingPostId}`));
    showToast("✅ Đã xóa bài viết thành công!");
    closeDeleteModal();
    setTimeout(() => location.reload(), 1500);
};

window.showToast = function (message) {
    const toast = document.createElement("div");
    toast.className = "toast-message";
    toast.textContent = message;
    document.body.appendChild(toast);
    setTimeout(() => toast.classList.add("show"), 10);
    setTimeout(() => {
        toast.classList.remove("show");
        setTimeout(() => toast.remove(), 500);
    }, 3000);
};
window.toggleFilterDropdown = function () {
    const dropdown = document.getElementById("filterDropdown");
    dropdown.classList.toggle("hidden");
};

document.addEventListener("click", function (event) {
    const isToggle = event.target.closest(".filter-post");
    const isDropdown = event.target.closest("#filterDropdown");

    if (!isToggle && !isDropdown) {
        document.getElementById("filterDropdown")?.classList.add("hidden");
    }
});

window.submitComment = async function (questionId, button) {
    const input = button.previousElementSibling;
    const content = input.value.trim();
    const user = getCurrentUser();

    if (!user) return alert("Bạn cần đăng nhập để bình luận.");
    if (!content) return alert("Nội dung bình luận trống!");

    const newAnswerRef = push(ref(db, "answers"));
    await set(newAnswerRef, {
        questionId,
        content,
        authorUserId: user.userId,
        createdAt: new Date().toISOString(),
        voteCount: 0
    });

    input.value = "";
    showToast("✅ Đã gửi bình luận!");
    setTimeout(() => {
        loadPosts();
    }, 300); // 300ms
};

window.removeTagFromEditModal = async function (questionId, tagId, buttonElement) {
    const user = getCurrentUser();
    if (!user) {
        showToast("Bạn cần đăng nhập để thực hiện thao tác này.");
        return;
    }

    // Tìm và xóa mối liên kết trong questionTags trên Firebase
    const questionTagsRef = ref(db, 'questionTags');
    const snapshot = await get(questionTagsRef);
    const qTags = snapshot.val() || {};

    let associationKeyToRemove = null;
    for (const key in qTags) {
        if (qTags[key].questionId === questionId && qTags[key].tagId === tagId) {
            associationKeyToRemove = key;
            break;
        }
    }

    if (associationKeyToRemove) {
        await remove(ref(db, `questionTags/${associationKeyToRemove}`));
        showToast(`✅ Đã gỡ thẻ #${allTags[tagId]?.tagName || tagId}!`);
        // Xóa thẻ khỏi UI ngay lập tức
        buttonElement.closest('.tag-item').remove();
        // Cập nhật lại biến global allQuestionTags sau khi xóa
        delete allQuestionTags[associationKeyToRemove];
    } else {
        showToast("Không tìm thấy mối liên kết thẻ để xóa.");
    }
};
window.openEditCommentModal = function (answerId) {
    console.log("📢 Đã nhấn sửa bình luận với ID:", answerId);

    const comment = allAnswers[answerId];
    if (!comment) {
        showToast("Không tìm thấy bình luận để chỉnh sửa.");
        return;
    }
    const author = allUsers[comment.authorUserId];
    const currentUser = getCurrentUser(); // Lấy current user để hiển thị thông tin

    // Kiểm tra xem có người dùng đăng nhập hay không
    if (!currentUser) {
        showToast("Bạn cần đăng nhập để chỉnh sửa bình luận.");
        return;
    }

    document.getElementById("editCommentAvatar").src = author?.avatar || 'https://i.imgur.com/uIgDDDd.png';
    document.getElementById("editCommentUserName").textContent = author?.fullName || 'Ẩn danh';
    document.getElementById("editCommentUserScore").textContent = `${author?.reputationScore || 0} điểm`;
    document.getElementById("editCommentContent").value = comment.content;

    currentEditingCommentId = answerId; // Lưu ID của bình luận đang sửa
    document.getElementById("editCommentModalOverlay").classList.remove("hidden");
};

window.closeEditCommentModal = function () {
    currentEditingCommentId = null;
    document.getElementById("editCommentModalOverlay").classList.add("hidden");
};

window.submitEditComment = async function () {
    const content = document.getElementById("editCommentContent").value.trim();
    const id = currentEditingCommentId;
    const currentUser = getCurrentUser(); // Lấy current user

    if (!id || !content) {
        showToast("Nội dung bình luận trống!");
        return;
    }

    // Chỉ cần kiểm tra người dùng có đăng nhập hay không
    if (!currentUser) {
        showToast("Bạn không có quyền cập nhật bình luận này.");
        return;
    }

    try {
        await update(ref(db, `answers/${id}`), { content: content });
        showToast("✅ Đã cập nhật bình luận!");
        closeEditCommentModal();
        setTimeout(() => loadPosts(), 300);
    } catch (error) {
        console.error("Lỗi khi cập nhật bình luận:", error);
        showToast("Lỗi khi cập nhật bình luận.");
    }
};
window.openDeleteCommentPopup = function (answerId) {
    deletingCommentId = answerId;
    document.getElementById("deleteModal").classList.remove("hidden"); // Mở modal xác nhận chung
    document.querySelector("#deleteModal .modal-body").textContent = "Bạn có chắc muốn xóa bình luận này không?";
    document.querySelector("#deleteModal .modal-footer .submit-post-btn:first-child").onclick = confirmDeleteComment;
};

window.confirmDeleteComment = async function () {
    const id = deletingCommentId;
    const currentUser = getCurrentUser(); // Lấy current user

    if (!id) return;

    // Chỉ cần kiểm tra người dùng có đăng nhập hay không
    if (!currentUser) {
        showToast("Bạn không có quyền xóa bình luận này.");
        closeDeleteModal(); // Đóng modal nếu không có quyền
        return;
    }

    try {
        // Xóa các vote liên quan đến bình luận này
        const votesRef = ref(db, 'votes');
        const votesSnapshot = await get(votesRef);
        const allVotesData = votesSnapshot.val() || {};

        for (const voteId in allVotesData) {
            if (allVotesData[voteId].answerId === id) {
                await remove(ref(db, `votes/${voteId}`));
            }
        }

        // Xóa bình luận
        await remove(ref(db, `answers/${id}`));
        showToast("✅ Đã xóa bình luận thành công!");
        closeDeleteModal(); // Gọi hàm đóng modal chung
        setTimeout(() => loadPosts(), 300);
    } catch (error) {
        console.error("Lỗi khi xóa bình luận:", error);
        showToast("Lỗi khi xóa bình luận.");
    }
};
window.toggleUserDropdown = function (event) { 
    const userDropdown = document.getElementById("userDropdown");
    if (userDropdown) {
        userDropdown.classList.toggle('hidden'); 
        if (event) {
            event.stopPropagation();
        }
    } else {
        console.warn("Không tìm thấy phần tử có ID 'userDropdown' để bật/tắt.");
    }
};
window.logout = function () {
    localStorage.removeItem("currentUser");
    localStorage.removeItem("currentUserId"); 
    window.location.href = "login.html"; 
};
document.addEventListener('click', function (event) {
    const userDropdown = document.getElementById("userDropdown");
    const userMenuContainer = document.querySelector(".user-menu-container");
if (userDropdown && userMenuContainer) {
        
        if (!userDropdown.classList.contains("hidden") && !userMenuContainer.contains(event.target)) {
            userDropdown.classList.add("hidden");
        }
    }
});