// Copyright (c) 1999-2003 Brian Wellington (bwelling@xbill.org)

package org.xbill.DNS;

import java.io.*;
import java.util.*;
import org.xbill.DNS.utils.*;

/**
 * Options - describes Extended DNS (EDNS) properties of a Message.
 * No specific options are defined other than those specified in the
 * header.  An OPT should be generated by Resolver.
 *
 * EDNS is a method to extend the DNS protocol while providing backwards
 * compatibility and not significantly changing the protocol.  This
 * implementation of EDNS is mostly complete at level 0.
 *
 * @see Message
 * @see Resolver 
 *
 * @author Brian Wellington
 */

public class OPTRecord extends Record {

private static OPTRecord member = new OPTRecord();

private Map options;

private
OPTRecord() {}

private
OPTRecord(Name name, int dclass, long ttl) {
	super(name, Type.OPT, dclass, ttl);
}

static OPTRecord
getMember() {
	return member;
}

/**
 * Creates an OPT Record with no data.  This is normally called by
 * SimpleResolver, but can also be called by a server.
 */
public
OPTRecord(int payloadSize, byte xrcode, byte version, int flags) {
	this(Name.root, payloadSize,
	      ((int)xrcode << 24) + ((int)version << 16) + flags);
	checkU16("payloadSize", payloadSize);
	checkU8("xrcode", xrcode);
	checkU8("version", version);
	checkU16("flags", flags);
	options = null;
}

/**
 * Creates an OPT Record with no data.  This is normally called by
 * SimpleResolver, but can also be called by a server.
 */
public
OPTRecord(int payloadSize, byte xrcode, byte version) {
	this(payloadSize, xrcode, version, 0);
}

Record
rrFromWire(Name name, int type, int dclass, long ttl, int length,
	   DataByteInputStream in)
throws IOException
{
	OPTRecord rec = new OPTRecord(name, dclass, ttl);
	if (in == null)
		return rec;
	int count = 0;
	if (count < length)
		rec.options = new HashMap();
	while (count < length) {
		int code = in.readUnsignedShort();
		int len = in.readUnsignedShort();
		byte [] data = new byte[len];
		in.read(data);
		count += (4 + len);
		rec.options.put(new Integer(code), data);
	}
	return rec;
}

Record
rdataFromString(Name name, int dclass, long ttl, Tokenizer st, Name origin)
throws IOException
{
	throw st.exception("no text format defined for OPT");
}

/** Converts rdata to a String */
public String
rdataToString() {
	StringBuffer sb = new StringBuffer();
	sb.append(getName());
	sb.append("\t");
	sb.append(Type.string(getType()));
	if (options != null) {
		Iterator it = options.keySet().iterator();
		while (it.hasNext()) {
			Integer i = (Integer) it.next();
			sb.append(i + " ");
		}
	}
	sb.append(" ; payload ");
	sb.append(getPayloadSize());
	sb.append(", xrcode ");
	sb.append(getExtendedRcode());
	sb.append(", version ");
	sb.append(getVersion());
	sb.append(", flags ");
	sb.append(getFlags());
	return sb.toString();
}

/** Returns the maximum allowed payload size. */
public int
getPayloadSize() {
	return dclass;
}

/**
 * Returns the extended Rcode
 * @see Rcode
 */
public int
getExtendedRcode() {
	return (int)(ttl >>> 24);
}

/** Returns the highest supported EDNS version */
public int
getVersion() {
	return (int)((ttl >>> 16) & 0xFF);
}

/** Returns the EDNS flags */
public int
getFlags() {
	return (int)(ttl & 0xFFFF);
}

void
rrToWire(DataByteOutputStream out, Compression c, boolean canonical) {
	if (options == null)
		return;
	Iterator it = options.keySet().iterator();
	while (it.hasNext()) {
		Integer i = (Integer) it.next();
		out.writeShort(i.intValue());
		byte [] data = (byte []) options.get(i);
		out.writeShort(data.length);
		out.writeArray(data);
	}
}

}
