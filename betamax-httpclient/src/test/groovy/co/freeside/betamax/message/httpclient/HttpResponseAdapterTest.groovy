package co.freeside.betamax.message.httpclient

import org.apache.http.entity.*
import org.apache.http.message.BasicHttpResponse
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import spock.lang.*
import static co.freeside.betamax.message.AbstractMessage.*
import static org.apache.http.HttpHeaders.*
import static org.apache.http.HttpVersion.HTTP_1_1

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

class HttpResponseAdapterTest {

    @DataProvider(name="content-type-info")
    public Object[][] getContentTypeInfo() {
        [
                ['image/png'              , 'image/png'],
                ['text/xml;charset=utf-8' , 'text/xml'],
                [null, DEFAULT_CONTENT_TYPE]
        ]
    }

    @DataProvider(name="header-values")
    public Object[][] getHeaderValues() {
        [
                [ CONTENT_TYPE, ['text/html'], 'text/html'],
                [ LAST_MODIFIED, ['12 Sep 2012 22:44:42 GMT'], '12 Sep 2012 22:44:42 GMT'],
                [ VIA, ['Proxy 1', 'Proxy 2'], 'Proxy 1, Proxy 2']
        ]
    }

    @DataProvider(name="charset-values")
    public Object[][] getCharsetValuesForContentTypeHeader() {
        [
                [null, DEFAULT_CHARSET],
                ['text/plain', DEFAULT_CHARSET],
                ['text/plain;charset=utf-8', 'UTF-8'],
                ['text/plain;charset=utf-16LE', 'UTF-16LE']
        ]
    }

    @Test(dataProvider='content-type-info')
    public void validateContentTypes(contentTypeHeader, expectedContentType) {
		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')
		if (contentTypeHeader) {
			response.setHeader(CONTENT_TYPE, contentTypeHeader)
		}

		def responseAdapter = new HttpResponseAdapter(response)

		assertThat(responseAdapter.contentType, equalTo(expectedContentType))

	}


    @Test(dataProvider='header-values')
    public void validateInterpretationOfHeaderValues(headerName, headerValues, expectedValue) {
		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')
		headerValues.each {
			response.addHeader(headerName, it)
		}

		def responseAdapter = new HttpResponseAdapter(response)

		assertThat(responseAdapter.headers[headerName], equalTo(expectedValue))
		assertThat(responseAdapter.getHeader(headerName), equalTo(expectedValue))

	}

    @Test(description="identifies if a response body is present")
    public void identifiesIfAResponseBodyIsPresent() {
		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')
		response.entity = new StringEntity('O HAI')

		def responseAdapter = new HttpResponseAdapter(response)

		assertThat(responseAdapter.hasBody(), is(true))
	}

    @Test(dataProvider="charset-values", description="interprets response body using #charset when Content-Type is declared as #contentTypeHeader")
    public void interpretsResponseBodyUsingACharsetWhenContentTypeIsX(contentTypeHeader, charset) {
		def body = 'Price: \u00a399.99'
		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')
		response.setHeader(CONTENT_TYPE, contentTypeHeader)
		response.entity = new ByteArrayEntity(body.getBytes(charset))

		def responseAdapter = new HttpResponseAdapter(response)

		assertThat(responseAdapter.bodyAsText.text, equalTo(body))

	}

    @Test(description="response body is re-readable")
    public void responseBodyIsReReadable() {
		def body = 'O HAI'.bytes
		def entity = new BasicHttpEntity()
		entity.content = new ByteArrayInputStream(body)

		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')
		response.setHeader(CONTENT_TYPE, 'text/plain')
		response.entity = entity

		def responseAdapter = new HttpResponseAdapter(response)

		assertThat(responseAdapter.bodyAsBinary.bytes,equalTo(body))

		assertThat(responseAdapter.bodyAsBinary.bytes, equalTo(body))
	}


    @Test(expectedExceptions = [IllegalStateException], description="cannot get the body of a response that does not have one")
    public void testYouCannotGetTheBodyOfAResponseThatDoesNotHaveOne() {
		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')

		def responseAdapter = new HttpResponseAdapter(response)

		responseAdapter.bodyAsBinary
	}

    @Test(description="You can add extra headers")
    public void testThatYouCanAddExtraHeaders() {
		def response = new BasicHttpResponse(HTTP_1_1, 200, 'OK')
		response.addHeader('X-What-Ever', 'Pff')

		def responseAdapter = new HttpResponseAdapter(response)

		responseAdapter.addHeader('X-What-Ever', 'Meh')

		assertThat(responseAdapter.getHeader('X-What-Ever'), equalTo('Pff, Meh'))
	}

}
