package it.fast4x.piped.models

import io.ktor.http.Url

@JvmInline
value class Session internal constructor(private val value: Pair<Url, String>) {
    val apiBaseUrl get() = value.first
    val token get() = value.second
}

infix fun Url.authenticatedWith(token: String) = Session(this to token)
infix fun String.authenticatedWith(token: String) = Url(this) authenticatedWith token
