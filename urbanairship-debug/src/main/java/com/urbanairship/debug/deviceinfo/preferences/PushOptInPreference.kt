package com.urbanairship.debug.deviceinfo.preferences

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import androidx.preference.Preference
import android.util.AttributeSet

import com.urbanairship.UAirship
import com.urbanairship.debug.R
import com.urbanairship.push.RegistrationListener

import android.os.Looper
import android.os.Handler

class PushOptInPreference : Preference {
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {}

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}

    private val registrationListener = object : RegistrationListener {
        override fun onChannelCreated(channelId: String) {}

        override fun onChannelUpdated(channelId: String) {}

        override fun onPushTokenUpdated(token: String) { notifyChannelUpdate() }
    }

    override fun getSummary(): CharSequence {
        return if (UAirship.shared().pushManager.isOptIn) context.getString(R.string.ua_opted_in) else context.getString(R.string.ua_opted_out)
    }

    override fun onAttached() {
        super.onAttached()
        UAirship.shared().pushManager.addRegistrationListener(registrationListener)
    }

    override fun onDetached() {
        super.onDetached()
        UAirship.shared().pushManager.removeRegistrationListener(registrationListener)
    }

    private fun notifyChannelUpdate() {
        val handler = Handler(Looper.getMainLooper())
        handler.post(Runnable { notifyChanged() })
    }
}
