package com.nothing.settings.network.telephony;

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
import com.android.settings.R$string;
import com.android.settings.R$style;
import com.android.settings.network.AllowedNetworkTypesListener;
import com.android.settings.network.telephony.MobileNetworkUtils;
import com.android.settings.network.telephony.TelephonyTogglePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnStart;
import com.android.settingslib.core.lifecycle.events.OnStop;
import com.android.settingslib.utils.ThreadUtils;
import java.util.Objects;
/* loaded from: classes2.dex */
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

    @Override // com.android.settings.network.telephony.TelephonyTogglePreferenceController, com.android.settings.core.TogglePreferenceController, com.android.settings.slices.Sliceable
    public /* bridge */ /* synthetic */ Class getBackgroundWorkerClass() {
        return super.getBackgroundWorkerClass();
    }

    @Override // com.android.settings.network.telephony.TelephonyTogglePreferenceController, com.android.settings.core.TogglePreferenceController, com.android.settings.slices.Sliceable
    public /* bridge */ /* synthetic */ IntentFilter getIntentFilter() {
        return super.getIntentFilter();
    }

    @Override // com.android.settings.network.telephony.TelephonyTogglePreferenceController, com.android.settings.core.TogglePreferenceController, com.android.settings.slices.Sliceable
    public /* bridge */ /* synthetic */ boolean hasAsyncUpdate() {
        return super.hasAsyncUpdate();
    }

    @Override // com.android.settings.network.telephony.TelephonyTogglePreferenceController, com.android.settings.core.TogglePreferenceController, com.android.settings.slices.Sliceable
    public /* bridge */ /* synthetic */ boolean useDynamicSliceSummary() {
        return super.useDynamicSliceSummary();
    }

    public SmartFiveGPreferenceController(Context context, String str) {
        super(context, str);
        this.mReceiverRegistered = false;
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { // from class: com.nothing.settings.network.telephony.SmartFiveGPreferenceController.1
            @Override // android.content.BroadcastReceiver
            public void onReceive(Context context2, Intent intent) {
                if (SmartFiveGPreferenceController.this.mPreference != null) {
                    Log.d(SmartFiveGPreferenceController.TAG, "DDS is changed");
                    SmartFiveGPreferenceController smartFiveGPreferenceController = SmartFiveGPreferenceController.this;
                    smartFiveGPreferenceController.updateState(smartFiveGPreferenceController.mPreference);
                }
            }
        };
        this.mDefaultDataChangedReceiver = broadcastReceiver;
        this.mTelephonyManager = (TelephonyManager) context.getSystemService(TelephonyManager.class);
        if (!this.mReceiverRegistered) {
            this.mContext.registerReceiver(broadcastReceiver, new IntentFilter("android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"));
            this.mReceiverRegistered = true;
        }
    }

    public SmartFiveGPreferenceController init(int i) {
        if (this.mPhoneTelephonyCallback == null) {
            this.mPhoneTelephonyCallback = new PhoneTelephonyCallback();
        }
        if (this.mAllowedNetworkTypesListener == null) {
            AllowedNetworkTypesListener allowedNetworkTypesListener = new AllowedNetworkTypesListener(this.mContext.getMainExecutor());
            this.mAllowedNetworkTypesListener = allowedNetworkTypesListener;
            allowedNetworkTypesListener.setAllowedNetworkTypesListener(new AllowedNetworkTypesListener.OnAllowedNetworkTypesListener() { // from class: com.nothing.settings.network.telephony.SmartFiveGPreferenceController$$ExternalSyntheticLambda0
                @Override // com.android.settings.network.AllowedNetworkTypesListener.OnAllowedNetworkTypesListener
                public final void onAllowedNetworkTypesChanged() {
                    SmartFiveGPreferenceController.this.lambda$init$0();
                }
            });
        }
        if (!SubscriptionManager.isValidSubscriptionId(this.mSubId) || this.mSubId != i) {
            this.mSubId = i;
            if (this.mTelephonyManager == null) {
                this.mTelephonyManager = (TelephonyManager) this.mContext.getSystemService(TelephonyManager.class);
            }
            if (SubscriptionManager.isValidSubscriptionId(i)) {
                this.mTelephonyManager = this.mTelephonyManager.createForSubscriptionId(i);
            }
            return this;
        }
        return this;
    }

    /* renamed from: updatePreference */
    public void lambda$init$0() {
        Preference preference = this.mPreference;
        if (preference != null) {
            updateState(preference);
        }
    }

    @Override // com.android.settings.network.telephony.TelephonyTogglePreferenceController, com.android.settings.network.telephony.TelephonyAvailabilityCallback
    public int getAvailabilityStatus(int i) {
        return isSwitchVisible(this.mContext, this.mSubId) ? 0 : 2;
    }

    @Override // com.android.settings.core.TogglePreferenceController, com.android.settings.core.BasePreferenceController, com.android.settingslib.core.AbstractPreferenceController
    public void displayPreference(PreferenceScreen preferenceScreen) {
        super.displayPreference(preferenceScreen);
        this.mPreference = preferenceScreen.findPreference(getPreferenceKey());
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStart
    public void onStart() {
        PhoneTelephonyCallback phoneTelephonyCallback = this.mPhoneTelephonyCallback;
        if (phoneTelephonyCallback != null) {
            phoneTelephonyCallback.register(this.mSubId, this.mTelephonyManager);
        }
        AllowedNetworkTypesListener allowedNetworkTypesListener = this.mAllowedNetworkTypesListener;
        if (allowedNetworkTypesListener != null) {
            allowedNetworkTypesListener.register(this.mContext, this.mSubId);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.events.OnStop
    public void onStop() {
        if (this.mReceiverRegistered) {
            this.mContext.unregisterReceiver(this.mDefaultDataChangedReceiver);
            this.mReceiverRegistered = false;
        }
        PhoneTelephonyCallback phoneTelephonyCallback = this.mPhoneTelephonyCallback;
        if (phoneTelephonyCallback != null) {
            phoneTelephonyCallback.unregister();
        }
        AllowedNetworkTypesListener allowedNetworkTypesListener = this.mAllowedNetworkTypesListener;
        if (allowedNetworkTypesListener != null) {
            allowedNetworkTypesListener.unregister(this.mContext, this.mSubId);
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
        boolean isSwitchVisible = isSwitchVisible(this.mContext, this.mSubId);
        if (isCallStateIdle() && preferredNetworkMode > 22 && isSwitchVisible) {
            z = true;
        }
        switchPreference.setEnabled(z);
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean setChecked(boolean z) {
        if (!SubscriptionManager.isValidSubscriptionId(this.mSubId)) {
            return false;
        }
        if (!z) {
            showDisableSmart5gDialog();
        } else {
            MobileNetworkUtils.setSmart5gMode(this.mContext, 1, this.mSubId);
            MobileNetworkUtils.log5GEvent(this.mContext, LOG_5G_Event_SMART_5G, 1);
        }
        return true;
    }

    private void showDisableSmart5gDialog() {
        Log.d(TAG, "showDisableSmart5gDialog");
        AlertDialog.Builder builder = new AlertDialog.Builder(this.mContext, R$style.TelephonyToggleAlertDialog);
        DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() { // from class: com.nothing.settings.network.telephony.SmartFiveGPreferenceController.2
            @Override // android.content.DialogInterface.OnClickListener
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1) {
                    MobileNetworkUtils.setSmart5gMode(((AbstractPreferenceController) SmartFiveGPreferenceController.this).mContext, 0, ((TelephonyTogglePreferenceController) SmartFiveGPreferenceController.this).mSubId);
                    MobileNetworkUtils.log5GEvent(((AbstractPreferenceController) SmartFiveGPreferenceController.this).mContext, SmartFiveGPreferenceController.LOG_5G_Event_SMART_5G, 0);
                    return;
                }
                SmartFiveGPreferenceController.this.lambda$init$0();
            }
        };
        builder.setMessage(R$string.nt_turn_off_smart5g_dialog_title).setNegativeButton(this.mContext.getResources().getString(R$string.cancel), onClickListener).setPositiveButton(this.mContext.getResources().getString(R$string.condition_turn_off), onClickListener).create().show();
    }

    @Override // com.android.settings.core.TogglePreferenceController
    public boolean isChecked() {
        return MobileNetworkUtils.getSmart5gMode(this.mContext, this.mSubId) == 1;
    }

    boolean isCallStateIdle() {
        Integer num = this.mCallState;
        boolean z = num == null || num.intValue() == 0;
        Log.d(TAG, "isCallStateIdle:" + z);
        return z;
    }

    private int getPreferredNetworkMode() {
        return MobileNetworkUtils.getNetworkTypeFromRaf((int) this.mTelephonyManager.getAllowedNetworkTypesForReason(0));
    }

    /* loaded from: classes2.dex */
    public class PhoneTelephonyCallback extends TelephonyCallback implements TelephonyCallback.CallStateListener {
        private TelephonyManager mLocalTelephonyManager;

        private PhoneTelephonyCallback() {
        }

        @Override // android.telephony.TelephonyCallback.CallStateListener
        public void onCallStateChanged(int i) {
            SmartFiveGPreferenceController.this.mCallState = Integer.valueOf(i);
            SmartFiveGPreferenceController smartFiveGPreferenceController = SmartFiveGPreferenceController.this;
            smartFiveGPreferenceController.updateState(smartFiveGPreferenceController.mPreference);
        }

        public void register(int i, TelephonyManager telephonyManager) {
            this.mLocalTelephonyManager = telephonyManager;
            SmartFiveGPreferenceController.this.mCallState = Integer.valueOf(telephonyManager.getCallState(i));
            TelephonyManager telephonyManager2 = this.mLocalTelephonyManager;
            Handler uiThreadHandler = ThreadUtils.getUiThreadHandler();
            Objects.requireNonNull(uiThreadHandler);
            telephonyManager2.registerTelephonyCallback(new HandlerExecutor(uiThreadHandler), this);
        }

        public void unregister() {
            SmartFiveGPreferenceController.this.mCallState = null;
            this.mLocalTelephonyManager.unregisterTelephonyCallback(this);
        }
    }

    public static boolean isSwitchVisible(Context context, int i) {
        PersistableBundle configForSubId;
        CarrierConfigManager carrierConfigManager = (CarrierConfigManager) context.getSystemService(CarrierConfigManager.class);
        if (carrierConfigManager == null || (configForSubId = carrierConfigManager.getConfigForSubId(i)) == null) {
            return true;
        }
        return !configForSubId.getBoolean(HIDE_SMART_5G);
    }
}
