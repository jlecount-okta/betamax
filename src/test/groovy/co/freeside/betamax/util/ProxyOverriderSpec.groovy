package co.freeside.betamax.util

import spock.lang.*

@Unroll
class ProxyOverriderSpec extends Specification {

	private static final URI HTTP_URI = 'http://freeside.co/betamax'.toURI()
	private static final URI HTTPS_URI = 'https://github.com/robfletcher/betamax'.toURI()

	ProxyOverrider proxyOverrider = new ProxyOverrider()

	void cleanup() {
		System.with {
			clearProperty 'http.proxyHost'
			clearProperty 'http.proxyPort'
			clearProperty 'https.proxyHost'
			clearProperty 'https.proxyPort'
			clearProperty 'http.nonProxyHosts'
		}
	}

	void 'activate sets up proxy override if no existing proxy settings exist'() {
		when:
		proxyOverrider.activate 'override', 9999, ['localhost']

		then:
		System.properties.'http.proxyHost' == 'override'
		System.properties.'http.proxyPort' == '9999'
		System.properties.'https.proxyHost' == 'override'
		System.properties.'https.proxyPort' == '9999'
		System.properties.'http.nonProxyHosts' == 'localhost'
	}

	void 'activate overrides existing proxy settings'() {
		given:
		System.properties.'http.proxyHost' = 'myproxy'
		System.properties.'http.proxyPort' = '1337'
		System.properties.'https.proxyHost' = 'myproxy'
		System.properties.'https.proxyPort' = '1337'
		System.properties.'http.nonProxyHosts' == 'mydomain.com'

		when:
		proxyOverrider.activate 'override', 9999, ['localhost']

		then:
		System.properties.'http.proxyHost' == 'override'
		System.properties.'http.proxyPort' == '9999'
		System.properties.'https.proxyHost' == 'override'
		System.properties.'https.proxyPort' == '9999'
		System.properties.'http.nonProxyHosts' == 'localhost'
	}

	void 'deactivateAll clears proxy settings if none existed before activation'() {
		when:
		proxyOverrider.activate 'override', 9999, ['localhost']
		proxyOverrider.deactivateAll()

		then:
		System.properties.'http.proxyHost' == null
		System.properties.'http.proxyPort' == null
		System.properties.'https.proxyHost' == null
		System.properties.'https.proxyPort' == null
		System.properties.'http.nonProxyHosts' == null

		and:
		proxyOverrider.@originalProxies.isEmpty()
		!proxyOverrider.@originalNonProxyHosts
	}

	void 'deactivateAll restores original proxy settings'() {
		given:
		System.properties.'http.proxyHost' = 'myproxy'
		System.properties.'http.proxyPort' = '1337'
		System.properties.'https.proxyHost' = 'myproxy'
		System.properties.'https.proxyPort' = '1337'
		System.properties.'http.nonProxyHosts' = 'mydomain.com'

		when:
		proxyOverrider.activate 'override', 9999, ['localhost']
		proxyOverrider.deactivateAll()

		then:
		System.properties.'http.proxyHost' == 'myproxy'
		System.properties.'http.proxyPort' == '1337'
		System.properties.'https.proxyHost' == 'myproxy'
		System.properties.'https.proxyPort' == '1337'
		System.properties.'http.nonProxyHosts' == 'mydomain.com'

		and:
		proxyOverrider.@originalProxies.isEmpty()
		!proxyOverrider.@originalNonProxyHosts
	}

	void 'originalProxySelector uses direct route if Betamax is not active'() {
		expect:
		def proxies = proxyOverrider.originalProxySelector.select(HTTP_URI)
		proxies.size() == 1

		and:
		def proxy = proxies.first()
		proxy.type() == Proxy.Type.DIRECT
	}

	void 'uses direct route if there were no pre-existing proxy settings'() {
		given:
		proxyOverrider.activate 'override', 9999, ['localhost']

		expect:
		def proxies = proxyOverrider.originalProxySelector.select(HTTP_URI)
		proxies.size() == 1

		and:
		def proxy = proxies.first()
		proxy.type() == Proxy.Type.DIRECT
	}

	void 'uses pre-existing #scheme proxy if Betamax is overriding proxy settings'() {
		given:
		setupProxy 'myproxy', 1337
		proxyOverrider.activate 'override', 9999, ['localhost']

		expect:
		def proxies = proxyOverrider.originalProxySelector.select(uri)
		proxies.size() == 1

		and:
		def proxy = proxies.first()
		proxy.type() == Proxy.Type.HTTP
		proxy.address() == new InetSocketAddress('myproxy', 1337)

		where:
		uri << [HTTP_URI, HTTPS_URI]
		scheme = uri.scheme
	}

	void 'does not use pre-existing #scheme proxy for uri that is ignored'() {
		given:
		setupProxy 'myproxy', 1337, uri.host
		proxyOverrider.activate 'override', 9999, ['localhost']

		expect:
		def proxies = proxyOverrider.originalProxySelector.select(uri)
		proxies.size() == 1

		and:
		def proxy = proxies.first()
		proxy.type() == Proxy.Type.DIRECT

		where:
		uri << [HTTP_URI, HTTPS_URI]
		scheme = uri.scheme
	}

	private void setupProxy(String host, int port, String nonProxyHosts = null) {
		System.with {
			for (scheme in ['http', 'https']) {
				properties."${scheme}.proxyHost" = host
				properties."${scheme}.proxyPort" = port.toString()
			}
			if (nonProxyHosts) {
				properties.'http.nonProxyHosts' = nonProxyHosts
			}
		}
	}
}
