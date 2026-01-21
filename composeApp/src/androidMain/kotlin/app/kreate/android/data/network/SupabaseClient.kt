package app.kreate.android.data.network

import app.kreate.android.BuildConfig

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.supabaseurl,
        supabaseKey = BuildConfig.supabasekey
    ) {
        install(GoTrue)
    }
}