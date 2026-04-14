package com.fzm.wallet.sdk.utils

import android.content.Context
import android.telephony.TelephonyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RegionHelper {

    private const val KEY_IS_CHINA = "region_is_china"
    private const val IP_API_URL = "http://ip-api.com/json?fields=countryCode"
    private const val TIMEOUT_SECONDS = 5L

    fun isChinaUser(): Boolean {
        //return MMkvUtil.decodeBoolean(KEY_IS_CHINA)
        return false
    }

    suspend fun checkRegion(context: Context) {
        val isSim = checkSim(context)
        val isIp = checkIp()
        MMkvUtil.encode(KEY_IS_CHINA, isSim || isIp)
    }

    private fun checkSim(context: Context): Boolean {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                ?: return false
            val simCountry = tm.simCountryIso?.lowercase()
            val networkCountry = tm.networkCountryIso?.lowercase()
            simCountry == "cn" || networkCountry == "cn"
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun checkIp(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build()
            val request = Request.Builder().url(IP_API_URL).get().build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: return@withContext false
                val json = JSONObject(body)
                json.optString("countryCode") == "CN"
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
