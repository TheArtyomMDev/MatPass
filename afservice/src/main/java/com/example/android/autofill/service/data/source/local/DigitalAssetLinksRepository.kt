package com.example.android.autofill.service.data.source.local

import androidx.annotation.RequiresApi
import android.os.Build
import android.content.pm.PackageManager
import com.google.common.net.InternetDomainName
import retrofit2.Retrofit
import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.data.source.DalService
import com.example.android.autofill.service.data.source.DigitalAssetLinksDataSource
import com.example.android.autofill.service.model.DalCheck
import com.example.android.autofill.service.model.DalInfo
import com.example.android.autofill.service.util.SecurityHelper
import com.example.android.autofill.service.util.Util
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.Exception
import java.util.HashMap

/**
 * Singleton repository that caches the result of Digital Asset Links checks.
 */
class DigitalAssetLinksRepository private constructor(private val mPackageManager: PackageManager?) :
    DigitalAssetLinksDataSource {
    private val mDalService: DalService?
    private val mCache: HashMap<DalInfo?, DalCheck?>?
    override fun clear() {
        mCache!!.clear()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun checkValid(
        dalCheckRequirement: Util.DalCheckRequirement?, dalInfo: DalInfo?,
        dalCheckDataCallback: DataCallback<DalCheck?>?
    ) {
        if (dalCheckRequirement == Util.DalCheckRequirement.Disabled) {
            val dalCheck = DalCheck()
            dalCheck.linked = true
            dalCheckDataCallback!!.onLoaded(dalCheck)
            return
        }
        val dalCheck = mCache!!.get(dalInfo)
        if (dalCheck != null) {
            dalCheckDataCallback!!.onLoaded(dalCheck)
            return
        }
        val packageName = dalInfo!!.getPackageName()
        val webDomain = dalInfo.getWebDomain()
        val fingerprint: String?
        fingerprint = try {
            val packageInfo = mPackageManager!!.getPackageInfo(packageName!!,
                PackageManager.GET_SIGNATURES)
            SecurityHelper.Companion.getFingerprint(packageInfo, packageName)
        } catch (e: Exception) {
            dalCheckDataCallback!!.onDataNotAvailable("Error getting fingerprint for %s",
                packageName)
            return
        }
        Util.logd("validating domain %s for pkg %s and fingerprint %s.", webDomain,
            packageName, fingerprint)
        mDalService!!.check(webDomain, PERMISSION_GET_LOGIN_CREDS, packageName, fingerprint)!!.enqueue(
            object : Callback<DalCheck?> {
                override fun onResponse(
                    call: Call<DalCheck?>,
                    response: Response<DalCheck?>
                ) {
                    val dalCheck = response.body()
                    if (dalCheck == null || !dalCheck.linked) {
                        // get_login_creds check failed, so try handle_all_urls check
                        if (dalCheckRequirement == Util.DalCheckRequirement.LoginOnly) {
                            dalCheckDataCallback!!.onDataNotAvailable(
                                "DAL: Login creds check failed.")
                        } else if (dalCheckRequirement == Util.DalCheckRequirement.AllUrls) {
                            mDalService.check(webDomain, PERMISSION_HANDLE_ALL_URLS,
                                packageName, fingerprint)!!.enqueue(object : Callback<DalCheck?> {
                                override fun onResponse(
                                    call: Call<DalCheck?>,
                                    response: Response<DalCheck?>
                                ) {
                                    val dalCheck = response.body()
                                    mCache[dalInfo] = dalCheck
                                    dalCheckDataCallback!!.onLoaded(dalCheck)
                                }

                                override fun onFailure(
                                    call: Call<DalCheck?>,
                                    t: Throwable
                                ) {
                                    dalCheckDataCallback!!.onDataNotAvailable(t.message)
                                }
                            })
                        }
                    } else {
                        // get_login_creds check succeeded, so we're finished.
                        mCache[dalInfo] = dalCheck
                        dalCheckDataCallback!!.onLoaded(dalCheck)
                    }
                }

                override fun onFailure(call: Call<DalCheck?>, t: Throwable) {
                    // get_login_creds check failed, so try handle_all_urls check.
                    mDalService.check(webDomain, PERMISSION_HANDLE_ALL_URLS, packageName,
                        fingerprint)
                }
            })
    }

    companion object {
        private val DAL_BASE_URL: String? = "https://digitalassetlinks.googleapis.com"
        private val PERMISSION_GET_LOGIN_CREDS: String? = "common.get_login_creds"
        private val PERMISSION_HANDLE_ALL_URLS: String? = "common.handle_all_urls"
        private var sInstance: DigitalAssetLinksRepository? = null
        fun getInstance(packageManager: PackageManager?): DigitalAssetLinksRepository? {
            if (sInstance == null) {
                sInstance = DigitalAssetLinksRepository(packageManager)
            }
            return sInstance
        }

        fun getCanonicalDomain(domain: String?): String? {
            var idn = InternetDomainName.from(domain)
            while (idn != null && !idn.isTopPrivateDomain) {
                idn = idn.parent()
            }
            return idn?.toString()
        }
    }

    init {
        mCache = HashMap()
        mDalService = Retrofit.Builder()
            .baseUrl(DAL_BASE_URL)
            .build()
            .create(DalService::class.java)
    }
}