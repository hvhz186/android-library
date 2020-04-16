/* Copyright Airship and Contributors */

package com.urbanairship.aaid;

import android.content.Context;

import com.urbanairship.PreferenceDataStore;
import com.urbanairship.modules.Module;
import com.urbanairship.modules.aaid.AdIdModuleFactory;

import androidx.annotation.NonNull;

/**
 * Ad Id module factory implementation.
 *
 * @hide
 */
public class AdIdModuleFactoryImpl implements AdIdModuleFactory {

    @Override
    public Module build(@NonNull Context context, @NonNull PreferenceDataStore dataStore) {
        return Module.singleComponent(new AdvertisingIdTracker(context, dataStore), 0);
    }

}