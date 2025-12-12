package com.student.overcooked.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * Lightweight online/offline detector using ConnectivityManager callbacks.
 */
public class ConnectivityObserver {
    private final ConnectivityManager connectivityManager;
    private final MutableLiveData<Boolean> isOnlineLive = new MutableLiveData<>(false);

    private final NetworkRequest request = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build();

    private final ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(Network network) {
            isOnlineLive.postValue(true);
        }

        @Override
        public void onLost(Network network) {
            isOnlineLive.postValue(isCurrentlyOnline());
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            isOnlineLive.postValue(isCurrentlyOnline());
        }
    };

    public ConnectivityObserver(Context context) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        isOnlineLive.setValue(isCurrentlyOnline());
    }

    private boolean isCurrentlyOnline() {
        Network net = connectivityManager.getActiveNetwork();
        if (net == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(net);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    public void start() {
        connectivityManager.registerNetworkCallback(request, callback);
        isOnlineLive.postValue(isCurrentlyOnline());
    }

    public void stop() {
        try {
            connectivityManager.unregisterNetworkCallback(callback);
        } catch (Exception ignored) {
        }
    }

    public LiveData<Boolean> isOnline() {
        return isOnlineLive;
    }
}
