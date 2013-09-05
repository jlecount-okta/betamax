package co.freeside.betamax.handler

import co.freeside.betamax.message.*

import org.testng.annotations.*
import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.equalTo
import org.easymock.EasyMock

class ChainedHttpHandlerTest {

	ChainedHttpHandler handler

    @BeforeMethod
    public void setup() {
	    handler = new ChainedHttpHandler() {
		    @Override
		    Response handle(Request request) {
			    throw new UnsupportedOperationException()
		    }
	    }
	}

	Request request = [:] as Request
	Response response = [:] as Response

	@Test(expectedExceptions=[IllegalStateException.class], description="throws an exception if chain is called on the last handler in the chain")
	 public void throwExceptionIfChainCalledOnLastHandler() {
		handler.chain(request)
	}

	@Test(description="chain passes to the next handler if there is one")
	 public void chainPassesToNextHandlerIfExists() {
		def nextHandler = EasyMock.createMock(HttpHandler.class)
		EasyMock.expect(nextHandler.handle(request)).andReturn(response).times(1)
		EasyMock.replay(nextHandler)
		handler << nextHandler

		def result =  handler.chain(request)

        assertThat(result, equalTo(response))
        EasyMock.verify(nextHandler)
	}
}
