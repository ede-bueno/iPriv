package com.example.priv.data.security

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SecurityLockManager {

    private const val PREFS_NAME = "priv_security_prefs"
    private const val KEY_PIN = "security_pin_code"
    private const val KEY_PIN_ENABLED = "security_pin_enabled"
    private const val KEY_BIOMETRIC_ENABLED = "security_biometric_enabled"

    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_PIN_ENABLED, false)
        if (enabled) {
            _isAppLocked.value = true
        }
    }

    fun isPinEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PIN_ENABLED, false)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setPin(context: Context, pin: String, enableBiometrics: Boolean = true) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_PIN, pin)
            .putBoolean(KEY_PIN_ENABLED, true)
            .putBoolean(KEY_BIOMETRIC_ENABLED, enableBiometrics)
            .apply()
    }

    fun disableSecurity(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_PIN)
            .putBoolean(KEY_PIN_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        _isAppLocked.value = false
    }

    fun verifyPin(context: Context, inputPin: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val storedPin = prefs.getString(KEY_PIN, "")
        val match = storedPin == inputPin
        if (match) {
            _isAppLocked.value = false
        }
        return match
    }

    fun unlockWithBiometrics() {
        _isAppLocked.value = false
    }

    fun lockNow() {
        _isAppLocked.value = true
    }
}
