/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofill.service.util

import android.content.pm.PackageManager
import kotlin.Throws
import android.content.pm.PackageInfo
import java.io.ByteArrayInputStream
import java.io.IOException
import java.lang.StringBuilder
import java.lang.UnsupportedOperationException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

/**
 * Helper class for security checks.
 */
class SecurityHelper private constructor() {
    companion object {
        /**
         * Gets the fingerprint of the signed certificate of a package.
         */
        @Throws(PackageManager.NameNotFoundException::class,
            IOException::class,
            NoSuchAlgorithmException::class,
            CertificateException::class)
        fun getFingerprint(packageInfo: PackageInfo?, packageName: String?): String? {
            val signatures = packageInfo!!.signatures
            if (signatures.size != 1) {
                throw SecurityException(packageName + " has " + signatures.size + " signatures")
            }
            val cert = signatures[0].toByteArray()
            ByteArrayInputStream(cert).use { input ->
                val factory = CertificateFactory.getInstance("X509")
                val x509 = factory.generateCertificate(input) as X509Certificate
                val md = MessageDigest.getInstance("SHA256")
                val publicKey = md.digest(x509.encoded)
                return toHexFormat(publicKey)
            }
        }

        private fun toHexFormat(bytes: ByteArray?): String? {
            val builder = StringBuilder(bytes!!.size * 2)
            for (i in bytes.indices) {
                var hex = Integer.toHexString(bytes.get(i).toInt())
                val length = hex.length
                if (length == 1) {
                    hex = "0$hex"
                }
                if (length > 2) {
                    hex = hex.substring(length - 2, length)
                }
                builder.append(hex.toUpperCase())
                if (i < bytes.size - 1) {
                    builder.append(':')
                }
            }
            return builder.toString()
        }
    }

    init {
        throw UnsupportedOperationException("Provides static methods only.")
    }
}