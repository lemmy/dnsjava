// Copyright (c) 1999 Brian Wellington (bwelling@xbill.org)
// Portions Copyright (c) 1999 Network Associates, Inc.

package org.xbill.DNS;

import java.io.*;
import java.util.*;
import org.xbill.DNS.utils.*;

/**
 * Transaction Signature - this record is automatically generated by the
 * resolver.  TSIG records provide transaction security between the
 * sender and receiver of a message, using a shared key.
 * @see Resolver
 * @see TSIG
 *
 * @author Brian Wellington
 */

public class TSIGRecord extends Record {

private static TSIGRecord member = new TSIGRecord();

private Name alg;
private Date timeSigned;
private int fudge;
private byte [] signature;
private int originalID;
private short error;
private byte [] other;

private
TSIGRecord() {} 

private
TSIGRecord(Name name, int dclass, long ttl) {
	super(name, Type.TSIG, dclass, ttl);
}

static TSIGRecord
getMember() {
	return member;
}

/**
 * Creates a TSIG Record from the given data.  This is normally called by
 * the TSIG class
 * @param alg The shared key's algorithm
 * @param timeSigned The time that this record was generated
 * @param fudge The fudge factor for time - if the time that the message is
 * received is not in the range [now - fudge, now + fudge], the signature
 * fails
 * @param signature The signature
 * @param originalID The message ID at the time of its generation
 * @param error The extended error field.  Should be 0 in queries.
 * @param other The other data field.  Currently used only in BADTIME
 * responses.
 * @see TSIG
 */
public
TSIGRecord(Name name, int dclass, long ttl, Name alg, Date timeSigned,
	   int fudge, byte [] signature, int originalID, short error,
	   byte other[])
{
	this(name, dclass, ttl);
	if (!alg.isAbsolute())
		throw new RelativeNameException(alg);
	checkU16("fudge", fudge);
	this.alg = alg;
	this.timeSigned = timeSigned;
	this.fudge = fudge;
	this.signature = signature;
	this.originalID = originalID;
	this.error = error;
	this.other = other;
}

Record
rrFromWire(Name name, int type, int dclass, long ttl, int length,
	   DataByteInputStream in)
throws IOException
{
	TSIGRecord rec = new TSIGRecord(name, dclass, ttl);
	if (in == null)
		return rec;
	rec.alg = new Name(in);

	long timeHigh = in.readUnsignedShort();
	long timeLow = in.readUnsignedInt();
	long time = (timeHigh << 32) + timeLow;
	rec.timeSigned = new Date(time * 1000);
	rec.fudge = in.readUnsignedShort();

	int sigLen = in.readUnsignedShort();
	rec.signature = new byte[sigLen];
	in.read(rec.signature);

	rec.originalID = in.readUnsignedShort();
	rec.error = in.readShort();

	int otherLen = in.readUnsignedShort();
	if (otherLen > 0) {
		rec.other = new byte[otherLen];
		in.read(rec.other);
	}
	else
		rec.other = null;
	return rec;
}

Record
rdataFromString(Name name, int dclass, long ttl, Tokenizer st, Name origin)
throws IOException
{
	throw st.exception("no text format defined for TSIG");
}

/** Converts rdata to a String */
public String
rdataToString() {
	StringBuffer sb = new StringBuffer();
	if (alg == null)
		return sb.toString();

	sb.append(alg);
	sb.append(" ");
	if (Options.check("multiline"))
		sb.append("(\n\t");

	sb.append (timeSigned.getTime() / 1000);
	sb.append (" ");
	sb.append (fudge);
	sb.append (" ");
	sb.append (signature.length);
	if (Options.check("multiline")) {
		sb.append ("\n");
		sb.append (base64.formatString(signature, 64, "\t", false));
	} else {
		sb.append (" ");
		sb.append (base64.toString(signature));
	}
	sb.append (Rcode.TSIGstring(error));
	sb.append (" ");
	if (other == null)
		sb.append (0);
	else {
		sb.append (other.length);
		if (Options.check("multiline"))
			sb.append("\n\n\n\t");
		else
			sb.append(" ");
		if (error == Rcode.BADTIME) {
			if (other.length != 6) {
				sb.append("<invalid BADTIME other data>");
			} else {
				long time = ((other[0] & 0xFF) << 40) +
					    ((other[1] & 0xFF) << 32) +
					    ((other[2] & 0xFF) << 24) +
					    ((other[3] & 0xFF) << 16) +
					    ((other[4] & 0xFF) << 8) +
					    ((other[5] & 0xFF)     );
				sb.append("<server time: ");
				sb.append(new Date(time * 1000));
				sb.append(">");
			}
		} else
			sb.append(base64.toString(other));
		sb.append(">");
	}
	if (Options.check("multiline"))
		sb.append(" )");
	return sb.toString();
}

/** Returns the shared key's algorithm */
public Name
getAlgorithm() {
	return alg;
}

/** Returns the time that this record was generated */
public Date
getTimeSigned() {
	return timeSigned;
}

/** Returns the time fudge factor */
public int
getFudge() {
	return fudge;
}

/** Returns the signature */
public byte []
getSignature() {
	return signature;
}

/** Returns the original message ID */
public int
getOriginalID() {
	return originalID;
}

/** Returns the extended error */
public short
getError() {
	return error;
}

/** Returns the other data */
public byte []
getOther() {
	return other;
}

void
rrToWire(DataByteOutputStream out, Compression c, boolean canonical) {
	if (alg == null)
		return;

	alg.toWire(out, null, canonical);

	long time = timeSigned.getTime() / 1000;
	short timeHigh = (short) (time >> 32);
	int timeLow = (int) (time);
	out.writeShort(timeHigh);
	out.writeInt(timeLow);
	out.writeShort(fudge);

	out.writeShort((short)signature.length);
	out.writeArray(signature);

	out.writeShort(originalID);
	out.writeShort(error);

	if (other != null) {
		out.writeShort((short)other.length);
		out.writeArray(other);
	}
	else
		out.writeShort(0);
}

}
