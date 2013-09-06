package co.freeside.betamax.handler

import co.freeside.betamax.Recorder
import co.freeside.betamax.message.*
import co.freeside.betamax.tape.Tape
import co.freeside.betamax.util.message.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import org.easymock.EasyMock
import org.easymock.IAnswer

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo


class TapeWriterTest {

    Recorder recorder
    TapeWriter handler
    Request request
    Response response

    @BeforeMethod
    public void setup() {
	    recorder = EasyMock.createMock(Recorder)
	    handler = new TapeWriter(recorder)
	    request = new BasicRequest()
	    response = new BasicResponse()
    }

    @Test
    public void testWritesChainedReponseToTapeBeforeReturningIt() {
		def nextHandler = EasyMock.createMock(HttpHandler)
        EasyMock.expect(nextHandler.handle(EasyMock.anyObject())).andReturn(response)
        EasyMock.replay(nextHandler)

		handler << nextHandler

		def tape = EasyMock.createMock(Tape)
        EasyMock.expect(recorder.tape).andReturn(tape)
        tape.record(request, response)
        EasyMock.expect(tape.getName()).andReturn("something")
        EasyMock.expect(tape.isWritable()).andReturn(true)

        EasyMock.replay(recorder)
        EasyMock.replay(tape)

		def result = handler.handle(request)

		assertThat(result, equalTo(response))

        EasyMock.verify(nextHandler)
        EasyMock.verify(recorder)
        EasyMock.verify(tape)

	}

    @Test(expectedExceptions = [NoTapeException])
    public void throwExceptionIfNoTapeInserted() {
		EasyMock.expect(recorder.tape).andReturn(null)
        EasyMock.replay(recorder)

		handler.handle(request)

        EasyMock.verify(recorder)

	}

    @Test(expectedExceptions = [NonWritableTapeException])
    public void throwExceptionIfTapeIsNotWritable() {
		given:
		def tape = EasyMock.createMock(Tape)
        EasyMock.expect(recorder.tape).andReturn(tape)
        EasyMock.expect(tape.isWritable()).andReturn(false)
        EasyMock.replay(tape)
        EasyMock.replay(recorder)

		handler.handle(request)

	}

}
