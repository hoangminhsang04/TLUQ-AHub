import { db } from './firebase-config.js';
import { get, ref, push, update, remove } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-database.js";

const tagTableBody = document.getElementById("tagTableBody");
const pagination = document.getElementById("pagination");
const rowsPerPage = 10;

let currentPage = 1;
let tagList = [];
let tagIdToDelete = null;
let tagIdToEdit = null;


async function loadTags() {
    const [tagSnap, qTagSnap] = await Promise.all([
        get(ref(db, 'tags')),
        get(ref(db, 'questionTags'))
    ]);

    const tags = tagSnap.exists() ? tagSnap.val() : {};
    const questionTags = qTagSnap.exists() ? qTagSnap.val() : {};

    const tagCounts = {};
    Object.values(questionTags).forEach(qt => {
        if (!tagCounts[qt.tagId]) tagCounts[qt.tagId] = 0;
        tagCounts[qt.tagId]++;
    });

    tagList = Object.entries(tags).map(([tagId, tag]) => ({
        tagId,
        tagName: tag.tagName,
        count: tagCounts[tagId] || 0
    }));

    renderPagination(tagList.length);
    renderTablePage(1);
}

function renderTablePage(page) {
    currentPage = page;
    tagTableBody.innerHTML = '';

    const start = (page - 1) * rowsPerPage;
    const current = tagList.slice(start, start + rowsPerPage);

    current.forEach((tag, index) => {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td>${start + index + 1}</td>
            <td>${tag.tagName}</td>
            <td>${tag.tagId}</td>
            <td>${tag.count}</td>
            <td>
                <button onclick="editTag('${tag.tagId}', '${tag.tagName}')" style="background-color: #1976D2; color: white; border: none; padding: 4px 8px; border-radius: 4px; margin-right: 4px; cursor: pointer;">Sửa</button>
                <button onclick="deleteTag('${tag.tagId}')" style="background-color: #D32F2F; color: white; border: none; padding: 4px 8px; border-radius: 4px; cursor: pointer;">Xóa</button>
            </td>
        `;
        tagTableBody.appendChild(row);
    });

    setActivePage(page);
}

function renderPagination(totalItems) {
    pagination.innerHTML = "";
    const totalPages = Math.ceil(totalItems / rowsPerPage);

    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement("button");
        btn.textContent = i;
        btn.classList.add("pagination-btn");

        btn.addEventListener("click", () => {
            renderTablePage(i);
        });

        pagination.appendChild(btn);
    }
}

function setActivePage(pageNumber) {
    const buttons = document.querySelectorAll("#pagination button");
    buttons.forEach((btn, index) => {
        if (index + 1 === pageNumber) {
            btn.classList.add("active");
        } else {
            btn.classList.remove("active");
        }
    });
}

// ========== Toast Notification ==========
function showToast(message = "Thao tác thành công!") {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.style.display = "block";
    setTimeout(() => {
        toast.style.display = "none";
    }, 3000);
}

// ========== Modal Thêm Thẻ ==========
window.openAddTagModal = function () {
    document.getElementById("addTagModal").style.display = "flex";
};

window.closeAddTagModal = function () {
    document.getElementById("addTagModal").style.display = "none";
    document.getElementById("newTagName").value = "";
};

window.submitNewTag = async function () {
    const tagName = document.getElementById("newTagName").value.trim();
    if (tagName === "") {
        alert("Vui lòng nhập tên thẻ.");
        return;
    }

    const snap = await get(ref(db, 'tags'));
    const tags = snap.exists() ? snap.val() : {};

    const isDuplicate = Object.values(tags).some(tag =>
        tag.tagName.trim().toLowerCase() === tagName.toLowerCase()
    );
    if (isDuplicate) {
        alert("Tên thẻ đã tồn tại!");
        return;
    }

    let maxId = 0;
    Object.keys(tags).forEach(id => {
        const match = id.match(/^T(\d+)$/);
        if (match) {
            const num = parseInt(match[1]);
            if (num > maxId) maxId = num;
        }
    });

    const newId = `T${String(maxId + 1).padStart(3, '0')}`;
    await update(ref(db, `tags/${newId}`), {
        tagId: newId,
        tagName
    });

    closeAddTagModal();
    showToast("Thêm thẻ thành công!");
    loadTags();
};

// ========== Modal Xác Nhận Xóa ==========
window.deleteTag = function (tagId) {
    tagIdToDelete = tagId;
    document.getElementById("deleteTagModal").style.display = "flex";
};

window.closeDeleteTagModal = function () {
    document.getElementById("deleteTagModal").style.display = "none";
    tagIdToDelete = null;
};

document.getElementById("confirmDeleteBtn").addEventListener("click", async () => {
    if (tagIdToDelete) {
        await remove(ref(db, `tags/${tagIdToDelete}`));
        closeDeleteTagModal();
        showToast("Xóa thẻ thành công!");
        loadTags();
    }
});

// ========== Sửa Thẻ ==========
window.editTag = async function (tagId, oldName) {
    const newName = prompt("Nhập tên mới cho thẻ:", oldName);
    if (!newName || newName.trim() === "" || newName === oldName) return;

    await update(ref(db, `tags/${tagId}`), {
        tagName: newName.trim()
    });

    showToast("Cập nhật thẻ thành công!");
    loadTags();
};
document.addEventListener('DOMContentLoaded', () => {
    const stored = localStorage.getItem("currentUser");
    let user = null;
    try {
        user = stored ? JSON.parse(stored) : null;
    } catch {
        user = null;
    }

    const avatarImg = document.getElementById("avatarImg");
    if (avatarImg && user?.avatar) {
        avatarImg.src = user.avatar;
    } else if (avatarImg) {
        avatarImg.src = "https://api.dicebear.com/7.x/thumbs/svg?seed=user";
    }
});

window.editTag = function(tagId, oldName) {
    tagIdToEdit = tagId;
    document.getElementById("editTagName").value = oldName;
    document.getElementById("editTagModal").style.display = "flex";
};

window.closeEditTagModal = function () {
    document.getElementById("editTagModal").style.display = "none";
    tagIdToEdit = null;
};

window.submitEditTag = async function () {
    const newName = document.getElementById("editTagName").value.trim();
    if (!newName) {
        alert("Vui lòng nhập tên thẻ mới.");
        return;
    }

    if (!tagIdToEdit) return;

    await update(ref(db, `tags/${tagIdToEdit}`), {
        tagName: newName
    });

    closeEditTagModal();
    showToast("Cập nhật thẻ thành công!");
    loadTags();
};
// ========== Khởi động ==========
document.addEventListener("DOMContentLoaded", loadTags);
