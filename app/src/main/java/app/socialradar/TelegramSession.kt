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
import org.drinkless.tdlib.TdApi

data class RadarChat(val id: Long, val title: String, val type: String)
data class RadarAuth(val label: String = "Не подключено", val ready: Boolean = false, val codeRequired: Boolean = false, val passwordRequired: Boolean = false, val phoneRequired: Boolean = false, val emailRequired: Boolean = false, val emailCodeRequired: Boolean = false, val registrationRequired: Boolean = false, val otherDeviceLink: String = "")

class TelegramSession(private val context: Context) {
    private val prefs = context.getSharedPreferences("telegram_session_config", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var client: TdClient? = null
    private val _auth = MutableStateFlow(RadarAuth())
    val auth: StateFlow<RadarAuth> = _auth
    private val _results = MutableStateFlow<List<RadarChat>>(emptyList())
    val results: StateFlow<List<RadarChat>> = _results
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy
    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message
    val apiId: String get() = prefs.getString("api_id", "") ?: ""
    val apiHash: String get() = prefs.getString("api_hash", "") ?: ""

    fun configure(id: String, hash: String) {
        val parsed = id.trim().toIntOrNull()
        if (parsed == null || hash.trim().length < 20) { _message.value = "Проверьте API ID и API Hash."; return }
        prefs.edit().putString("api_id", parsed.toString()).putString("api_hash", hash.trim()).apply()
        connect()
    }

    fun connect() {
        if (client != null) return
        val id = apiId.toIntOrNull() ?: run { _message.value = "Сначала укажите API ID и API Hash."; return }
        val hash = apiHash.takeIf { it.length >= 20 } ?: run { _message.value = "API Hash не заполнен."; return }
        try {
            System.loadLibrary("tdjni")
            val td = TdClient(context.filesDir.absolutePath + "/social-radar-tdlib", 1, id, hash)
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
                            is TdApi.AuthorizationStateReady -> { _auth.value = RadarAuth("Подключено", ready = true); _message.value = "Telegram подключён." }
                            is TdApi.AuthorizationStateLoggingOut -> _auth.value = RadarAuth("Выход…")
                            is TdApi.AuthorizationStateClosing -> _auth.value = RadarAuth("Закрытие…")
                            is TdApi.AuthorizationStateClosed -> { _auth.value = RadarAuth("Сессия закрыта"); client = null }
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

    fun submitPhone(phone: String) { request(TdApi.SetAuthenticationPhoneNumber(phone.trim(), null)) }
    fun submitCode(code: String) { request(TdApi.CheckAuthenticationCode(code.trim())) }
    fun submitPassword(password: String) { request(TdApi.CheckAuthenticationPassword(password)) }
    fun submitEmail(email: String) { request(TdApi.SetAuthenticationEmailAddress(email.trim())) }
    fun submitEmailCode(code: String) { request(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(code.trim()))) }
    fun register(firstName: String) { request(TdApi.RegisterUser(firstName.trim(), "", false)) }

    private fun request(fn: TdApi.Function<*>) {
        val td = client ?: run { _message.value = "TDLib не запущена."; return }
        scope.launch {
            try {
                @Suppress("UNCHECKED_CAST")
                td.send(fn as TdApi.Function<TdApi.Object>)
                _message.value = "Запрос отправлен."
            } catch (e: Exception) { _message.value = e.message ?: "Ошибка авторизации." }
        }
    }

    fun search(raw: String) {
        val td = client
        if (td == null || !_auth.value.ready) { _message.value = "Подключите Telegram и завершите авторизацию."; return }
        val q = raw.trim()
        if (q.isBlank()) return
        _busy.value = true
        _results.value = emptyList()
        _message.value = "Ищем публичные чаты…"
        scope.launch {
            try {
                val variants = linkedSetOf(q)
                q.split(Regex("[\\s,;]+")).filter { it.length >= 3 }.take(4).forEach { variants.add(it) }
                val merged = linkedMapOf<Long, RadarChat>()
                for (variant in variants.take(5)) {
                    val response = td.send(TdApi.SearchPublicChats(variant, null))
                    response.chats.forEach { chat ->
                        val kind = when (chat.type) {
                            is TdApi.ChatTypeSupergroup -> "Группа / канал"
                            is TdApi.ChatTypeBasicGroup -> "Группа"
                            else -> "Публичный чат"
                        }
                        merged[chat.id] = RadarChat(chat.id, chat.title, kind)
                    }
                }
                _results.value = merged.values.sortedByDescending { chat -> q.split(Regex("[\\s,;]+")).count { it.length >= 2 && chat.title.contains(it, true) } }.take(100)
                _message.value = "Найдено публичных чатов: ${_results.value.size}"
            } catch (e: Exception) { _message.value = "Ошибка поиска: ${e.message ?: "неизвестная ошибка"}" }
            finally { _busy.value = false }
        }
    }

    fun logout() {
        val td = client ?: return
        scope.launch {
            try { td.send(TdApi.LogOut()) } catch (_: Exception) {}
            td.close()
            client = null
            _auth.value = RadarAuth("Вы вышли из Telegram")
            _results.value = emptyList()
        }
    }
}
