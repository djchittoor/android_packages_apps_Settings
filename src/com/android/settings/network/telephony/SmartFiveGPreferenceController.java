package com.android.settings.network.telephony;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.PersistableBundle;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;
import android.util.Log;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;
import com.android.settings.R;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;
import java.util.Objects;
public class SmartFiveGPreferenceController extends TelephonyTogglePreferenceController implements LifecycleObserver, OnStart, OnStop {
    private static final String HIDE_SMART_5G = "hide_smart_5g_bool";
    private static final String LOG_5G_Event_SMART_5G = "smart_5g";
    private static final String TAG = "SmartFiveGPreferenceController";
    private AllowedNetworkTypesListener mAllowedNetworkTypesListener;
    Integer mCallState;
    private final BroadcastReceiver mDefaultDataChangedReceiver;
    private PhoneTelephonyCallback mPhoneTelephonyCallback;
    Preference mPreference;
    private boolean mReceiverRegistered;
    private TelephonyManager mTelephonyManager;

    public SmartFiveGPreferenceController(Context context, String str) {
        super(context, str);
        this.mReceiverRegistered = false;
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context2, Intent intent) {
                if (mPreference != null) {
                    Log.d(TAG, "DDS is changed");
                    updateState(mPreference);
                }
            }
        };
        mDefaultDataChangedReceiver = broadcastReceiver;
        mTelephonyManager = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        if (!mReceiverRegistered) {
            mContext.registerReceiver(broadcastReceiver, new IntentFilter(
                    "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"));
            mReceiverRegistered = true;
        }
    }

    public SmartFiveGPreferenceController init(int subId) {
        if (mPhoneTelephonyCallback == null) {
            mPhoneTelephonyCallback = new PhoneTelephonyCallback();
        }
        if (mAllowedNetworkTypesListener == null) {
            AllowedNetworkTypesListener allowedNetworkTypesListener =
                    new AllowedNetworkTypesListener(mContext.getMainExecutor());
            mAllowedNetworkTypesListener = allowedNetworkTypesListener;
            allowedNetworkTypesListener.setAllowedNetworkTypesListener(
                    () -> updatePreference());
        }
        if (!SubscriptionManager.isValidSubscriptionId(mSubId) || mSubId != subId) {
            mSubId = subId;
            if (mTelephonyManager == null) {
                mTelephonyManager = (TelephonyManager) mContext.getSystemService(TelephonyManager.class);
            }
            if (SubscriptionManager.isValidSubscriptionId(subId)) {
                mTelephonyManager = mTelephonyManager.createForSubscriptionId(subId);
            }
            return this;
        }
        return this;
    }

    public void updatePreference() {
        Preference preference = mPreference;
        if (preference != null) {
            updateState(preference);
        }
    }

    @Override // com.android.settings.network.telephony.TelephonyTogglePreferenceController, com.android.settings.network.telephony.TelephonyAvailabilityCallback
    public int getAvailabilityStatus(int i) {
        return isSwitchVisible(mContext, mSubId) ? 0 : 2;
    }

    @Override // com.android.settings.core.TogglePreferenceController, com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        PhoneTelephonyCallback phoneTelephonyCallback = mPhoneTelephonyCallback;
        if (phoneTelephonyCallback != null) {
            phoneTelephonyCallback.register(mSubId, mTelephonyManager);
        }
        AllowedNetworkTypesListener allowedNetworkTypesListener = mAllowedNetworkTypesListener;
        if (allowedNetworkTypesListener != null) {
            allowedNetworkTypesListener.register(mContext, mSubId);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        if (mReceiverRegistered) {
            mContext.unregisterReceiver(mDefaultDataChangedReceiver);
            mReceiverRegistered = false;
        }
        PhoneTelephonyCallback phoneTelephonyCallback = mPhoneTelephonyCallback;
        if (phoneTelephonyCallback != null) {
            phoneTelephonyCallback.unregister();
        }
        AllowedNetworkTypesListener allowedNetworkTypesListener = mAllowedNetworkTypesListener;
        if (allowedNetworkTypesListener != null) {
            allowedNetworkTypesListener.unregister(mContext, mSubId);
        }
    }

    @Override // com.android.settings.core.TogglePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void updateState(Preference preference) {
        super.updateState(preference);
        SwitchPreference switchPreference = (SwitchPreference) preference;
        switchPreference.setVisible(isAvailable());
        boolean z = false;
        if (isChecked()) {
            Log.d(TAG, "updateState check");
            switchPreference.setChecked(true);
        } else {
            switchPreference.setChecked(false);
        }
        int preferredNetworkMode = getPreferredNetworkMode();
        boolean isSwitchVisible = isSwitchVisible(mContext, mSubId);
        if (isCallStateIdle() && preferredNetworkMode > 22 && isSwitchVisible) {
            z = true;
        }
        switchPreference.setEnabled(z);
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        if (!SubscriptionManager.isValidSubscriptionId(mSubId)) {
            return false;
        }
        if (!z) {
        Log.d(TAG, "showDisableSmart5gDialog");
        } else {
            MobileNetworkUtils.setSmart5gMode(mContext, 1, mSubId);
           // MobileNetworkUtils.log5GEvent(mContext, LOG_5G_Event_SMART_5G, 1);
        }
        return true;
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return true;
    }

    boolean isCallStateIdle() {
        Integer num = mCallState;
        boolean z = num == null || num.intValue() == 0;
        Log.d(TAG, "isCallStateIdle:" + z);
        return z;
    }

    private int getPreferredNetworkMode() {
        return MobileNetworkUtils.getNetworkTypeFromRaf((int) mTelephonyManager.getAllowedNetworkTypesForReason(0));
    }

    /* loaded from: classes2.dex */
    public class PhoneTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        private TelephonyManager mLocalTelephonyManager;

        private PhoneTelephonyCallback() {
        }

        @Override // android.telephony.TelephonyCallback.CallStateListener
        public void onCallStateChanged(int i) {
            mCallState = Integer.valueOf(i);
            SmartFiveGPreferenceController smartFiveGPreferenceController = SmartFiveGPreferenceController.this;
            updateState(mPreference);
        }

        public void register(int i, TelephonyManager telephonyManager) {
            mLocalTelephonyManager = telephonyManager;
            mCallState = Integer.valueOf(telephonyManager.getCallState(i));
            Handler uiThreadHandler = ThreadUtils.getUiThreadHandler();
            Objects.requireNonNull(uiThreadHandler);
            mLocalTelephonyManager.registerTelephonyCallback(new HandlerExecutor(uiThreadHandler), this);
        }

        public void unregister() {
            mCallState = null;
            mLocalTelephonyManager.unregisterTelephonyCallback(this);
        }
    }

    public static boolean isSwitchVisible(Context context, int i) {
            return true;
    }
}
