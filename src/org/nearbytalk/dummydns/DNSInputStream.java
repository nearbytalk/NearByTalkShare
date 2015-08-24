package org.nearbytalk.dummydns;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;

/**
 * Class that provides methods to decode the data returned in a DNS response.
 *<p>
 * It is a modification of code from:
 *<PRE>
 *       Java Network Programming, Second Edition
 *       Merlin Hughes, Michael Shoffner, Derek Hamner
 *       Manning Publications Company; ISBN 188477749X
 *</PRE>
 *
 * The original code is Copyright (c) 1997-1999 
 * Merlin Hughes, Michael Shoffner, Derek Hamner; all rights reserved;
 * see jnp-license.txt for details.
 *<p>
 * Modifications were made by Lawrie Brown to support the use of this class
 * by the DummyDNS server program, which is handling the server end of the
 * DNS lookup request. Hence the code to parse the RR fields has been deleted.
 *
 * @see <a href="http://nitric.com/jnp/">Java Network Programming</a>
 * @see <a href="http://www.iana.org/assignments/dns-parameters">IANA DNS parameter registry</a>
 */

public class DNSInputStream extends ByteArrayInputStream {

  protected DataInputStream dataIn;

  /**
   * construct new reader of a DNS input stream given buffer details to process.
   *
   * @param data buffer containing DNS request packet contents
   * @param off offset into buffer (usually 0)
   * @param len length of buffer
   */
  public DNSInputStream (byte[] data, int off, int len) {
    super (data, off, len);
    dataIn = new DataInputStream (this);
  }

  /**
   * read next byte from buffer
   * @return byte read
   */
  public int readByte () throws IOException {
    return dataIn.readUnsignedByte ();
  }

  /**
   * read next short int from buffer in network byte order
   * @return short read
   */
  public int readShort () throws IOException {
    return dataIn.readUnsignedShort ();
  }

  /**
   * read next integer from buffer in network byte order
   * @return int read
   */
  public long readInt () throws IOException {
    return dataIn.readInt () & 0xffffffffL;
  }

  /**
   * read string from buffer, with byte length first then contents
   * @return string read
   */
  public String readString () throws IOException {
    int len = readByte ();
    if (len == 0) {
      return "";
    } else {
      byte[] buffer = new byte[len];
      dataIn.readFully (buffer);
      return new String (buffer, "latin1");
    }
  }

  /**
   * read DNS domain name from buffer, formatted as per DNS query specs
   * @return DNS domain name read
   */
  public String readDomainName () throws IOException {
    if (pos >= count)
      throw new EOFException ("EOF reading domain name");
    if ((buf[pos] & 0xc0) == 0) {
      String label = readString ();
      if (label.length () > 0) {
        String tail = readDomainName ();
        if (tail.length () > 0)
          label = label + '.' + tail;
      }
      return label;
    } else {
      if ((buf[pos] & 0xc0) != 0xc0)
        throw new IOException ("Invalid domain name compression offset");
      int offset = readShort () & 0x3fff;
      DNSInputStream dnsIn =
        new DNSInputStream (buf, offset, buf.length - offset);
      return dnsIn.readDomainName ();
    }
  }

  /**
   * read (partially) response record from buffer.
   * This routine has been severely pruned as it is not used in this package.
   * Having been called once, the buffer is no longer usuable, as the RR has
   * not been completely read.
   */
  public void readRR () throws IOException {
    String rrName = readDomainName ();
    int rrType = readShort ();
    int rrClass = readShort ();
    long rrTTL = readInt ();
    int rrDataLen = readShort ();
    // nb. leaving remainder of RR unread, not of interest!
  }
}
