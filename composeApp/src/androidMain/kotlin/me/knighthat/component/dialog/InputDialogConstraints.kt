package me.knighthat.component.dialog

object InputDialogConstraints {

    /**
     * This REGEX pattern allows all characters
     */
    const val ALL = ".*";

    /**
     * This REGEX pattern only allows ASCII characters and numbers
     */
    const val NO_SPECIAL_CHARS = "^[a-zA-Z0-9 ]+\$"

    const val ANDROID_FILE_PATH = "^\\/[\\w\\-]*(?:\\/[\\w\\-\\.]+)*\$"

    /**
     * Only whole number (integer) digits are allowed
     */
    const val ONLY_INTEGERS = "^\\d+\$"

    /**
     * Matches most URLs
     */
    const val URL = "^https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)$"
}