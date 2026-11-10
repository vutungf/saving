package vn.sotietkiem.sync

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import vn.sotietkiem.data.Codec
import vn.sotietkiem.domain.SyncData
import vn.sotietkiem.domain.merge
import vn.sotietkiem.util.Crypto
import java.util.UUID

/** Đang cần người dùng làm gì với mật khẩu đồng bộ. */
enum class PassNeed { NONE, CREATE, ENTER }

data class SyncState(
    /** Đã có google-services.json (Firebase được cấu hình) chưa. */
    val available: Boolean = false,
    /** Người dùng đã chọn đồng bộ với tài khoản Google. */
    val enabled: Boolean = false,
    val email: String? = null,
    val pass: PassNeed = PassNeed.NONE,
    val busy: Boolean = false,
    val error: String? = null,
    val lastSync: Long = 0,
) {
    val ready get() = enabled && email != null && pass == PassNeed.NONE
}

/**
 * Đồng bộ dữ liệu với Cloud Firestore, mã hóa đầu-cuối:
 * - Đăng nhập bằng tài khoản Google (Credential Manager + Firebase Auth).
 * - Toàn bộ dữ liệu được mã hóa AES-256-GCM bằng khóa suy ra từ "mật khẩu đồng bộ" trước khi gửi lên,
 *   nên trên Firebase chỉ có dữ liệu đã mã hóa. Mỗi người dùng một tài liệu `users/{uid}`.
 * - Khóa đã suy ra được lưu trên máy (bọc bằng khóa Keystore) để không phải nhập lại mật khẩu.
 * - Gộp hai phía theo từng sổ / người sở hữu (bản sửa sau thắng, có ghi nhận mục đã xóa).
 */
class CloudSync(
    context: Context,
    private val scope: CoroutineScope,
    private val local: () -> SyncData,
    private val applyData: (SyncData) -> Unit,
    private val onSynced: () -> Unit,
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("stk_sync", Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private var listener: ListenerRegistration? = null
    private var pushJob: Job? = null

    private val available = FirebaseApp.getApps(app).isNotEmpty()
    private val auth get() = FirebaseAuth.getInstance()
    private val db get() = FirebaseFirestore.getInstance()

    private val deviceId: String = prefs.getString("device", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("device", it).apply()
    }

    private val _state = MutableStateFlow(
        SyncState(
            available = available,
            enabled = available && prefs.getBoolean("enabled", false),
            email = if (available) auth.currentUser?.email else null,
            lastSync = prefs.getLong("lastSync", 0),
        )
    )
    val state: StateFlow<SyncState> = _state.asStateFlow()

    // ---- Khóa đồng bộ lưu trên máy (đã bọc bằng Keystore) ----

    private fun savedKey(uid: String): ByteArray? {
        if (prefs.getString("keyUid", null) != uid) return null
        val enc = prefs.getString("key", null) ?: return null
        return runCatching { Crypto.decryptLocal(Crypto.unb64(enc)) }.getOrNull()
    }

    private fun saveKey(uid: String, key: ByteArray) {
        prefs.edit().putString("keyUid", uid).putString("key", Crypto.b64(Crypto.encryptLocal(key))).apply()
    }

    private fun clearKey() = prefs.edit().remove("key").remove("keyUid").apply()

    private fun doc(uid: String) = db.collection("users").document(uid)

    private fun fail(msg: String) = _state.update { it.copy(busy = false, error = msg) }

    // ---- Khởi động ----

    /** Gọi khi mở app: nếu đã đăng nhập và có khóa thì bắt đầu đồng bộ. */
    fun start() {
        if (!_state.value.enabled) return
        val user = auth.currentUser ?: run { _state.update { it.copy(email = null) }; return }
        if (savedKey(user.uid) == null) {
            scope.launch { checkRemote() }
            return
        }
        listen(user.uid)
        syncNow()
    }

    // ---- Đăng nhập / đăng xuất ----

    /** Đăng nhập Google. Cần Activity để hiện hộp chọn tài khoản. */
    suspend fun signIn(activity: Activity) {
        if (!available) { fail("Chưa cấu hình Firebase (thiếu google-services.json)."); return }
        val clientIdRes = app.resources.getIdentifier("default_web_client_id", "string", app.packageName)
        if (clientIdRes == 0) { fail("Thiếu cấu hình đăng nhập Google trong Firebase."); return }
        _state.update { it.copy(busy = true, error = null) }
        try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(app.getString(clientIdRes))
                .build()
            val result = CredentialManager.create(activity)
                .getCredential(activity, GetCredentialRequest.Builder().addCredentialOption(option).build())
            val token = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
            prefs.edit().putBoolean("enabled", true).apply()
            _state.update { it.copy(enabled = true, email = auth.currentUser?.email, busy = false) }
            checkRemote()
        } catch (e: GetCredentialCancellationException) {
            _state.update { it.copy(busy = false) }
        } catch (e: Exception) {
            fail("Đăng nhập không thành công: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    /** Sau khi đăng nhập: xem trên mây đã có dữ liệu chưa để hỏi tạo hay nhập mật khẩu đồng bộ. */
    private suspend fun checkRemote() {
        val user = auth.currentUser ?: return
        _state.update { it.copy(busy = true, error = null) }
        try {
            val snap = doc(user.uid).get(Source.SERVER).await()
            val salt = snap.getString("salt")
            val key = savedKey(user.uid)
            val pass = when {
                salt == null -> PassNeed.CREATE
                key != null && verify(key, snap.getString("check")) -> PassNeed.NONE
                else -> PassNeed.ENTER
            }
            _state.update { it.copy(busy = false, pass = pass, email = user.email) }
            if (pass == PassNeed.NONE) { listen(user.uid); syncNow() }
        } catch (e: Exception) {
            fail("Không kết nối được Firebase: ${e.message ?: "lỗi mạng"}")
        }
    }

    private fun verify(key: ByteArray, check: String?): Boolean =
        check != null && runCatching { Crypto.decrypt(key, Crypto.unb64(check)).decodeToString() == CHECK }.getOrDefault(false)

    /** Lần đầu: tạo mật khẩu đồng bộ và đưa dữ liệu trên máy lên. */
    suspend fun createPassphrase(pass: String) = replaceKey(pass)

    /** Đổi mật khẩu đồng bộ: mã hóa lại toàn bộ dữ liệu. Máy khác sẽ được hỏi mật khẩu mới. */
    suspend fun changePassphrase(pass: String) = replaceKey(pass)

    private suspend fun replaceKey(pass: String) {
        val user = auth.currentUser ?: return
        _state.update { it.copy(busy = true, error = null) }
        try {
            mutex.withLock {
                val salt = Crypto.randomBytes(16)
                val key = withContext(Dispatchers.Default) { Crypto.deriveKey(pass, salt) }
                // Gộp với dữ liệu đang có trên mây (nếu còn đọc được bằng khóa cũ) để không mất gì
                val oldKey = savedKey(user.uid)
                val snap = doc(user.uid).get(Source.SERVER).await()
                val remote = oldKey?.let { k -> snap.getString("data")?.let { decode(k, it) } }
                val merged = if (remote != null) merge(local(), remote) else local()
                applyData(merged)
                doc(user.uid).set(
                    mapOf(
                        "v" to 1,
                        "salt" to Crypto.b64(salt),
                        "iter" to Crypto.PBKDF2_ITER,
                        "check" to Crypto.b64(Crypto.encrypt(key, CHECK.toByteArray())),
                        "data" to encode(key, merged),
                        "device" to deviceId,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
                saveKey(user.uid, key)
            }
            done()
            listen(user.uid)
        } catch (e: Exception) {
            fail("Không lưu được lên Firebase: ${e.message ?: "lỗi mạng"}")
        }
    }

    /** Máy mới hoặc mật khẩu đã đổi ở máy khác: nhập mật khẩu đồng bộ để mở dữ liệu trên mây. */
    suspend fun enterPassphrase(pass: String): Boolean {
        val user = auth.currentUser ?: return false
        _state.update { it.copy(busy = true, error = null) }
        return try {
            val snap = doc(user.uid).get(Source.SERVER).await()
            val salt = Crypto.unb64(snap.getString("salt") ?: return false.also { fail("Chưa có dữ liệu trên mây.") })
            val iter = snap.getLong("iter")?.toInt() ?: Crypto.PBKDF2_ITER
            val key = withContext(Dispatchers.Default) { Crypto.deriveKey(pass, salt, iter) }
            if (!verify(key, snap.getString("check"))) {
                fail("Mật khẩu đồng bộ không đúng.")
                return false
            }
            saveKey(user.uid, key)
            _state.update { it.copy(busy = false, pass = PassNeed.NONE) }
            listen(user.uid)
            syncNow()
            true
        } catch (e: Exception) {
            fail("Không kết nối được Firebase: ${e.message ?: "lỗi mạng"}")
            false
        }
    }

    /** Đăng xuất và quay về chỉ lưu trên máy. Dữ liệu trên máy giữ nguyên. */
    suspend fun signOut() {
        listener?.remove(); listener = null
        pushJob?.cancel()
        if (available) {
            auth.signOut()
            runCatching { CredentialManager.create(app).clearCredentialState(ClearCredentialStateRequest()) }
        }
        clearKey()
        prefs.edit().putBoolean("enabled", false).apply()
        _state.update { SyncState(available = available, lastSync = it.lastSync) }
    }

    /** Xóa dữ liệu của tài khoản này trên Firebase rồi đăng xuất. */
    suspend fun deleteCloudData() {
        val user = auth.currentUser ?: return
        _state.update { it.copy(busy = true, error = null) }
        try {
            listener?.remove(); listener = null
            doc(user.uid).delete().await()
            signOut()
        } catch (e: Exception) {
            fail("Không xóa được: ${e.message ?: "lỗi mạng"}")
        }
    }

    // ---- Đồng bộ ----

    private fun encode(key: ByteArray, d: SyncData): String =
        Crypto.b64(Crypto.encrypt(key, Codec.json.encodeToString(SyncData.serializer(), d).toByteArray()))

    private fun decode(key: ByteArray, data: String): SyncData? = runCatching {
        Codec.json.decodeFromString(SyncData.serializer(), Crypto.decrypt(key, Crypto.unb64(data)).decodeToString())
    }.getOrNull()

    /** Có thay đổi trên máy: đợi 2 giây (gom nhiều thao tác liên tiếp) rồi đồng bộ. */
    fun scheduleSync() {
        if (!_state.value.ready) return
        pushJob?.cancel()
        pushJob = scope.launch { delay(2000); sync() }
    }

    fun syncNow() {
        if (!_state.value.ready) return
        scope.launch { sync() }
    }

    /** Tải về, gộp với dữ liệu trên máy, rồi gửi bản đã gộp lên nếu khác. */
    private suspend fun sync() = mutex.withLock {
        val user = auth.currentUser ?: return@withLock
        val key = savedKey(user.uid) ?: run { _state.update { it.copy(pass = PassNeed.ENTER) }; return@withLock }
        _state.update { it.copy(busy = true, error = null) }
        try {
            val snap = doc(user.uid).get(Source.SERVER).await()
            if (snap.exists() && !verify(key, snap.getString("check"))) {
                // Mật khẩu đã đổi ở máy khác
                clearKey()
                _state.update { it.copy(busy = false, pass = PassNeed.ENTER, error = "Mật khẩu đồng bộ đã được đổi ở máy khác.") }
                return@withLock
            }
            val remote = snap.getString("data")?.let { decode(key, it) }
            val merged = if (remote != null) merge(local(), remote) else local()
            if (merged != local()) applyData(merged)
            if (merged != remote) {
                doc(user.uid).set(
                    mapOf("data" to encode(key, merged), "device" to deviceId, "updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge(),
                ).await()
            }
            done()
        } catch (e: Exception) {
            fail("Chưa đồng bộ được: ${e.message ?: "lỗi mạng"}. Dữ liệu vẫn được lưu trên máy.")
        }
    }

    private fun done() {
        val t = System.currentTimeMillis()
        prefs.edit().putLong("lastSync", t).apply()
        _state.update { it.copy(busy = false, error = null, pass = PassNeed.NONE, lastSync = t) }
        onSynced()
    }

    /** Nghe thay đổi từ máy khác để cập nhật ngay. */
    private fun listen(uid: String) {
        if (listener != null) return
        listener = doc(uid).addSnapshotListener { snap, err ->
            if (err != null || snap == null || snap.metadata.hasPendingWrites()) return@addSnapshotListener
            if (snap.getString("device") != deviceId) syncNow()
        }
    }

    companion object {
        /** Chuỗi mẫu được mã hóa để kiểm tra mật khẩu đồng bộ có đúng không. */
        private const val CHECK = "so-tiet-kiem-ok"
    }
}
