// Copyright (c) 1999 Brian Wellington (bwelling@xbill.org)
// Portions Copyright (c) 1999 Network Associates, Inc.

package org.xbill.DNS;

import java.io.*;
import java.text.*;
import java.util.*;
import org.xbill.DNS.utils.*;

/**
 * A representation of a domain name. 
 *
 * @author Brian Wellington
 */

public class Name {

private static final int LABEL_NORMAL = 0;
private static final int LABEL_COMPRESSION = 0xC0;
private static final int LABEL_EXTENDED = 0x40;
private static final int LABEL_MASK = 0xC0;

private static final int EXT_LABEL_BITSTRING = 1;

private Object [] name;
private byte labels;
private boolean qualified;

/** The root name */
public static final Name root = Name.fromStringNoValidate(".", null);

/** The maximum number of labels in a Name */
static final int MAXLABELS = 128;

/* The number of labels initially allocated. */
private static final int STARTLABELS = 4;

/* Used for printing non-printable characters */
private static DecimalFormat byteFormat = new DecimalFormat();

static {
	byteFormat.setMinimumIntegerDigits(3);
}

private
Name() {
}

private final void
grow(int n) {
	if (n > MAXLABELS)
		throw new ArrayIndexOutOfBoundsException("name too long");
	Object [] newarray = new Object[n];
	System.arraycopy(name, 0, newarray, 0, labels);
	name = newarray;
}

private final void
grow() {
	grow(labels * 2);
}

private final void
compact() {
	for (int i = labels - 1; i > 0; i--) {
		if (!(name[i] instanceof BitString) ||
		    !(name[i - 1] instanceof BitString))
		    	continue;
		BitString bs = (BitString) name[i];
		BitString bs2 = (BitString) name[i - 1];
		if (bs.nbits == 256)
			continue;
		int nbits = bs.nbits + bs2.nbits;
		bs.join(bs2);
		if (nbits <= 256) {
			System.arraycopy(name, i, name, i - 1, labels - i);
			labels--;
		}
	}
}

/**
 * Create a new name from a string and an origin
 * @param s  The string to be converted
 * @param origin  If the name is unqualified, the origin to be appended
 * @deprecated As of dnsjava 1.3.0, * replaced by <code>Name.fromString</code>.
 */
public
Name(String s, Name origin) {
	Name n;
	try {
		n = Name.fromString(s, origin);
	}
	catch (TextParseException e) {
		StringBuffer sb = new StringBuffer();
		sb.append(s);
		if (origin != null) {
			sb.append(".");
			sb.append(origin);
		}
		sb.append(": ");
		sb.append(e.getMessage());
		System.err.println(sb.toString());
		name = null;
		labels = 0;
		return;
	}
	labels = n.labels;
	name = n.name;
	qualified = n.qualified;
	if (!qualified) {
		/*
		 * This isn't exactly right, but it's close.
		 * Partially qualified names are evil.
		 */
		if (Options.check("pqdn"))
			qualified = false;
		else
			qualified = (labels > 1);
	}
}

/**
 * Create a new name from a string
 * @param s  The string to be converted
 * @deprecated as of dnsjava 1.3.0, replaced by <code>Name.fromString</code>.
 */
public
Name(String s) {
	this (s, null);
}

/**
 * Create a new name from a string and an origin
 * @param s  The string to be converted
 * @param origin  If the name is unqualified, the origin to be appended.
 * @throws TextParseException The name is invalid.
 */
public static Name
fromString(String s, Name origin) throws TextParseException {
	Name name = new Name();
	name.labels = 0;
	name.name = new Object[1];
	boolean seenBitString = false;

	if (s.equals("@")) {
		if (origin != null)
			name.append(origin);
		name.qualified = true;
		return name;
	} else if (s.equals(".")) {
		name.qualified = true;
		return name;
	}
	int labelstart = -1;
	int pos = 0;
	byte [] label = new byte[64];
	boolean escaped = false;
	int digits = 0;
	int intval = 0;
	boolean bitstring = false;
	for (int i = 0; i < s.length(); i++) {
		byte b = (byte) s.charAt(i);
		if (escaped) {
			if (pos == 0 && b == '[')
				bitstring = true;
			if (b >= '0' && b <= '9' && digits < 3) {
				digits++;
				intval *= 10 + (b - '0');
				intval += (b - '0');
				if (digits < 3)
					continue;
				b = (byte) intval;
			}
			else if (digits > 0 && digits < 3)
				throw new TextParseException("bad escape");
			if (pos >= label.length)
				throw new TextParseException("label too long");
			label[pos++] = b;
			escaped = false;
		} else if (b == '\\') {
			escaped = true;
			digits = 0;
			intval = 0;
		} else if (b == '.') {
			if (labelstart == -1)
				throw new TextParseException("invalid label");
			byte [] newlabel = new byte[pos];
			System.arraycopy(label, 0, newlabel, 0, pos);
			if (name.labels == MAXLABELS)
				throw new TextParseException("too many labels");
			if (name.labels == name.name.length)
				name.grow();
			if (bitstring) {
				bitstring = false;
				name.name[name.labels++] =
						new BitString(newlabel);
			}
			else
				name.name[name.labels++] = newlabel;
			labelstart = -1;
			pos = 0;
		} else {
			if (labelstart == -1)
				labelstart = i;
			if (pos >= label.length)
				throw new TextParseException("label too long");
			label[pos++] = b;
		}
	}
	if (labelstart == -1)
		name.qualified = true;
	else {
		byte [] newlabel = new byte[pos];
		System.arraycopy(label, 0, newlabel, 0, pos);
		if (name.labels == MAXLABELS)
			throw new TextParseException("too many labels");
		if (name.labels == name.name.length)
			name.grow();
		if (bitstring) {
			bitstring = false;
			name.name[name.labels++] = new BitString(newlabel);
		}
		else
			name.name[name.labels++] = newlabel;
	}
	if (!name.qualified && origin != null)
		name.append(origin);
	if (seenBitString)
		name.compact();
	return (name);
}

/*
 * Convert a name from a string, knowing that it will succeed.
 */
static Name
fromStringNoValidate(String s, Name origin) {
	try {
		return fromString(s, origin);
	}
	catch (TextParseException e) {
		return null;
	}
}

/**
 * Create a new name from DNS wire format
 * @param in A stream containing the input data
 */
public
Name(DataByteInputStream in) throws IOException {
	int len, start, pos, count = 0, savedpos;
	Name name2;
	boolean seenBitString = false;

	labels = 0;
	name = new Object[STARTLABELS];

	start = in.getPos();
loop:
	while ((len = in.readUnsignedByte()) != 0) {
		count++;
		switch(len & LABEL_MASK) {
		case LABEL_NORMAL:
			byte [] b = new byte[len];
			in.read(b);
			if (labels == name.length)
				grow();
			name[labels++] = b;
			break;
		case LABEL_COMPRESSION:
			pos = in.readUnsignedByte();
			pos += ((len & ~LABEL_MASK) << 8);
			if (Options.check("verbosecompression"))
				System.err.println("currently " + in.getPos() +
						   ", pointer to " + pos);
			if (pos >= in.getPos())
				throw new WireParseException("bad compression");
			savedpos = in.getPos();
			in.setPos(pos);
			if (Options.check("verbosecompression"))
				System.err.println("current name '" + this +
						   "', seeking to " + pos);
			try {
				name2 = new Name(in);
			}
			finally {
				in.setPos(savedpos);
			}
			if (labels + name2.labels > name.length)
				grow(labels + name2.labels);
			System.arraycopy(name2.name, 0, name, labels,
					 name2.labels);
			labels += name2.labels;
			break loop;
		case LABEL_EXTENDED:
			int type = len & ~LABEL_MASK;
			switch (type) {
			case EXT_LABEL_BITSTRING:
				int bits = in.readUnsignedByte();
				if (bits == 0)
					bits = 256;
				int bytes = (bits + 7) / 8;
				byte [] data = new byte[bytes];
				in.read(data);
				if (labels == name.length)
					grow();
				name[labels++] = new BitString(bits, data);
				seenBitString = true;
				break;
			default:
				throw new WireParseException(
						"Unknown name format");
			} /* switch */
			break;
		} /* switch */
	}
	qualified = true;

	if (seenBitString)
		compact();
}

/**
 * Create a new name by removing labels from the beginning of an existing Name
 * @param d  An existing Name
 * @param n  The number of labels to remove from the beginning in the copy
 */
/* Skips n labels and creates a new name */
public
Name(Name d, int n) {
	name = new Object[d.labels - n];

	labels = (byte) (d.labels - n);
	System.arraycopy(d.name, n, name, 0, labels);
	qualified = d.qualified;
}

/**
 * Generates a new Name with the first n labels replaced by a wildcard 
 * @return The wildcard name
 */
public Name
wild(int n) {
	Name wild = new Name(this, n - 1);
	wild.name[0] = new byte[] {(byte)'*'};
	return wild;
}

/**
 * Generates a new Name to be used when following a DNAME.
 * @return The new name, or null if the DNAME is invalid.
 */
public Name
fromDNAME(DNAMERecord dname) {
	Name dnameowner = dname.getName();
	Name dnametarget = dname.getTarget();
	int nlabels;
	int saved;
	if (!subdomain(dnameowner))
		return null;
	saved = labels - dnameowner.labels;
	nlabels = saved + dnametarget.labels;
	if (nlabels > MAXLABELS)
		return null;
	Name newname = new Name();
	newname.labels = (byte)nlabels;
	newname.name = new Object[labels];
	System.arraycopy(this.name, 0, newname.name, 0, saved);
	System.arraycopy(dnametarget.name, 0, newname.name, saved,
			 dnametarget.labels);
	newname.qualified = true;
	newname.compact();
	return newname;
}

/**
 * Is this name a wildcard?
 */
public boolean
isWild() {
	if (labels == 0 || (name[0] instanceof BitString))
		return false;
	byte [] b = (byte []) name[0];
	return (b.length == 1 && b[0] == '*');
}

/**
 * Is this name fully qualified?
 */
public boolean
isQualified() {
	return qualified;
}

/**
 * Appends the specified name to the end of the current Name
 */
public void
append(Name d) {
	if (labels + d.labels > name.length)
		grow(labels + d.labels);
	System.arraycopy(d.name, 0, name, labels, d.labels);
	labels += d.labels;
	qualified = d.qualified;
	compact();
}

/**
 * The length
 */
public short
length() {
	short total = 0;
	for (int i = 0; i < labels; i++) {
		if (name[i] instanceof BitString)
			total += (((BitString)name[i]).bytes() + 2);
		else
			total += (((byte [])name[i]).length + 1);
	}
	return ++total;
}

/**
 * The number of labels
 */
public byte
labels() {
	return labels;
}

/**
 * Is the current Name a subdomain of the specified name?
 */
public boolean
subdomain(Name domain) {
	if (domain == null || domain.labels > labels)
		return false;
	Name tname = new Name(this, labels - domain.labels);
	return (tname.equals(domain));
}

private String
byteString(byte [] array) {
	StringBuffer sb = new StringBuffer();
	for (int i = 0; i < array.length; i++) {
		/* Ick. */
		short b = (short)(array[i] & 0xFF);
		if (b <= 0x20 || b >= 0x7f) {
			sb.append('\\');
			sb.append(byteFormat.format(b));
		}
		else if (b == '"' || b == '(' || b == ')' || b == '.' ||
			 b == ';' || b == '\\' || b == '@' || b == '$')
		{
			sb.append('\\');
			sb.append((char)b);
		}
		else
			sb.append((char)b);
	}
	return sb.toString();
}

/**
 * Convert Name to a String
 */
public String
toString() {
	StringBuffer sb = new StringBuffer();
	if (labels == 0)
		sb.append(".");
	for (int i = 0; i < labels; i++) {
		if (name[i] instanceof BitString)
			sb.append(name[i]);
		else
			sb.append(byteString((byte [])name[i]));
		if (qualified || i < labels - 1)
			sb.append(".");
	}
	return sb.toString();
}

/**
 * Convert the nth label in a Name to a String
 * @param n  The label to be converted to a String
 */
public String
getLabelString(int n) {
	if (name[n] instanceof BitString)
		return name[n].toString();
	else
		return byteString((byte [])name[n]);
}

/**
 * Convert Name to DNS wire format
 */
public void
toWire(DataByteOutputStream out, Compression c) throws IOException {
	for (int i = 0; i < labels; i++) {
		Name tname;
		if (i == 0)
			tname = this;
		else
			tname = new Name(this, i);
		int pos = -1;
		if (c != null) {
			pos = c.get(tname);
			if (Options.check("verbosecompression"))
				System.err.println("Looking for " + tname +
						   ", found " + pos);
		}
		if (pos >= 0) {
			pos |= (LABEL_MASK << 8);
			out.writeShort(pos);
			return;
		}
		else {
			if (c != null) {
				c.add(out.getPos(), tname);
				if (Options.check("verbosecompression"))
					System.err.println("Adding " + tname +
							   " at " +
							   out.getPos());
			}
			if (name[i] instanceof BitString) {
				out.writeByte(LABEL_EXTENDED |
					      EXT_LABEL_BITSTRING);
				out.writeByte(((BitString)name[i]).wireBits());
				out.write(((BitString)name[i]).data);
			}
			else
				out.writeString((byte []) name[i]);
		}
	}
	out.writeByte(0);
}

/**
 * Convert Name to canonical DNS wire format (all lowercase)
 */
public void
toWireCanonical(DataByteOutputStream out) throws IOException {
	for (int i = 0; i < labels; i++) {
		if (name[i] instanceof BitString) {
			out.writeByte(LABEL_EXTENDED | EXT_LABEL_BITSTRING);
			out.writeByte(((BitString)name[i]).wireBits());
			out.write(((BitString)name[i]).data);
		}
		else
			out.writeStringCanonical(new String((byte []) name[i]));
	}
	out.writeByte(0);
}

private static final byte
toLower(byte b) {
	if (b < 'A' || b > 'Z')
		return b;
	else
		return (byte)(b - 'A' + 'a');
}

/**
 * Are these two Names equivalent?
 */
public boolean
equals(Object arg) {
	if (arg == null || !(arg instanceof Name))
		return false;
	if (arg == this)
		return true;
	Name d = (Name) arg;
	if (d.labels != labels)
		return false;
	for (int i = 0; i < labels; i++) {
		if (name[i].getClass() != d.name[i].getClass())
			return false;
		if (name[i] instanceof BitString) {
			if (!name[i].equals(d.name[i]))
				return false;
		}
		else {
			byte [] b1 = (byte []) name[i];
			byte [] b2 = (byte []) d.name[i];
			if (b1.length != b2.length)
				return false;
			for (int j = 0; j < b1.length; j++) {
				if (toLower(b1[j]) != toLower(b2[j]))
					return false;
			}
		}
	}
	return true;
}

/**
 * Computes a hashcode based on the value
 */
public int
hashCode() {
	int code = labels;
	for (int i = 0; i < labels; i++) {
		if (name[i] instanceof BitString) {
			BitString b = (BitString) name[i];
			for (int j = 0; j < b.bytes(); j++)
				code += ((code << 3) + b.data[j]);
		}
		else {
			byte [] b = (byte []) name[i];
			for (int j = 0; j < b.length; j++)
				code += ((code << 3) + toLower(b[j]));
		}
	}
	return code;
}

public int
compareTo(Object o) {
	Name arg = (Name) o;

	int compares = labels > arg.labels ? arg.labels : labels;

	for (int i = 1; i <= compares; i++) {
		Object label = name[labels - i];
		Object alabel = arg.name[arg.labels - i];

		if (label.getClass() != alabel.getClass()) {
			if (label instanceof BitString)
				return (-1);
			else
				return (1);
		}
		if (label instanceof BitString) {
			BitString bs = (BitString)label;
			BitString abs = (BitString)alabel;
			int bits = bs.nbits > abs.nbits ? abs.nbits : bs.nbits;
			int n = bs.compareBits(abs, bits);
			if (n != 0)
				return (n);
			if (bs.nbits == abs.nbits)
				continue;

			/*
			 * If label X has more bits than label Y, then the
			 * name with X is greater if Y is the first label
			 * of its name.  Otherwise, the name with Y is greater.
			 */
			if (bs.nbits > abs.nbits)
				return (i == arg.labels ? 1 : -1);
			else
				return (i == labels ? -1 : 1);
		}
		else {
			byte [] b = (byte []) label;
			byte [] ab = (byte []) alabel;
			for (int j = 0; j < b.length && j < ab.length; j++) {
				int n = toLower(b[j]) - toLower(ab[j]);
				if (n != 0)
					return (n);
			}
			if (b.length != ab.length)
				return (b.length - ab.length);
		}
	}
	return (labels - arg.labels);
}

}
