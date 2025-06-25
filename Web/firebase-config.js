// firebase-config.js
import { initializeApp } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-app.js";
import { getDatabase } from "https://www.gstatic.com/firebasejs/9.6.1/firebase-database.js";

// Cấu hình Firebase
const firebaseConfig = {
    apiKey: "AIzaSyBQhxYCLlfSEqpUqP5Ji5XWFB3Oj9qOlHU",
    authDomain: "baitaplon-3c6e3.firebaseapp.com",
    databaseURL: "https://baitaplon-3c6e3-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "baitaplon-3c6e3",
    storageBucket: "baitaplon-3c6e3.firebasestorage.app",
    messagingSenderId: "352856269124",
    appId: "1:352856269124:web:81b5404a2215b34995f711"
};

// Khởi tạo Firebase App và lấy Database
const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

// ✅ Chỉ export db
export { db };
