import { getDatabase, ref, get, child } from "https://www.gstatic.com/firebasejs/9.22.0/firebase-database.js";
import { db } from './firebase-config.js'; // Đảm bảo firebase-config.js export đúng db

document.querySelector(".btn-login").addEventListener("click", async () => {
  const email = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value.trim();

  if (!email || !password) {
    alert("Vui lòng nhập đầy đủ thông tin.");
    return;
  }

  try {
    const snapshot = await get(child(ref(db), "users"));
    if (snapshot.exists()) {
      let found = false;

      snapshot.forEach(childSnap => {
        const acc = childSnap.val();

        if (acc.email === email && acc.passwordHash === password) {
          found = true;

          // ✅ Lưu người dùng vào localStorage
          localStorage.setItem("currentUser", JSON.stringify(acc));

          // ✅ Điều hướng theo vai trò
          if (acc.roleId === "R1") {
            window.location.href = "home.html";
          } else if (acc.roleId === "R2") {
            alert("Chưa hỗ trợ tài khoản người dùng.");
          } else {
            alert("Role không hợp lệ.");
          }
        }
      });

      if (!found) {
        alert("Sai email hoặc mật khẩu.");
      }
    } else {
      alert("Không tìm thấy dữ liệu tài khoản.");
    }
  } catch (err) {
    console.error("Lỗi khi truy vấn Firebase:", err);
    alert("Đã xảy ra lỗi, vui lòng thử lại.");
  }
});
