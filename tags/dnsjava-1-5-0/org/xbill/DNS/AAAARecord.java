// Copyright (c) 1999-2003 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import org.xbill.DNS.utils.*;

/**
 * IPv6 Address Record - maps a domain name to an IPv6 address
 *
 * @author Brian Wellington
 */

public class AAAARecord extends Record {

private static AAAARecord member = new AAAARecord();

private Inet6Address address;

private
AAAARecord() {}

private
AAAARecord(Name name, int dclass, long ttl) {
	super(name, Type.AAAA, dclass, ttl);
}

static AAAARecord
getMember() {
	return member;
}

/**
 * Creates an AAAA Record from the given data
 * @param address The address suffix
 */
public
AAAARecord(Name name, int dclass, long ttl, Inet6Address address) {
	this(name, dclass, ttl);
	this.address = address;
}

Record
rrFromWire(Name name, int type, int dclass, long ttl, int length,
	   DataByteInputStream in)
throws IOException
{
	AAAARecord rec = new AAAARecord(name, dclass, ttl);

	if (in == null)
		return rec;

	byte [] data = new byte[16];
	in.read(data);
	rec.address = new Inet6Address(data);
	return rec;
}

Record
rdataFromString(Name name, int dclass, long ttl, Tokenizer st, Name origin)
throws IOException
{
	AAAARecord rec = new AAAARecord(name, dclass, ttl);
	try {
		rec.address = new Inet6Address(st.getString());
	}
	catch (TextParseException e) {
		throw st.exception(e.getMessage());
	}
	return rec;
}

/** Converts rdata to a String */
public String
rdataToString() {
	StringBuffer sb = new StringBuffer();
	if (address != null)
		sb.append(address);
	return sb.toString();
}

/** Returns the address */
public Inet6Address
getAddress() {
	return address;
}

void
rrToWire(DataByteOutputStream out, Compression c, boolean canonical) {
	if (address == null)
		return;
	byte [] b = address.toBytes();
	out.writeArray(b);
}

}
