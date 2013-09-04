package co.freeside.betamax.encoding

import org.testng.annotations.*
import
@Unroll
class EncoderTest {

    @BeforeMethod
    public void setup() {
		def bytes = encoder.encode(text)
    }

    @Test(description="#encoderClass can decode what it has encoded")
    void encoderClassCanDecode() {

		assertThat(new String(bytes) != text, is(true))
		assertThat(encoder.decode(new ByteArrayInputStream(bytes)), equalTo(text))

		where:
		encoderClass << [GzipEncoder, DeflateEncoder]
		text = 'this is some text that gets encoded'
		encoder = encoderClass.newInstance()
	}

}
