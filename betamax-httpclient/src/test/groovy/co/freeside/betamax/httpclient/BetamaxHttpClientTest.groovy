package co.freeside.betamax.httpclient

import co.freeside.betamax.*
import co.freeside.betamax.handler.HandlerException
import co.freeside.betamax.proxy.jetty.SimpleServer
import co.freeside.betamax.util.Network
import co.freeside.betamax.util.server.*
import groovyx.net.http.RESTClient
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.AbstractHttpClient
import org.apache.http.params.HttpParams
import org.eclipse.jetty.server.Handler

import org.easymock.EasyMock

import spock.lang.Issue

import com.tngtech.testng.rules.annotations.TestNGRule
import com.tngtech.testng.rules.RulesListener

import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import static org.hamcrest.Matchers.is

import org.testng.annotations.*

import static co.freeside.betamax.util.FileUtils.newTempDir
import static co.freeside.betamax.util.server.HelloHandler.HELLO_WORLD
import static java.net.HttpURLConnection.HTTP_OK
import static org.apache.http.HttpHeaders.VIA
import static org.apache.http.entity.ContentType.APPLICATION_FORM_URLENCODED

@Issue('https://github.com/robfletcher/betamax/issues/40')
@Listeners(RulesListener)
class BetamaxHttpClientTest {

    public @TestNGRule Recorder recorder;

	File tapeRoot
	SimpleServer endpoint
	def http

    @BeforeMethod
    public void setupMethod() {
	    endpoint = new SimpleServer()
	    recorder = new Recorder(tapeRoot: tapeRoot)
	    http = new BetamaxHttpClient(recorder)
    }

    @AfterMethod
    public void tearDownMethod() {
	    endpoint.stop()
    }

    @BeforeClass
    public void setupClass() {
        tapeRoot = co.freeside.betamax.util.FileUtils.newTempDir('tapes')
    }

    @AfterClass
    public void tearDownClass() {
        tapeRoot.deleteDir()
    }


	@Betamax(tape = 'betamax http client', mode = TapeMode.READ_WRITE)
	@Test(description="can use Betamax without starting the proxy")
	public void canUseBetamaxWithoutStartingTheProxy() {
		endpoint.start(HelloHandler)

		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		assertThat(response.statusLine.statusCode, equalTo(HTTP_OK))
		assertThat(response.entity.content.text, equalTo(HELLO_WORLD))

		assertThat(response.getFirstHeader(VIA).value, equalTo('Betamax'))
		assertThat(response.getFirstHeader('X-Betamax').value, equalTo('REC'))
	}

	@Betamax(tape = 'betamax http client', mode = TapeMode.READ_WRITE)
    @Test(description="can play back from tape")
	public void canPlayBackFromTape() {
		def handler = EasyMock.createMock(Handler)
        handler.setServer(EasyMock.anyObject())
        handler.handle(EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject(), EasyMock.anyObject())
        handler.start()
        handler.stop()
		EasyMock.replay(handler)

		endpoint.start(handler)

		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		assertThat(response.statusLine.statusCode, equalTo(HTTP_OK))
		assertThat(response.entity.content.text, equalTo(HELLO_WORLD))

		assertThat(response.getFirstHeader(VIA).value, equalTo('Betamax'))
		assertThat(response.getFirstHeader('X-Betamax').value, equalTo('PLAY'))
		EasyMock.verify(handler)
	}

	@Betamax(tape = 'betamax http client', mode = TapeMode.READ_WRITE)
	@Test(description= "betamax http client")
	public void canSendARequestWithABody() {
		endpoint.start(EchoHandler)

		def request = new HttpPost(endpoint.url)
		request.entity = new StringEntity('message=O HAI', APPLICATION_FORM_URLENCODED)

		def response = http.execute(request)


        assertThat(response.statusLine.statusCode, equalTo(HTTP_OK))
		assertThat(response.entity.content.text.endsWith('message=O HAI'), is(true))

		assertThat(response.getFirstHeader(VIA).value, equalTo('Betamax'))
	}

    @Test(description="Fails in a non-annotated test")
    public void failsInANonAnnotatedTest() {
		def handler = EasyMock.createMock(Handler)
	    EasyMock.replay(handler)

		endpoint.start(handler)

        try {
		    http.execute(new HttpGet(endpoint.url))
		    fail("Should have thrown HandlerException.")
		} catch (HandlerException e) {
		    assertThat(e.message, equalTo("No tape"))
		}
		EasyMock.verify(handler)
	}

	@Betamax(tape = 'betamax http client')
    @Test(description="can use ignoreLocalhost config setting")
    public void canUseIgnoreLocalhostConfigSetting() {

		endpoint.start(HelloHandler)
		recorder.ignoreLocalhost = true
		def request = new HttpGet(endpoint.url)

		def response = http.execute(request)

		assertThat(response.statusLine.statusCode, equalTo(HTTP_OK))
		assertThat(response.entity.content.text, equalTo(HELLO_WORLD))

        assertThat(response.getFirstHeader(VIA), nullValue())
        assertThat(response.getFirstHeader('X-Betamax'), nullValue())
	}

	@Betamax(tape = 'betamax http client')
    @Test(description="can use ignoreHosts config setting")
	public void canUseIgnoreHostsConfigSetting() {
		endpoint.start(HelloHandler)
		recorder.ignoreHosts = Network.localAddresses

		def request = new HttpGet(endpoint.url)
		def response = http.execute(request)

		assertThat(response.statusLine.statusCode, equalTo(HTTP_OK))
        assertThat(response.entity.content.text, equalTo(HELLO_WORLD))

		assertThat(response.getFirstHeader(VIA), nullValue())
		assertThat(response.getFirstHeader('X-Betamax'), nullValue())
	}

	@Betamax(tape = 'betamax http client', mode = TapeMode.READ_WRITE)
	@Test(description="can use with HttpBuilder")
	public void canUseWithHttpBuilder() {
		endpoint.start(HelloHandler)

		def restClient = new RESTClient() {
			@Override
			protected AbstractHttpClient createClient(HttpParams params) {
				new BetamaxHttpClient(recorder)
			}
		}

		def response = restClient.get(uri: endpoint.url)

		assertThat(response.status, equalTo(HTTP_OK))
		assertThat(response.data.text, equalTo(HELLO_WORLD))

		assertThat(response.getFirstHeader(VIA).value, equalTo('Betamax'))
		assertThat(response.getFirstHeader('X-Betamax').value, equalTo('PLAY'))
	}
}
