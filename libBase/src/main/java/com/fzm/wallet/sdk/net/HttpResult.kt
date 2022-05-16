package com.fzm.wallet.sdk.net

import android.content.Context
import com.fzm.wallet.sdk.R
import com.fzm.wallet.sdk.base.WalletModuleApp
import com.google.gson.JsonSyntaxException
import retrofit2.HttpException
import java.io.IOException
import java.io.InterruptedIOException
import java.net.MalformedURLException
import java.net.SocketException
import java.net.UnknownHostException
import java.security.cert.CertificateException
import java.util.concurrent.TimeoutException
import javax.net.ssl.SSLHandshakeException


sealed class HttpResult<out T> {

    data class Success<out T>(val data: T?) : HttpResult<T>()

    data class Error(val e: String) : HttpResult<Nothing>()


    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Error -> "Error[exception=$e]"
        }
    }

    fun data(): T? {
        return (this as Success<T>).data
    }

    fun dataOrNull(): T? {
        return (this as? Success<T>)?.data
    }

    fun isSucceed(): Boolean {
        return this is Success
    }

    fun error(): String {
        return (this as Error).e
    }

    companion object {

        fun handleException(e: Exception?): String? {
            val context: Context? = WalletModuleApp.context

            return when (e) {
                null -> context?.getString(R.string.basic_error_unknown)
                is CertificateException, is SSLHandshakeException
                -> context!!.getString(R.string.basic_error_certificate)
                is MalformedURLException -> context?.getString(R.string.basic_error_service_domain)
                is HttpException -> context?.getString(R.string.basic_error_service)
                is InterruptedIOException, is SocketException, is TimeoutException, is UnknownHostException
                -> context?.getString(R.string.basic_error_network)
                is JsonSyntaxException -> context?.getString(R.string.basic_error_response_parse)
                is IOException -> context?.getString(R.string.basic_error_request)
                is ClassCastException -> context?.getString(R.string.basic_error_data_structure)
                else -> e.toString()

            }
        }
    }


}