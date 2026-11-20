package app.socialradar

import android.content.Context
import io.github.tdlibandroid.ktx.TdClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import org.drinkless.tdlib.TdApi
import app.socialradar.search.SearchQueryPlanner
import app.socialradar.search.SearchRequest
import java.util.Locale
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class RadarChat(val id: Long, val title: String, val type: String, val username: String = "")
data class RadarAuth(
    val label: String = "Не подключено",
    val ready: Boolean = false,
    val codeRequired: Boolean = false,
    val passwordRequired: Boolean = false,
    val phoneRequired: Boolean = false,
    val emailRequired: Boolean = false,
    val emailCodeRequired: Boolean = false,
    val registrationRequired: Boolean = false,
    val otherDeviceLink: String = ""
)

class TelegramSession(private val context: Context) {
    private val prefs = context.getSharedPreferences("telegram_session_config", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val planner = SearchQueryPlanner()
    private var client: TdClient? = null
    private val _auth = MutableStateFlow(RadarAuth())
    val auth: StateFlow<RadarAuth> = _auth
    private val _results = MutableStateFlow<List<RadarChat>>(emptyList())
    val results: StateFlow<List<RadarChat>> = _results
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    init { migrateLegacyCredentials() }
    val apiId: String get() = CredentialVault.decrypt(prefs.getString("api_id_encrypted", null)) ?: (prefs.getString("api_id", "") ?: "")
    val apiHash: String get() = CredentialVault.decrypt(prefs.getString("api_hash_encrypted", null)) ?: (prefs.getString("api_hash", "") ?: "")

    private fun migrateLegacyCredentials() {
        val oldId = prefs.getString("api_id", null)
        val oldHash = prefs.getString("api_hash", null)
        if (oldId.isNullOrBlank() && oldHash.isNullOrBlank()) return
        try {
            val editor = prefs.edit()
            if (!oldId.isNullOrBlank()) editor.putString("api_id_encrypted", CredentialVault.encrypt(oldId))
            if (!oldHash.isNullOrBlank()) editor.putString("api_hash_encrypted", CredentialVault.encrypt(oldHash))
            editor.remove("api_id").remove("api_hash").apply()
        } catch (_: Exception) {
            _message.value = "Не удалось защитить API-данные на этом устройстве. Старые ключи не удалены."
        }
    }

    fun configure(id: String, hash: String) {
        val parsed = id.trim().toIntOrNull()
        val cleanedHash = hash.trim()
        if (parsed == null || parsed <= 0 || cleanedHash.length != 32 || !cleanedHash.all { it.isLetterOrDigit() }) {
            _message.value = "API ID должен быть числом, API Hash — 32 символа."
            return
        }
        try {
            prefs.edit()
                .putString("api_id_encrypted", CredentialVault.encrypt(parsed.toString()))
                .putString("api_hash_encrypted", CredentialVault.encrypt(cleanedHash))
                .remove("api_id").remove("api_hash").apply()
        } catch (e: Exception) {
            _message.value = "Не удалось сохранить API-данные защищённо: ${e.message ?: "ошибка Keystore"}"
            return
        }
        connect()
    }

    fun connect() {
        if (client != null) return
        val id = apiId.toIntOrNull() ?: run { _message.value = "Сначала укажите API ID и API Hash."; return }
        val hash = apiHash.takeIf { it.length == 32 } ?: run { _message.value = "API Hash не заполнен или некорректен."; return }
        try {
            System.loadLibrary("tdjni")
            val td = TdClient(
                filesDir = context.filesDir.absolutePath + "/social-radar-tdlib",
                verbosityLevel = 1,
                apiId = id,
                apiHash = hash
            )
            client = td
            scope.launch {
                td.updates.collect { update ->
                    if (update is TdApi.UpdateAuthorizationState) {
                        when (val state = update.authorizationState) {
                            is TdApi.AuthorizationStateWaitPhoneNumber -> _auth.value = RadarAuth("Введите номер телефона", phoneRequired = true)
                            is TdApi.AuthorizationStateWaitCode -> _auth.value = RadarAuth("Код Telegram отправлен", codeRequired = true)
                            is TdApi.AuthorizationStateWaitPassword -> _auth.value = RadarAuth("Нужен пароль двухэтапной проверки", passwordRequired = true)
                            is TdApi.AuthorizationStateWaitEmailAddress -> _auth.value = RadarAuth("Введите email", emailRequired = true)
                            is TdApi.AuthorizationStateWaitEmailCode -> _auth.value = RadarAuth("Код отправлен на email", emailCodeRequired = true)
                            is TdApi.AuthorizationStateWaitRegistration -> _auth.value = RadarAuth("Регистрация Telegram", registrationRequired = true)
                            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> _auth.value = RadarAuth("Подтвердите вход на другом устройстве", otherDeviceLink = state.link)
                            is TdApi.AuthorizationStateReady -> {
                                _auth.value = RadarAuth("Подключено", ready = true)
                                _message.value = "Telegram подключён. Можно искать публичные сообщества."
                            }
                            is TdApi.AuthorizationStateLoggingOut -> _auth.value = RadarAuth("Выход…")
                            is TdApi.AuthorizationStateClosing -> _auth.value = RadarAuth("Закрытие…")
                            is TdApi.AuthorizationStateClosed -> {
                                _auth.value = RadarAuth("Сессия закрыта")
                                client = null
                            }
                            else -> _auth.value = RadarAuth("Подготовка TDLib…")
                        }
                    }
                }
            }
            td.init()
            _auth.value = RadarAuth("Инициализация TDLib…")
            _message.value = "TDLib запущена. Ожидание авторизации."
        } catch (e: Exception) {
            client = null
            _auth.value = RadarAuth("Ошибка запуска TDLib")
            _message.value = "Не удалось запустить TDLib: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    fun submitPhone(phone: String) {
        val normalized = phone.trim().replace(" ", "").replace("-", "")
        if (!normalized.startsWith("+") || normalized.drop(1).any { !it.isDigit() } || normalized.length < 8) {
            _message.value = "Введите номер телефона в международном формате, например +49…"
            return
        }
        request(TdApi.SetAuthenticationPhoneNumber(normalized, null))
    }
    fun submitCode(code: String) {
        val normalized = code.trim()
        if (normalized.isBlank()) { _message.value = "Введите код из Telegram."; return }
        request(TdApi.CheckAuthenticationCode(normalized))
    }
    fun submitPassword(password: String) {
        if (password.isBlank()) { _message.value = "Введите пароль 2FA."; return }
        request(TdApi.CheckAuthenticationPassword(password))
    }
    fun submitEmail(email: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { _message.value = "Введите корректный email."; return }
        request(TdApi.SetAuthenticationEmailAddress(email.trim()))
    }
    fun submitEmailCode(code: String) {
        if (code.isBlank()) { _message.value = "Введите email-код."; return }
        request(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(code.trim())))
    }
    fun register(firstName: String) {
        val name = firstName.trim()
        if (name.isBlank()) { _message.value = "Введите имя для регистрации."; return }
        request(TdApi.RegisterUser(name, "", false))
    }

    private fun request(fn: TdApi.Function<*>) {
        val td = client ?: run { _message.value = "TDLib не запущена."; return }
        scope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                td.send(fn as TdApi.Function<TdApi.Object>)
                _message.value = "Запрос отправлен."
            } catch (e: Exception) {
                _message.value = safeError(e)
            }
        }
    }

    /**
     * Telegram's public-chat discovery endpoint searches names and usernames only.
     * It is not a full message/content search. Queries shorter than 5 characters
     * are skipped because Telegram returns no useful public-chat results.
     */
    fun search(raw: String) {
        val td = client
        if (td == null || !_auth.value.ready) {
            _message.value = "Подключите Telegram и завершите авторизацию."
            return
        }
        val q = raw.trim()
        if (q.isBlank()) return
        if (q.length < 5) {
            _results.value = emptyList()
            _message.value = "Для поиска публичных групп и каналов нужно минимум 5 символов."
            return
        }
        if (_busy.value) return
        _busy.value = true
        _results.value = emptyList()
        _message.value = "Поиск публичных групп и каналов по названиям и username…"
        scope.launch {
            try {
                val planned = planner.expand(SearchRequest(q, languageCodes = setOf("ru", "en"), maxExpansionQueries = 24))
                val variants = linkedSetOf<String>()
                variants += q
                q.split(Regex("[^\\p{L}\\p{N}]+"))
                    .filter { it.length >= 5 }
                    .take(4)
                    .forEach { variants += it }
                planned.filter { it.length >= 5 }.forEach { variants += it }
                val safeVariants = variants.map(String::trim)
                    .filter { it.length >= 5 && it.length <= 64 }
                    .distinctBy { it.lowercase(Locale.ROOT) }
                    .take(8)
                val merged = linkedMapOf<Long, RadarChat>()
                var searched = 0
                var failures = 0
                for (variant in safeVariants) {
                    try {
                        val response = td.send(TdApi.SearchPublicChats(variant, null))
                        searched++
                        response.chatIds.take(20).forEach { chatId ->
                            if (chatId in merged) return@forEach
                            val chat = try { td.send(TdApi.GetChat(chatId)) } catch (_: Exception) { return@forEach }
                            val type = chat.type
                            var kind = "Публичный чат"
                            var username = ""
                            if (type is TdApi.ChatTypeSupergroup) {
                                try {
                                    val supergroup = td.send(TdApi.GetSupergroup(type.supergroupId))
                                    username = supergroup.usernames?.activeUsernames?.firstOrNull().orEmpty()
                                    kind = if (supergroup.isChannel) "Канал" else "Группа"
                                } catch (_: Exception) {
                                    kind = "Группа / канал"
                                }
                            } else if (type is TdApi.ChatTypeBasicGroup) {
                                kind = "Группа"
                            }
                            merged[chat.id] = RadarChat(chat.id, chat.title, kind, username)
                        }
                    } catch (_: Exception) {
                        failures++
                    }
                }
                val terms = q.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.length >= 3 }
                _results.value = merged.values.sortedWith(
                    compareByDescending<RadarChat> { chat ->
                        val title = chat.title.lowercase(Locale.ROOT)
                        val user = chat.username.lowercase(Locale.ROOT)
                        (terms.count { title.contains(it) } * 4) +
                            (terms.count { user.contains(it) } * 3) +
                            (if (title.contains(q.lowercase(Locale.ROOT)) || user.contains(q.lowercase(Locale.ROOT)) ) 10 else 0)
                    }.thenBy { it.title.lowercase(Locale.ROOT) }
                ).take(100)
                _message.value = if (searched == 0 && failures > 0) {
                    "Telegram не принял запросы. Проверьте соединение и повторите позже."
                } else {
                    "Проверено запросов: $searched · найдено: ${_results.value.size}. Поиск ограничен публичными названиями и username."
                }
            } catch (e: Exception) {
                _message.value = "Ошибка поиска: ${safeError(e)}"
            } finally {
                _busy.value = false
            }
        }
    }

    private fun safeError(e: Exception): String {
        val raw = e.message ?: e.javaClass.simpleName
        return raw.take(220)
    }

    fun close() {
        client?.close()
        client = null
        scope.cancel()
    }

    fun logout() {
        val td = client ?: return
        scope.launch {
            try { td.send(TdApi.LogOut()) } catch (_: Exception) {}
            td.close()
            client = null
            _auth.value = RadarAuth("Вы вышли из Telegram")
            _results.value = emptyList()
            _message.value = "Сессия Telegram закрыта."
        }
    }
}


/** AES-GCM encrypted storage backed by the Android Keystore. */
private object CredentialVault {
    private const val KEY_ALIAS = "social-radar-api-credentials"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private val lock = Any()

    private fun key(): SecretKey = synchronized(lock) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey) ?: run {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generator.generateKey()
        }
    }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + "." +
            Base64.encodeToString(cipherText, Base64.NO_WRAP)
    }

    fun decrypt(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return try {
            val parts = value.split(".", limit = 2)
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }
}
