package com.example.util

import android.util.Base64
import com.example.data.model.Customer
import com.example.data.model.CustomerTransaction
import com.example.data.model.Invoice
import com.example.data.model.InvoiceItem
import com.example.data.model.Product
import com.example.data.model.SafeTransaction
import com.example.data.model.StoreSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

data class BackupPackage(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val appSignature: String = "ALHESAB_PRO_VAULT",
    val includedTypes: List<String> = emptyList(),
    val products: List<Product>? = null,
    val customers: List<Customer>? = null,
    val customerTransactions: List<CustomerTransaction>? = null,
    val invoices: List<Invoice>? = null,
    val invoiceItems: List<InvoiceItem>? = null,
    val safeTransactions: List<SafeTransaction>? = null,
    val storeSettings: StoreSettings? = null
)

data class BackupPreviewInfo(
    val timestamp: Long,
    val productsCount: Int,
    val customersCount: Int,
    val invoicesCount: Int,
    val safeTransactionsCount: Int,
    val includedTypes: List<String>
)

object BackupCryptoManager {

    private const val HEADER_PREFIX = "ALHESAB_ENC_V1::"
    private const val MASTER_SECRET = "AlHesabPro_Secret_Key_2026_Enterprise_Vault_Accounting"
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupPackage::class.java)

    private fun deriveKey(): SecretKeySpec {
        val sha = MessageDigest.getInstance("SHA-256")
        val keyBytes = sha.digest(MASTER_SECRET.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts the backup package into an encrypted proprietary Base64 string
     */
    fun encryptBackup(pkg: BackupPackage): String {
        val json = adapter.toJson(pkg)
        val secretKey = deriveKey()

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(json.toByteArray(StandardCharsets.UTF_8))

        // Combine IV (16 bytes) + EncryptedBytes
        val combined = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

        val base64Payload = Base64.encodeToString(combined, Base64.NO_WRAP)
        return "$HEADER_PREFIX$base64Payload"
    }

    /**
     * Decrypts the encrypted backup string back into a BackupPackage
     */
    fun decryptBackup(encryptedData: String): BackupPackage? {
        return try {
            val cleanData = encryptedData.trim()
            val payload = if (cleanData.startsWith(HEADER_PREFIX)) {
                cleanData.removePrefix(HEADER_PREFIX)
            } else {
                cleanData
            }

            val combined = Base64.decode(payload, Base64.DEFAULT)
            if (combined.size < 17) return null

            val iv = ByteArray(16)
            val ciphertext = ByteArray(combined.size - 16)
            System.arraycopy(combined, 0, iv, 0, 16)
            System.arraycopy(combined, 16, ciphertext, 0, ciphertext.size)

            val secretKey = deriveKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(ciphertext)
            val json = String(decryptedBytes, StandardCharsets.UTF_8)
            val pkg = adapter.fromJson(json)

            if (pkg != null && pkg.appSignature == "ALHESAB_PRO_VAULT") {
                pkg
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Get summary info about what's inside a decrypted backup
     */
    fun getPreviewInfo(pkg: BackupPackage): BackupPreviewInfo {
        return BackupPreviewInfo(
            timestamp = pkg.timestamp,
            productsCount = pkg.products?.size ?: 0,
            customersCount = pkg.customers?.size ?: 0,
            invoicesCount = pkg.invoices?.size ?: 0,
            safeTransactionsCount = pkg.safeTransactions?.size ?: 0,
            includedTypes = pkg.includedTypes
        )
    }
}
