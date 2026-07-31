package com.sabir.watchtracker.data.local

import androidx.room.TypeConverter

class LibraryTypeConverters {

    @TypeConverter
    fun libraryStatusToString(
        status: LibraryStatus
    ): String {
        return status.name
    }

    @TypeConverter
    fun stringToLibraryStatus(
        value: String
    ): LibraryStatus {
        return runCatching {
            LibraryStatus.valueOf(value)
        }.getOrDefault(
            LibraryStatus.PLAN_TO_WATCH
        )
    }
}