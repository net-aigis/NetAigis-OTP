package de.wittmer.app.otp.data.util

import androidx.room.TypeConverter
import de.wittmer.app.otp.data.EncryptionType
import de.wittmer.app.otp.data.OtpTokenType

class Converters {
    @TypeConverter
    fun fromOtpTokenType(value: OtpTokenType): String {
        return value.name
    }

    @TypeConverter
    fun toOtpTokenType(value: String): OtpTokenType {
        return OtpTokenType.valueOf(value)
    }

    @TypeConverter
    fun fromEncryptionType(value: EncryptionType): String {
        return value.name
    }

    @TypeConverter
    fun toEncryptionType(value: String): EncryptionType {
        return EncryptionType.valueOf(value)
    }
}
