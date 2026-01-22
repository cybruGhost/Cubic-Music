package app.kreate.android.data.network

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import it.fast4x.innertube.models.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SupabaseAuth {
}

sealed interface AuthResponse {
    data object Success : AuthResponse
    data class Error(val message: String) : AuthResponse
}

class AuthManager(
    private val context: Context
) {
    private val supabase = createSupabaseClient(
        supabaseUrl = "https://mbtfunapyvfiptozwfff.supabase.co",
        supabaseKey = "sb_publishable_E7nsl9NgOR89kV4UAD6Nhg_XhFOF0w7"
    ) {
        install(Auth)
    }

    fun signupWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow {
        try {
            supabase.auth.signUpWith(Email) {
                email = emailValue
                password = passwordValue
            }
        } catch (e: Exception) {
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }

    fun signinWithEmail(emailValue: String, passwordValue: String): Flow<AuthResponse> = flow {
        try {
            supabase.auth.signInWith(Email) {
                email = emailValue
                password = passwordValue
            }
        } catch (e: Exception) {
            emit(AuthResponse.Error(e.localizedMessage))
        }
    }
}