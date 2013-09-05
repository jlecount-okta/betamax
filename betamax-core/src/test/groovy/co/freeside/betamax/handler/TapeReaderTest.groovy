package co.freeside.betamax.handler

import co.freeside.betamax.Recorder
import co.freeside.betamax.message.*
import co.freeside.betamax.tape.Tape
import co.freeside.betamax.util.message.*

import org.easymock.EasyMock
import org.easymock.IAnswer

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo

import org.testng.annotations.*

class TapeReaderTest {

    Request request;
    Response response;
	HttpHandler nextHandler;
	TapeReader handler;
	Recorder recorder;

    @BeforeMethod
    public void setup() {
	    recorder = EasyMock.createMock(Recorder.class)
	    handler = new TapeReader(recorder)
	    nextHandler = EasyMock.createMock(HttpHandler.class)
	    request = new BasicRequest()
	    response = new BasicResponse()
		handler << nextHandler
	}

    @Test(description="chains if there is no matching tape entry")
    public void testChainsIfNoMatchingTapeEntry() {
		def tape = EasyMock.createMock(Tape.class)

		EasyMock.expect(tape.seek(request)).andReturn(false)
        EasyMock.expect(tape.isWritable()).andReturn(true)
        EasyMock.expect(tape.isReadable()).andReturn(true)
        EasyMock.expect(recorder.tape).andReturn(tape)
        EasyMock.expect(nextHandler.handle(request)).andReturn(response).times(1)

		EasyMock.replay(nextHandler, tape, recorder)

		handler.handle(request)

		EasyMock.verify(nextHandler, tape, recorder)
	}

    @Test(description="chains if there is a matching tape entry if the tape is not readable")
    public void testChainsIfThereIsAMatchingTapeEntryIfTapeNotReadable() {
		def tape = EasyMock.createMock(Tape.class)
		tape.play()
		EasyMock.expectLastCall().andAnswer(new IAnswer() {
            public Object answer() {
              fail();
              return null;
            }
        }).anyTimes();
        EasyMock.expect(nextHandler.handle(request)).andReturn(response).times(1)
        EasyMock.expect(tape.isReadable()).andReturn(false)
        EasyMock.expect(tape.isWritable()).andReturn(true)
        EasyMock.expect(recorder.tape).andReturn(tape)
        EasyMock.replay(recorder, tape, nextHandler)

		handler.handle(request)
		EasyMock.verify(recorder, tape, nextHandler)
	}

    @Test(description="succeeds if there is a matching tape entry")
    public void succeedsIfMatchingTapeEntry() {
		def tape = EasyMock.createMock(Tape.class)

		EasyMock.expect(tape.play(request)).andReturn(response).times(1)
		EasyMock.expect(tape.getName()).andReturn("name")
		EasyMock.expect(tape.isReadable()).andReturn(true)
		EasyMock.expect(tape.seek(request)).andReturn(true)
		EasyMock.expect(recorder.tape).andReturn(tape)
		EasyMock.replay(recorder, tape, nextHandler)

		def result = handler.handle(request)

		assertThat(result, equalTo(response))

        EasyMock.verify(tape, nextHandler)
	}

	@Test(expectedExceptions = [NoTapeException.class], description="throws an exception if there is no tape")
	public void throwExceptionIfNoTape() {
		EasyMock.expect(recorder.tape).andReturn(null)
	    EasyMock.replay(nextHandler, recorder)

		handler.handle(request)
        EasyMock.verify(nextHandler, recorder)
	}

	@Test(expectedExceptions = [NonWritableTapeException.class], description="throws an exception if there is no matching entry and the tape is not writable")
	public void throwExceptionIfNoMatchingEntryAndTapeNotWritable() {
		def tape = EasyMock.createMock(Tape.class)
		EasyMock.expect(tape.isReadable()).andReturn(true)
		EasyMock.expect(tape.isWritable()).andReturn(false)
		EasyMock.expect(tape.seek(request)).andReturn(false)
        EasyMock.expect(recorder.tape).andReturn(tape)
		EasyMock.replay(nextHandler, recorder, tape)

        handler.handle(request)
        EasyMock.verify(nextHandler, recorder, tape)
	}

}
