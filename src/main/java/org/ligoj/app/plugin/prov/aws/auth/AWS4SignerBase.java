package org.ligoj.app.plugin.prov.aws.auth;

import static org.apache.commons.lang3.StringUtils.LF;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.bootstrap.core.resource.TechnicalException;

/**
 * Common methods and properties for all AWS4 signer variants
 */
public abstract class AWS4SignerBase {

	public static final String UNSIGNED_PAYLOAD = "UNSIGNED-PAYLOAD";
	public static final String SCHEME = "AWS4";
	public static final String ALGORITHM = "HMAC-SHA256";
	public static final String TERMINATOR = "aws4_request";
	private URLCodec urlCodec = new URLCodec();

	/**
	 * Returns the canonical collection of header names that will be included in
	 * the signature. For AWS4, all header names must be included in the process
	 * in sorted canonicalized order.
	 */
	protected String getCanonicalizeHeaderNames(final Map<String, String> headers) {
		return headers.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).map(header -> header.toLowerCase()).collect(Collectors.joining(";"));
	}

	/**
	 * Computes the canonical headers with values for the request. For AWS4, all
	 * headers must be included in the signing process.
	 */
	protected String getCanonicalizedHeaderString(final Map<String, String> headers) {
		if (headers.isEmpty()) {
			return "";
		}
		// step1: sort the headers by case-insensitive order
		// step2: form the canonical header:value entries in sorted order.
		// Multiple white spaces in the values should be compressed to a single
		// space.
		return headers.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER)
				.map(key -> key.toLowerCase().replaceAll("\\s+", " ") + ":" + headers.get(key).replaceAll("\\s+", " "))
				.collect(Collectors.joining(LF)) + LF;
	}

	/**
	 * Returns the canonical request string to go into the signer process; this
	 * consists of several canonical sub-parts.
	 *
	 * @return
	 */
	protected String getCanonicalRequest(final String path, final String httpMethod, final String queryParameters,
			final String canonicalizedHeaderNames, final String canonicalizedHeaders, final String bodyHash) {
		final String canonicalRequest = httpMethod + LF + getCanonicalizedResourcePath(path) + LF + queryParameters + LF + canonicalizedHeaders + LF
				+ canonicalizedHeaderNames + LF + bodyHash;
		return canonicalRequest;
	}

	/**
	 * Returns the canonicalized resource path for the service endpoint.
	 */
	protected String getCanonicalizedResourcePath(final String path) {
		try {
			return StringUtils.prependIfMissing(urlCodec.encode(StringUtils.trimToEmpty(path)).replace("%2F", "/"), "/");
		} catch (final EncoderException e) {
			throw new TechnicalException("Error during resource path encoding", e);
		}
	}

	/**
	 * Examines the specified query string parameters and returns a
	 * canonicalized form.
	 * <p>
	 * The canonicalized query string is formed by first sorting all the query
	 * string parameters, then URI encoding both the key and value and then
	 * joining them, in order, separating key value pairs with an '&'.
	 *
	 * @param parameters
	 *            The query string parameters to be canonicalized.
	 *
	 * @return A canonicalized form for the specified query string parameters.
	 */
	public String getCanonicalizedQueryString(final Map<String, String> parameters) {
		return parameters.keySet().stream().sorted().map(key -> {
			try {
				return urlCodec.encode(key) + "=" + urlCodec.encode(parameters.get(key));
			} catch (final EncoderException e) {
				throw new TechnicalException("Error during parameters encoding", e);
			}
		}).collect(Collectors.joining("&"));
	}

	/**
	 * return the string which must be signed
	 * 
	 * @param dateTime
	 *            sign date
	 * @param scope
	 *            scope
	 * @param canonicalRequest
	 *            canonical Request
	 * @return string to sign
	 */
	protected String getStringToSign(final String dateTime, final String scope, final String canonicalRequest) {
		return SCHEME + "-" + ALGORITHM + LF + dateTime + LF + scope + LF + hash(canonicalRequest);
	}

	/**
	 * Hashes the string contents (assumed to be UTF-8) using the SHA-256
	 * algorithm.
	 */
	public String hash(final String text) {
		return Hex.encodeHexString(DigestUtils.getSha256Digest().digest(org.apache.commons.codec.binary.StringUtils.getBytesUtf8(text)));
	}

	/**
	 * do a HMac sha256 sign
	 * 
	 * @param stringData
	 *            data as string
	 * @param key
	 *            key
	 * @return signature
	 */
	protected byte[] sign(final String stringData, final byte[] key) {
		return HmacUtils.hmacSha256(key, org.apache.commons.codec.binary.StringUtils.getBytesUtf8(stringData));
	}
}