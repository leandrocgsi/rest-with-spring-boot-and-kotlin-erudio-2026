package br.com.erudio.unittests.services

import br.com.erudio.services.QRCodeService
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

class QRCodeServiceTest {

    private val service = QRCodeService()

    private fun decode(image: BufferedImage): String {
        val bitmap = BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image)))
        return MultiFormatReader().decode(bitmap).text
    }

    @Test
    fun generatesAPngWithTheRequestedSize() {
        service.generateQRCode("https://pub.erudio.com.br", 200, 200).use { png ->
            val image = ImageIO.read(png)

            assertEquals(200, image.width)
            assertEquals(200, image.height)
        }
    }

    @Test
    fun generatesAQrCodeThatDecodesBackToTheUrl() {
        val url = "https://en.wikipedia.org/wiki/Ayrton_Senna"

        service.generateQRCode(url, 300, 300).use { png ->
            assertEquals(url, decode(ImageIO.read(png)))
        }
    }

    @Test
    fun encodesTextWithAccents() {
        val text = "Formação Spring Boot com Kotlin"

        service.generateQRCode(text, 300, 300).use { png ->
            assertEquals(text, decode(ImageIO.read(png)))
        }
    }

    @Test
    fun rejectsEmptyContent() {
        assertThrows(IllegalArgumentException::class.java) { service.generateQRCode("", 200, 200) }
    }
}
