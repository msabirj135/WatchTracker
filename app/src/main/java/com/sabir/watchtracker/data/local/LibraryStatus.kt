package com.sabir.watchtracker.data.local

enum class LibraryStatus(
    val displayName: String
) {
    PLAN_TO_WATCH("Plan to Watch"),
    WATCHING("Watching"),
    COMPLETED("Completed"),
    DROPPED("Dropped")
}
