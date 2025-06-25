import { get, ref, update, remove } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-database.js";
import { db } from './firebase-config.js'; // Đảm bảo đường dẫn này đúng

// Hàm lấy thông tin người dùng hiện tại từ localStorage
// Vì mọi người dùng đăng nhập đều được coi là admin, không cần kiểm tra roleId cụ thể
function getCurrentUser() {
    const stored = localStorage.getItem("currentUser");
    if (!stored) return null;
    try {
        return JSON.parse(stored);
    } catch {
        console.error("Lỗi khi parse currentUser từ localStorage.");
        return null;
    }
}

// Hàm tính thời gian "cách đây bao lâu"
function getTimeAgo(createdAt) {
    const now = new Date();
    const created = new Date(createdAt);
    const diffMs = now - created;
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return "Hôm nay";
    if (diffDays === 1) return "1 ngày trước";
    return `${diffDays} ngày trước`;
}

// Hàm tải và hiển thị các bài viết chờ duyệt
async function loadModerationPosts() {
    const snapshot = await get(ref(db));
    const data = snapshot.val();

    // Lấy dữ liệu từ snapshot, đảm bảo các object không bị undefined
    const questions = data.questions || {};
    const users = data.users || {};
    const questionTags = data.questionTags || {};
    const tags = data.tags || {};

    const postList = document.getElementById("post-list");
    postList.innerHTML = ""; // Xóa nội dung cũ trước khi render

    // Lọc ra các bài viết có trạng thái "Chờ duyệt"
    const pendingQuestions = Object.values(questions).filter(q => q.status === "Chờ duyệt");

    if (pendingQuestions.length === 0) {
        // Nếu không có bài viết nào chờ duyệt, hiển thị thông báo
        postList.innerHTML = `
            <div class="empty-moderation-message" style="
                text-align: center;
                padding: 50px;
                font-size: 1.2em;
                color: #555;
                background-color: #f9f9f9;
                border-radius: 8px;
                margin-top: 20px;
                box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            ">
                Không có bài viết nào cần duyệt.
            </div>
        `;
    } else {
        // Nếu có bài viết chờ duyệt, render chúng
        pendingQuestions.forEach(q => {
            const author = users[q.authorUserId];
            // Lấy danh sách tên thẻ cho bài viết
            const tagNames = Object.values(questionTags)
                .filter(qt => qt.questionId === q.questionId)
                .map(qt => tags[qt.tagId]?.tagName || "");

            const html = `
                <div class="post-card" style="grid-column: span 8;">
                    <div class="post-header">
                        <div class="post-avatar">
                            <img src="${author?.avatar || 'https://i.imgur.com/uIgDDDd.png'}" alt="avatar">
                        </div>
                        <div class="post-user-info">
                            <div class="post-name-rep">
                                <span class="user-name">${author?.fullName || 'Ẩn danh'}</span>
                                <span class="user-reputation">
                                    <iconify-icon icon="ph:medal"></iconify-icon>
                                    <span>${author?.reputationScore || 0} điểm</span>
                                </span>
                            </div>
                            <div class="post-time">${getTimeAgo(q.createdAt)}</div>
                        </div>
                    </div>

                    <div class="post-body">
                        <div class="post-title">${q.title}</div>
                        <div class="post-content">${q.content}</div>
                        <div class="post-tags">
                            ${tagNames.map(t => `<span class="post-tag">#${t}</span>`).join(" ")}
                        </div>
                    </div>

                    <div class="post-actions">
                        <button onclick="approvePost('${q.questionId}')" style="margin-right: 10px;">✅ Duyệt</button>
                        <button onclick="rejectPost('${q.questionId}')">❌ Từ chối</button>
                    </div>
                </div>
            `;
            postList.innerHTML += html;
        });
    }
}

// Hàm duyệt bài viết
window.approvePost = async function (questionId) {
    // Kiểm tra xem người dùng có đang đăng nhập không (mặc định là admin)
    const currentUser = getCurrentUser();
    if (!currentUser) {
        alert("Bạn cần đăng nhập để thực hiện hành động này.");
        return;
    }

    try {
        await update(ref(db, `questions/${questionId}`), { status: "Đã duyệt" });
        alert("✅ Đã duyệt bài viết.");
        loadModerationPosts(); // Tải lại danh sách sau khi duyệt
    } catch (error) {
        console.error("Lỗi khi duyệt bài viết:", error);
        alert("Đã xảy ra lỗi khi duyệt bài viết.");
    }
};

// Hàm từ chối bài viết
window.rejectPost = async function (questionId) {
    // Kiểm tra xem người dùng có đang đăng nhập không (mặc định là admin)
    const currentUser = getCurrentUser();
    if (!currentUser) {
        alert("Bạn cần đăng nhập để thực hiện hành động này.");
        return;
    }

    try {
        await update(ref(db, `questions/${questionId}`), { status: "Từ chối" });
        alert("❌ Đã từ chối bài viết.");
        loadModerationPosts(); // Tải lại danh sách sau khi từ chối
    } catch (error) {
        console.error("Lỗi khi từ chối bài viết:", error);
        alert("Đã xảy ra lỗi khi từ chối bài viết.");
    }
};

// Khi DOM được tải hoàn chỉnh, thực hiện các khởi tạo
document.addEventListener('DOMContentLoaded', () => {
    const user = getCurrentUser();
    const avatarImg = document.getElementById("avatarImg");
    if (avatarImg) {
        // Cập nhật ảnh đại diện người dùng trên header
        // Nếu user hoặc avatar không tồn tại, dùng ảnh placeholder
        avatarImg.src = user?.avatar || "https://api.dicebear.com/7.x/thumbs/svg?seed=user";
    }
    loadModerationPosts(); // Tải các bài viết chờ duyệt khi trang tải
});