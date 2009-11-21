// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS.security;

import java.security.SignatureException;
import java.security.interfaces.DSAParams;
import java.util.Arrays;

/**
 * Converts DSA signatures between the RRSIG/SIG record format (as specified
 * in RFC 2536) and the format used by Java DSA routines (DER-encoded).
 *
 * @author Brian Wellington
 */

public class DSASignature {

static final int ASN1_SEQ = 0x30;
static final int ASN1_INT = 0x2;

private DSASignature() {}

/**
 * Converts the signature field in a SIG record to the
 * format expected by the DSA verification routines.
 */
public static byte []
fromDNS(byte [] sig) {
	final int len = 20;
	int n = 0;
	byte rlen, slen, seqlen;

	rlen = len;
	if (sig[1] < 0)
		rlen++;

	slen = len;
	if (sig[len + 1] < 0)
		slen++;

	/* 4 = 2 * (INT, value) */
	seqlen = (byte) (rlen + slen + 4);

	/* 2 = 1 * (SEQ, value) */
	byte [] array = new byte[seqlen + 2];

	array[n++] = ASN1_SEQ;
	array[n++] = (byte) seqlen;
	array[n++] = ASN1_INT;
	array[n++] = rlen;
	if (rlen > len)
		array[n++] = 0;
	for (int i = 0; i < len; i++, n++)
		array[n] = sig[1 + i];
	array[n++] = ASN1_INT;
	array[n++] = slen;
	if (slen > len)
		array[n++] = 0;
	for (int i = 0; i < len; i++, n++)
		array[n] = sig[1 + len + i];
	return array;
}

/**
 * Converts the signature generated by DSA signature routines to
 * the one expected inside an RRSIG/SIG record.
 */
public static byte []
toDNS(DSAParams params, byte [] sig)
throws SignatureException
{
	int rLength, sLength;
	int rOffset, sOffset;
	if ((sig[0] != ASN1_SEQ) || (sig[2] != ASN1_INT))
		throw new SignatureException("Expected SEQ, INT");
	rLength = sig[3];
	rOffset = 4;
	if (sig[rOffset] == 0) {
		rLength--;
		rOffset++;
	}
	if (sig[rOffset+rLength] != ASN1_INT)
		throw new SignatureException("Expected INT");
	sLength = sig[rOffset + rLength + 1];
	sOffset = rOffset + rLength + 2;
	if (sig[sOffset] == 0) {
		sLength--;
		sOffset++;
	}

	if ((rLength > 20) || (sLength > 20))
		throw new SignatureException("DSA R/S too long");

	byte[] newSig = new byte[41];
	Arrays.fill(newSig, (byte) 0);
	newSig[0] = (byte) ((params.getP().bitLength() - 512)/64);
	System.arraycopy(sig, rOffset, newSig, 1 + (20 - rLength), rLength);
	System.arraycopy(sig, sOffset, newSig, 21 + (20 - sLength), sLength);
	return newSig;
}
    
}

