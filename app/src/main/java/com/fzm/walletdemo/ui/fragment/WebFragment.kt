package com.fzm.walletdemo.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import com.fzm.wallet.sdk.IPConfig
import com.fzm.wallet.sdk.IPConfig.Companion.SQZZ
import com.fzm.wallet.sdk.IPConfig.Companion.TP_YBC
import com.fzm.wallet.sdk.IPConfig.Companion.TP_YBSQ
import com.fzm.wallet.sdk.IPConfig.Companion.TP_YJ
import com.fzm.walletdemo.BuildConfig
import com.fzm.walletdemo.W
import com.fzm.walletdemo.databinding.WebDappBinding
import com.fzm.walletdemo.ui.JsApi
import wendu.dsbridge.DWebView

class WebFragment : Fragment() {

    private lateinit var binding: WebDappBinding

    companion object {
        //提案
        const val TAG_TIAN = "showTianFragment"
        //投票
        const val TAG_TP = "showTPFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = WebDappBinding.inflate(inflater, container, false)

        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initWebView()
        var url = ""
        if(W.appType == IPConfig.APP_YBC) {
            if(tag == TAG_TIAN){
                url = SQZZ
            }else if(tag == TAG_TP){
                url = TP_YBC
            }
        }
        else if(W.appType == IPConfig.APP_YBS) {
            if(tag == TAG_TIAN){
                url = SQZZ
            }else if(tag == TAG_TP){
                url = TP_YBSQ
            }
        }
        else if(W.appType == IPConfig.APP_YJM) {
            if(tag == TAG_TIAN){
                url = SQZZ
            }else if(tag == TAG_TP){
                url = TP_YJ
            }
        }
        binding.webDapp.loadUrl(url)
    }
    private fun initWebView() {
        DWebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        activity?.let {
            binding.webDapp.addJavascriptObject(JsApi(binding.webDapp,it), null)
        }
        //binding.webDapp.addJavascriptObject(JSApi(this), null)
        binding.webDapp.settings.javaScriptEnabled = true
        binding.webDapp.settings.domStorageEnabled = true
        //解决http图片不显示
        binding.webDapp.settings.blockNetworkImage = false
        val userAgentString = binding.webDapp.settings.userAgentString
        val resultAgent = "$userAgentString;wallet;1.0"
        binding.webDapp.settings.userAgentString = resultAgent

        binding.webDapp.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressWeb.progress = newProgress
            }
        }
        binding.webDapp.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.progressWeb.visibility = View.GONE
            }
        }

    }


}