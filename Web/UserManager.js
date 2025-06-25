import { get, ref, update } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-database.js";
import { db } from './firebase-config.js';

let usersData = [];
const rowsPerPage = 10;

export async function loadUsers() {
    const snapshot = await get(ref(db, 'users'));
    const users = snapshot.val() || {};
    usersData = Object.values(users);

    if (usersData.length === 0) {
        document.getElementById("userTableBody").innerHTML = "<tr><td colspan='7'>Không có dữ liệu người dùng.</td></tr>";
        return;
    }

    renderTablePage(1);
    renderPagination(usersData.length);
}

function renderTablePage(page) {
    const tbody = document.getElementById("userTableBody");
    tbody.innerHTML = "";

    const start = (page - 1) * rowsPerPage;
    const end = start + rowsPerPage;
    const pageData = usersData.slice(start, end);

    pageData.forEach((user, index) => {
        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${start + index + 1}</td>
            <td>${user.fullName || ""}</td>
            <td>${user.birthday || ""}</td>
            <td>${user.address || ""}</td>
            <td>${user.sex || ""}</td>
            <td>${user.email || ""}</td>
            <td>${user.reputationScore || 0}</td>
        `;
        tbody.appendChild(tr);
    });
}

function renderPagination(totalItems) {
    const pagination = document.getElementById("pagination");
    pagination.innerHTML = "";

    const totalPages = Math.ceil(totalItems / rowsPerPage);
    for (let i = 1; i <= totalPages; i++) {
        const btn = document.createElement("button");
        btn.textContent = i;
        btn.addEventListener("click", () => {
            renderTablePage(i);
            setActivePage(i);
        });
        pagination.appendChild(btn);
    }

    setActivePage(1);
}

function setActivePage(activePage) {
    const buttons = document.querySelectorAll("#pagination button");
    buttons.forEach((btn, index) => {
        btn.classList.toggle("active", index + 1 === activePage);
    });
}

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
// Mở modal thêm người dùng
window.openAddUserModal = function () {
    document.getElementById("addUserModal").style.display = "flex";
}

// Đóng modal thêm người dùng
window.closeAddUserModal = function () {
    document.getElementById("addUserModal").style.display = "none";
}

// Gửi dữ liệu người dùng mới
window.submitNewUser = async function () {
  const fullName = document.getElementById("userFullName").value.trim();
  const password = document.getElementById("userPassword").value.trim();
  const birthday = document.getElementById("userBirthDate").value;
  const address = document.getElementById("userHometown").value.trim();
  const sex = document.getElementById("userGender").value;
  const email = document.getElementById("userEmail").value.trim();
  const roleId = document.getElementById("userRole").value;

  if (!fullName || !password || !birthday || !address || !sex || !email || !roleId) {
    alert("❌ Vui lòng nhập đầy đủ thông tin.");
    return;
  }

  if (!email.endsWith("@e.tlu.edu.vn")) {
    alert("❌ Email phải kết thúc bằng @e.tlu.edu.vn");
    return;
  }

  const snapshot = await get(ref(db, 'users'));
  const users = snapshot.val() || {};

  let maxId = 0;
  Object.keys(users).forEach(id => {
    const match = id.match(/^U(\d+)$/);
    if (match) {
      const num = parseInt(match[1]);
      if (num > maxId) maxId = num;
    }
  });

  const newId = `U${String(maxId + 1).padStart(3, '0')}`;
  const createdAt = new Date().toISOString();

  const newUser = {
    userId: newId,
    fullName,
    password,
    birthday,
    address,
    sex,
    email,
    roleId,
    createdAt,
    avatar: "https://i.imgur.com/uIgDDDd.png", // PNG mặc định
    reputationScore: 0
  };

  await update(ref(db, `users/${newId}`), newUser);
  closeAddUserModal();
  showToast("✅ Thêm người dùng thành công!");
  loadUsers();
}

function showToast(message, duration = 3000) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.style.display = "block";
    setTimeout(() => {
        toast.style.display = "none";
    }, duration);
}

loadUsers();