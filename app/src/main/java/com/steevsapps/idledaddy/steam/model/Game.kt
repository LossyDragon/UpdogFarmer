package com.steevsapps.idledaddy.steam.model

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.google.gson.annotations.SerializedName

class Game : Comparable<Game>, Parcelable {
    @SerializedName("appid")
    var appId: Int

    @SerializedName("name")
    var name: String = ""

    @SerializedName("img_logo_url")
    var iconUrl: String = ""

    @SerializedName("playtime_forever")
    var hoursPlayed: Float

    @SerializedName("drops_remaining")
    var dropsRemaining: Int

    constructor(appId: Int, name: String, hoursPlayed: Float, dropsRemaining: Int) {
        this.appId = appId
        this.name = name
        this.iconUrl = "http://cdn.akamai.steamstatic.com/steam/apps/$appId/header_292x136.jpg"
        this.hoursPlayed = hoursPlayed
        this.dropsRemaining = dropsRemaining
    }

    private constructor(parcel: Parcel) {
        appId = parcel.readInt()
        name = parcel.readString().orEmpty()
        iconUrl = parcel.readString().orEmpty()
        hoursPlayed = parcel.readFloat()
        dropsRemaining = parcel.readInt()
    }

    override fun compareTo(other: Game): Int {
        if (hoursPlayed == other.hoursPlayed) {
            return 0
        }
        return if (hoursPlayed < other.hoursPlayed) -1 else 1
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other !is Game) {
            return false
        }
        return other.appId == appId
    }

    override fun hashCode(): Int {
        // Start with a non-zero constant. Prime is preferred
        var result = 17
        // Include a hash for each field
        result = 31 * result + appId
        return result
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeInt(appId)
        parcel.writeString(name)
        parcel.writeString(iconUrl)
        parcel.writeFloat(hoursPlayed)
        parcel.writeInt(dropsRemaining)
    }

    companion object {
        @JvmField
        val CREATOR: Creator<Game?> = object : Creator<Game?> {
            override fun createFromParcel(parcel: Parcel): Game = Game(parcel)
            override fun newArray(i: Int): Array<Game?> = arrayOfNulls(i)
        }
    }
}
