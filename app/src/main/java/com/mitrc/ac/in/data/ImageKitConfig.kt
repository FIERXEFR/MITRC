package com.mitrc.ac.`in`.data

/**
 * ImageKit.io configuration and URL builder for optimized image serving.
 *
 * The public key and URL endpoint are safe to ship in the APK; the private key is
 * stored XOR-encrypted in native-lib.cpp for any server-side operations.
 */
object ImageKitConfig {
    /** The ImageKit URL endpoint from the dashboard. */
    const val URL_ENDPOINT = "https://ik.imagekit.io/Mainimages"

    /** The public API key for client-side URL signing / identification. */
    const val PUBLIC_KEY = "public_q/mY5gu2ooRwGwf9pdpnkqMTZVo="

    /** Build an ImageKit CDN URL for a given file path, with optional transformations.
     *
     * @param filePath   the path of the file in the ImageKit media library (e.g. "default-image.jpg")
     * @param tr         an optional transformation string (e.g. "w-400,h-300" or "f-webp,fo-auto").
     *                   If null, the URL is returned without the `?tr=` query parameter.
     * @return a fully-qualified ImageKit URL.
     */
    fun url(filePath: String, tr: String? = null): String {
        val base = "$URL_ENDPOINT/$filePath"
        return if (tr != null && tr.isNotBlank()) "$base?tr=$tr" else base
    }

    /** Convenience: resize-then-webp transform.
     *  Useful for event / avatar placeholders. */
    fun resizedWebp(filePath: String, widthPx: Int): String = url(filePath, "w-$widthPx,f-webp,fo-auto")
}