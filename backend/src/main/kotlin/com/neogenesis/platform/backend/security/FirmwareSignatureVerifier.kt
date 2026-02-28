package com.neogenesis.platform.backend.security

import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object FirmwareSignatureVerifier {
    fun verify(publicKeyPem: String, dataHashHex: String, signatureBase64: String): Boolean {
        val publicKey = parsePublicKey(publicKeyPem)
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initVerify(publicKey)
        signature.update(hexToBytes(dataHashHex))
        return signature.verify(Base64.getDecoder().decode(signatureBase64))
    }

    private fun parsePublicKey(pem: String): PublicKey {
        val clean = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .replace("\r", "")
            .trim()
        val bytes = Base64.getDecoder().decode(clean)
        val spec = X509EncodedKeySpec(bytes)
        return KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private fun hexToBytes(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in result.indices) {
            val index = i * 2
            val byte = hex.substring(index, index + 2).toInt(16)
            result[i] = byte.toByte()
        }
        return result
    }
}
