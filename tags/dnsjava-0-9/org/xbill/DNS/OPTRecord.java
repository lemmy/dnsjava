// Copyright (c) 1999 Brian Wellington (bwelling@xbill.org)
// Portions Copyright (c) 1999 Network Associates, Inc.

package DNS;

import java.io.*;
import java.util.*;
import DNS.utils.*;

/**
 * Options - describes extended DNS (EDNS) properties of a Message.
 * No specific options are defined other than those specified in the
 * header.  An OPT should be generated by Resolver.
 * @see EDNS
 * @see Message
 * @see Resolver
 */

public class OPTRecord extends Record {

private Hashtable options;

private
OPTRecord() {}

public
OPTRecord(Name _name, short _dclass, int _ttl) {
	super(_name, Type.OPT, _dclass, _ttl);
	options = null;
}

public
OPTRecord(Name _name, short _dclass, int _ttl,
	  int length, DataByteInputStream in, Compression c)
throws IOException
{
	super(_name, Type.OPT, _dclass, _ttl);
	if (in == null)
		return;
	int count = 0;
	if (count < length)
		options = new Hashtable();
	while (count < length) {
		int code = in.readUnsignedShort();
		int len = in.readUnsignedShort();
		byte [] data = new byte[len];
		in.read(data);
		count += (4 + len);
		options.put(new Integer(code), data);
	}
}

/** Converts to a String */
public String
toString() {
	StringBuffer sb = toStringNoData();
	if (options != null) {
		Enumeration e = options.keys();
		while (e.hasMoreElements()) {
			Integer i = (Integer) e.nextElement();
			sb.append(i + " ");
		}
	}
	return sb.toString();
}

/** Returns the maximum allowed payload size. */
public short
getPayloadSize() {
	return dclass;
}

/**
 * Returns the extended Rcode
 * @see Rcode
 */
public short
getExtendedRcode() {
	return (short) (ttl >>> 24);
}

/** Returns the highest support EDNS version */
public short
getVersion() {
	return (short) ((ttl >>> 16) & 0xFF);
}

void
rrToWire(DataByteOutputStream out, Compression c) throws IOException {
	if (options == null)
		return;
	Enumeration e = options.keys();
	while (e.hasMoreElements()) {
		Integer i = (Integer) e.nextElement();
		short key = i.shortValue();
		out.writeShort(key);
		byte [] data = (byte []) options.get(i);
		out.writeShort(data.length);
		out.write(data);
	}
}

void
rrToWireCanonical(DataByteOutputStream out) throws IOException {
	throw new IOException("An OPT should never be converted to canonical");
}

}
