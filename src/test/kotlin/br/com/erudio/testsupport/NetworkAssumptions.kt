package br.com.erudio.testsupport

import org.junit.jupiter.api.Assumptions
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

object NetworkAssumptions {

    private const val REPORT_IMAGES_HOST = "raw.githubusercontent.com"

    fun assumeReportImagesAreReachable() {
        Assumptions.assumeTrue(
            isReachable(REPORT_IMAGES_HOST, 443),
            "PDF reports download images from $REPORT_IMAGES_HOST, which is not reachable"
        )
    }

    private fun isReachable(host: String, port: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 3000)
                true
            }
        } catch (e: IOException) {
            false
        }
    }
}
