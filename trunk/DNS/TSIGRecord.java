// Copyright (c) 1999 Brian Wellington (bwelling@anomaly.munge.com)
// Portions Copyright (c) 1999 Network Associates, Inc.

import java.io.*;
import java.util.*;
import java.text.*;

public class dnsTSIGRecord extends dnsRecord {

dnsName alg;
Date timeSigned;
short fudge;
byte [] signature;
int originalID;
short error;
byte [] other;

public
dnsTSIGRecord(dnsName _name, short _dclass, int _ttl, dnsName _alg,
	      Date _timeSigned, short _fudge, byte [] _signature,
	      int _originalID, short _error, byte _other[]) throws IOException
{
	super(_name, dns.TSIG, _dclass, _ttl);
	alg = _alg;
	timeSigned = _timeSigned;
	fudge = _fudge;
	signature = _signature;
	originalID = _originalID;
	error = _error;
	other = _other;
}

public
dnsTSIGRecord(dnsName _name, short _dclass, int _ttl, int length,
	      CountedDataInputStream in, dnsCompression c) throws IOException
{
	super(_name, dns.TSIG, _dclass, _ttl);
	if (in == null)
		return;
	alg = new dnsName(in, c);

	short timeHigh = in.readShort();
	int timeLow = in.readInt();
	long time = ((long)timeHigh & 0xFFFF) << 32;
	time += (long)timeLow & 0xFFFFFFFF;
	timeSigned = new Date(time * 1000);
	fudge = in.readShort();

	int sigLen = in.readUnsignedShort();
	signature = new byte[sigLen];
	in.read(signature);

	originalID = in.readUnsignedShort();
	error = in.readShort();

	int otherLen = in.readUnsignedShort();
	if (otherLen > 0) {
		other = new byte[otherLen];
		in.read(other);
	}
	else
		other = null;
}

public String
toString() {
	StringBuffer sb = toStringNoData();
	if (alg == null)
		return sb.toString();

	sb.append(alg);
	sb.append(" (\n\t");

	sb.append (timeSigned.getTime() / 1000);
	sb.append (" ");
	sb.append (dns.rcodeString(error));
	sb.append ("\n");
	String s = base64.toString(signature);
	sb.append (dnsIO.formatBase64String(s, 64, "\t", false));
	if (other != null) {
		sb.append("\n\t <");
		if (error == dns.BADTIME) {
			try {
				ByteArrayInputStream is;
				is = new ByteArrayInputStream(other);
				DataInputStream ds = new DataInputStream(is);
				long time = ds.readUnsignedShort();
				time <<= 32;
				time += ((long)ds.readInt() & 0xFFFFFFFF);
				sb.append("Server time: ");
				sb.append(new Date(time * 1000));
			}
			catch (IOException e) {
				sb.append("Truncated BADTIME other data");
			}
		}
		else
			sb.append(base64.toString(other));
		sb.append(">");
	}
	sb.append(" )");
	return sb.toString();
}

public dnsName
getAlg() {
	return alg;
}

public Date
getTimeSigned() {
	return timeSigned;
}

public short
getFudge() {
	return fudge;
}

public byte []
getSignature() {
	return signature;
}

public int
getOriginalID() {
	return originalID;
}

public short
getError() {
	return error;
}

public byte []
getOther() {
	return other;
}

byte []
rrToWire(dnsCompression c) throws IOException {
	ByteArrayOutputStream bs = new ByteArrayOutputStream();
	CountedDataOutputStream ds = new CountedDataOutputStream(bs);

	alg.toWire(ds, null);

	long time = timeSigned.getTime() / 1000;
	short timeHigh = (short) (time >> 32);
	int timeLow = (int) (time);
	ds.writeShort(timeHigh);
	ds.writeInt(timeLow);
	ds.writeShort(fudge);

	ds.writeShort((short)signature.length);
	ds.write(signature);

	ds.writeShort(originalID);
	ds.writeShort(error);

	if (other != null) {
		ds.writeShort((short)other.length);
		ds.write(other);
	}
	else
		ds.writeShort(0);
	return bs.toByteArray();
}

}
