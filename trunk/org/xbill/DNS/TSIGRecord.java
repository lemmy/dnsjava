import java.net.*;
import java.io.*;
import java.util.*;
import java.text.*;

public class dnsTSIGRecord extends dnsRecord {

dnsName alg;
Date timeSigned;
short fudge;
byte [] signature;
short error;
byte [] other;

public dnsTSIGRecord(dnsName rname, short rclass) {
	super(rname, dns.TSIG, rclass);
}

public dnsTSIGRecord(dnsName rname, short rclass, int rttl, dnsName alg,
		     Date timeSigned, short fudge, byte [] signature,
		     short error, byte other[])
{
	this(rname, rclass);
	this.rttl = rttl;
	this.alg = alg;
	this.timeSigned = timeSigned;
	this.fudge = fudge;
	this.signature = signature;
	this.error = error;
	this.other = other;
	this.rlength = (short) (14 + alg.length() + signature.length);
	if (other != null)
		this.rlength += other.length;
}

void parse(CountedDataInputStream in, dnsCompression c) throws IOException {
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

	error = in.readShort();

	int otherLen = in.readUnsignedShort();
	if (otherLen > 0) {
		other = new byte[otherLen];
		in.read(other);
	}
	else
		other = null;
}

void rrToBytes(DataOutputStream out) throws IOException {
	alg.toBytes(out);
	long time = timeSigned.getTime() / 1000;
	short timeHigh = (short) (time >> 32);
	int timeLow = (int) (time);
	out.writeShort(timeHigh);
	out.writeInt(timeLow);
	out.writeShort(fudge);

	out.writeShort((short)signature.length);
	out.write(signature);

	out.writeShort(error);

	if (other != null) {
		out.writeShort((short)other.length);
		out.write(other);
	}
	else
		out.writeShort(0);
}

void rrToCanonicalBytes(DataOutputStream out) throws IOException {
	alg.toCanonicalBytes(out);
	long time = timeSigned.getTime() / 1000;
	short timeHigh = (short) (time >> 32);
	int timeLow = (int) (time);
	out.writeShort(timeHigh);
	out.writeInt(timeLow);
	out.writeShort(fudge);

	out.writeShort((short)signature.length);
	out.write(signature);

	out.writeShort(error);

	if (other != null) {
		out.writeShort((short)other.length);
		out.write(other);
	}
	else
		out.writeShort(0);
}

String rrToString() {
	if (rlength == 0)
		return null;
	StringBuffer sb = new StringBuffer();
	sb.append (alg);
	sb.append (" (\n\t");
	sb.append (timeSigned.getTime() / 1000);
	sb.append (" ");
	sb.append (dns.rcodeString(error));
	sb.append (" ");
	String s = base64.toString(signature);
	for (int i = 0; i < s.length(); i += 64) {
		if (i != 0)
			sb.append ("\n\t");
		if (i + 64 >= s.length())
			sb.append(s.substring(i));
		else
			sb.append(s.substring(i, i+64));
	}
	if (other != null) {
		sb.append("\n\t <");
		sb.append(other);
		sb.append("\n\t >");
	}
	sb.append(" )");
	return sb.toString();
}

}
