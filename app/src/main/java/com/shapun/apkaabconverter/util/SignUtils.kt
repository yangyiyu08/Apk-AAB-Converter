package com.shapun.apkaabconverter.util

import android.content.Context
import com.android.tools.build.bundletool.model.SignerConfig
import com.android.tools.build.bundletool.model.SigningConfiguration
import com.android.tools.build.bundletool.model.exceptions.CommandExecutionException
import com.google.common.collect.ImmutableList
import java.io.InputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*

object SignUtils {

    fun getDebugSigningConfiguration(context: Context): SigningConfiguration {
        val assets = context.assets
        val key = assets.open("testkey.pk8").use {
            (Class.forName("sun.security.pkcs.PKCS8Key").newInstance() as PrivateKey).apply {
                this.javaClass.getMethod("decode", InputStream::class.java).invoke(this, it)
            }
        }
        val cert = assets.open("testkey.x509.pem").use {
            (CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate)
        }
        return SigningConfiguration.builder().setSignerConfig(key, cert).build()
    }

    fun getKeyStore(inputStream: InputStream, password: String): KeyStore {
        val keystore: KeyStore = KeyStore.getInstance("PKCS12")
        keystore.load(inputStream, password.toCharArray())
        return keystore
    }

    fun getSigningConfig(
        inputStream: InputStream,
        keyAlias: String,
        keystorePassword: String,
        keyPassword: String
    ): SigningConfiguration {
        return getSigningConfig(
            getKeyStore(inputStream, keystorePassword), keyAlias, keyPassword
        )
    }

    fun getSigningConfig(
        keystore: KeyStore, keyAlias: String, keyPassword: String
    ): SigningConfiguration {
        val privateKey = keystore.getKey(keyAlias, keyPassword.toCharArray()) as PrivateKey
        val certChain = keystore.getCertificateChain(keyAlias)
            ?: throw CommandExecutionException.builder()
                .withInternalMessage("No key found with alias '%s' in keystore.", keyAlias)
                .build()
        val certificates = Arrays.stream(certChain).map { c: Certificate? -> c as X509Certificate? }
            .collect(ImmutableList.toImmutableList())
        return SignerConfig.builder().setPrivateKey(privateKey).setCertificates(certificates)
            .build().toSigningConfiguration()
    }

    private fun SignerConfig.toSigningConfiguration(): SigningConfiguration {
        return SigningConfiguration.builder().setSignerConfig(this).build()
    }
}