package co.freeside.betamax.tape

import java.util.concurrent.CountDownLatch
import co.freeside.betamax.*
import co.freeside.betamax.handler.*
import co.freeside.betamax.proxy.jetty.SimpleServer
import co.freeside.betamax.util.message.BasicRequest
import co.freeside.betamax.util.server.HelloHandler

import com.tngtech.testng.rules.annotations.TestNGRule
import com.tngtech.testng.rules.RulesListener
import org.testng.annotations.*;

import spock.lang.*
import static co.freeside.betamax.util.FileUtils.newTempDir
import static co.freeside.betamax.util.server.HelloHandler.HELLO_WORLD
import static java.util.concurrent.TimeUnit.SECONDS

@Listeners(RulesListener.class)
class MultiThreadedTapeWritingSpec {

	@Shared @AutoCleanup('deleteDir') File tapeRoot = newTempDir('tapes')
	@TestNGRule Recorder recorder = new Recorder(tapeRoot: tapeRoot)
	HttpHandler handler = new DefaultHandlerChain(recorder)

	@Shared @AutoCleanup('stop') SimpleServer endpoint = new SimpleServer()

    @BeforeClass
	void setupSpec() {
	    println "JL DEBUG: RUNNING!"
		endpoint.start(HelloHandler)
	}

    @Test
	@Betamax(tape = 'multi_threaded_tape_writing_spec', mode = TapeMode.READ_WRITE)
	void testThatTapeCopesWithConcurrentReadingAndWriting() {
		def finished = new CountDownLatch(threads)
		def responses = [:]
		10.times { i ->
			Thread.start {
				try {
					def request = new BasicRequest('GET', "$endpoint.url$i")
					responses[i] = handler.handle(request).bodyAsText.text
				} catch (IOException e) {
					responses[i] = 'FAIL!'
				}
				finished.countDown()
			}
		}

		finished.await(1, SECONDS)

		responses.each {
			assertTrue(it.value == HELLO_WORLD)
		}

	}
}
