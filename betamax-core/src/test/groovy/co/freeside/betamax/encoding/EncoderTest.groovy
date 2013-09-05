package co.freeside.betamax.encoding

import org.testng.annotations.*
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class EncoderTest {

    @DataProvider(name="provider")
    Object[] getData() {
        [
            ['this is some text that gets encoded', GzipEncoder.newInstance()],
            ['this is some text that gets encoded', DeflateEncoder.newInstance()]
        ]
    }


    @Test(dataProvider="provider", description="#encoderClass can decode what it has encoded")
    void encoderClassCanDecode(text, encoder) {
		def bytes = encoder.encode(text)

		assertThat(new String(bytes) != text, is(true))
		assertThat(encoder.decode(new ByteArrayInputStream(bytes)), equalTo(text))

	}

}
