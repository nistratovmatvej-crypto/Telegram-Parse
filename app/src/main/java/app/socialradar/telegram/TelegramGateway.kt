package app.socialradar.telegram

/**
 * Boundary around TDLib. The interface keeps the app independent from the native
 * library while the Android/ABI build is prepared.
 */
interface TelegramGateway {
    fun observeAuthorizationState(observer: (TelegramAuthorizationState) -> Unit)
    fun setApiCredentials(apiId: Int, apiHash: String)
    fun requestPhoneNumber(phoneNumber: String)
    fun submitLoginCode(code: String)
    fun submitTwoStepPassword(password: CharArray)
    fun logOut()
    fun close()
}

sealed interface TelegramAuthorizationState {
    data object NotConfigured : TelegramAuthorizationState
    data object WaitingForParameters : TelegramAuthorizationState
    data object WaitingForEncryptionKey : TelegramAuthorizationState
    data object WaitingForPhoneNumber : TelegramAuthorizationState
    data object WaitingForCode : TelegramAuthorizationState
    data object WaitingForPassword : TelegramAuthorizationState
    data object Ready : TelegramAuthorizationState
    data object LoggingOut : TelegramAuthorizationState
    data class Failed(val code: Int?, val safeMessage: String) : TelegramAuthorizationState
    data object Closed : TelegramAuthorizationState
}
