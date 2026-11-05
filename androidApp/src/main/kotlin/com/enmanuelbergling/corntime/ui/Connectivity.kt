package com.enmanuelbergling.corntime.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Emits whether the device currently has an internet-capable network. */
fun Context.observeOnline(): Flow<Boolean> = callbackFlow {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networks = mutableSetOf<Network>()

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networks += network
            trySend(true)
        }

        override fun onLost(network: Network) {
            networks -= network
            trySend(networks.isNotEmpty())
        }
    }
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()

    manager.registerNetworkCallback(request, callback)
    trySend(manager.activeNetwork != null)
    awaitClose { manager.unregisterNetworkCallback(callback) }
}.distinctUntilChanged()
