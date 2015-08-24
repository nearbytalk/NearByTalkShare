package org.nearbytalk.dummydns;


import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.StringTokenizer;

/**
 * Class that encapsulates the structure of DNS queries and responses and
 * provides functions to encode, decode and display their contents.
 *<p>
 * It is a heavy modification of code from:
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
 * DNS lookup request. Hence the focus is now on reading the original query
 * and extracting the question field, then subsequently creating a suitable
 * response packet (noting that we only handle Address type requests & responses).
 *
 * @see <a href="http://nitric.com/jnp/">Java Network Programming</a>
 * @see <a href="http://www.iana.org/assignments/dns-parameters">IANA DNS parameter registry</a>
 */

public class DNSQuery {

  /** address of the remote host making this DNS lookup query. */
  private InetAddress clientHost;
  /** port number on the remote host making this DNS lookup query. */
  private int clientPort;
  /** the number of queries/questions in the question part of the packet */
  private int numQueries;
  /** the number of answers in the answers part of the packet */
  private int numAnswers;
  /** the number of NS RR's in the authority part of the packet */
  private int numAuthorities;
  /** the number of additional RR's in the additional part of the packet */
  private int numAdditional;
  /** the unique query identifier value set by the client to identify replies. */
  private int queryID;
  /** the flags field in this DNS query. */
  private int queryFlags;
  /** the hostname for which information is being requested (strictly the first questions host). */
  private String queryHost;
  /** the type of information requested on the supplied hostname. */
  private int queryType;
  /** the class of address desired for the hostname (only INternet supported). */
  private int queryClass;
  /** the query opcode value (only QUERY supported). */
  private int opcode;
  /** specific individual flags extracted from the flags field. */
  private boolean authoritative, truncated, recurseRequest, recursive;
  /** the constant Time To Live (TTL) value sent with all valid replies. */
  private final int ttl = 43200;

  /** empty constructor. */
  public DNSQuery () {
  }

  /**
   *  populate this query with information from the next packet read.
   *<p>
   *  Reads a packet from the supplied socket (which should be bound to the
   *  well-known UDP DNS port 53), extracts the source host+port from it,
   *  then has it decoded into the relevant fields saved as class variables.
   *
   *  @param socket	the socket the packet will be read from (usually UDP 53)
   */
  public void getQuery (DatagramSocket socket) throws IOException {
    byte[] buffer = new byte[512];
    DatagramPacket packet = new DatagramPacket (buffer, buffer.length);
    socket.receive (packet);
    clientHost = packet.getAddress();
    clientPort = packet.getPort();
    decodeQuery (packet.getData (), packet.getLength ());
  }

  /**
   *  send a response to this query back to the requesting remote client host.
   *  Will construct a suitable response packet based on the supplied address info,
   *  and send it out the supplied socket.
   *<p>
   *  @param socket	the socket the response packet will be written through
   *  @param addr	the 4-byte IP address to be returned (null if name not known)
   */
  public void sendResponse (DatagramSocket socket, byte[] addr) throws IOException {
    byte[] data = genResponse (addr);
    DatagramPacket packet = new DatagramPacket (data, data.length, clientHost, clientPort);
    socket.send (packet);
  }

  /**
   *  internal routine to decode this DNS query from the packet buffer.
   *  Extracts the query header and first question details, ignoring
   *  any other information in the packet. The information extracted
   *  is saved in the relevant class variables.
   *
   *  @param data	buffer containing the packet contents
   *  @param length	the size of the packet in the buffer
   */
  private void decodeQuery (byte[] data, int length) throws IOException {
    DNSInputStream dnsIn = new DNSInputStream (data, 0, length);
    queryID = dnsIn.readShort ();
    queryFlags = dnsIn.readShort ();
    decodeFlags (queryFlags);
    int numQueries = dnsIn.readShort ();
    int numAnswers = dnsIn.readShort ();
    int numAuthorities = dnsIn.readShort ();
    int numAdditional = dnsIn.readShort ();
    if (numQueries > 0) { // extract first question
      queryHost = dnsIn.readDomainName ();
      queryType = dnsIn.readShort ();
      queryClass = dnsIn.readShort ();
    }
    // ignore remaining answers, authorities & additional sections
  }

  /**
   *  internal routine to construct a response to this query given the reply address.
   *<p>
   *  Constructs a suitable DNS response packet, containing the original question,
   *  and the answer address found to match (if any). If no match was found, the
   *  supplied address is null, and the response code is set to "name not known".
   *  The ID is echoed from the original query allowing the client to match this response.
   *  The flags are set to Response|Query|Authorative|No recursion.
   *  The response code is either: 0 = no error OR 3 = name not known.
   *  The (single) question is then repeated, followed by the answer if found.
   *
   *  @param addr	the 4-byte IP address to be returned (null if name not known)
   */
  private byte[] genResponse (byte[] addr) throws IOException {
    ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream ();
    DataOutputStream dataOut = new DataOutputStream (byteArrayOut);
    int rcode = 0;	// response code 0 is good
    int numAns = 1;	// number of answers
    int respFlags;	// response flags field

    // check that we can handle type & class combo
    if (((addr != null) && (queryType != DNS.TYPE_A)) && (queryType !=DNS.TYPE_ANY)|| (queryClass != DNS.CLASS_IN)) {
      throw new IOException ("Unsupported DNS query type " + queryType +
        " or class " + queryClass + " code");
    }

    if (addr == null) {	// no answer
      rcode = 3;	// hence response code is 3 for "name not known"
      numAns = 0;	// and there will be no answers section
    }

    // construct response flags
    respFlags = ((1 << DNS.SHIFT_QUERY) |
                     (DNS.OPCODE_QUERY << DNS.SHIFT_OPCODE) |
                     (1 << DNS.SHIFT_AUTHORITATIVE) |
                     (rcode << DNS.SHIFT_RESPONSE_CODE));
    try {
      dataOut.writeShort (queryID);
      dataOut.writeShort (respFlags);
      dataOut.writeShort (1); // # queries
      dataOut.writeShort (numAns); // # answers
      dataOut.writeShort (0); // # authorities
      dataOut.writeShort (0); // # additional
      // write single question
      writeDomainName (queryHost, dataOut);
      dataOut.writeShort (queryType);
      dataOut.writeShort (queryClass);
      // write answer section if needed
      if (numAns > 0) {
        // write single A record answer RR
        writeDomainName (queryHost, dataOut);
        dataOut.writeShort (queryType);
        dataOut.writeShort (queryClass);
        dataOut.writeInt (ttl);
        dataOut.writeShort (4);		// A address is 4 bytes
        dataOut.write (addr, 0, 4);	// address bytes
      }
    } catch (IOException ignored) { }
    return byteArrayOut.toByteArray ();
  }

  /**
   *  internal routine to write a domain name into the DNS query/response.
   *<p>
   *  The DNS name is written with each period replaced by the length of the
   *  next component of the name (assuming a leading period which becomes the
   *  length of the first component, and a trailing period which becomes 0
   *  to mark the end of the domain name).
   *<p>
   *  eg. "www.acme.com" becomes "3www4acme3com0" where each digit is a literal byte
   *
   *  @param name	the domain name to be written
   *  @param out	the output stream the name is written to
   */
  private void writeDomainName (String name, DataOutputStream out) throws IOException {
      StringTokenizer labels = new StringTokenizer (name, ".");
      while (labels.hasMoreTokens ()) {
        String label = labels.nextToken ();
        out.writeByte (label.length ());
        out.writeBytes (label);
      }
      out.writeByte (0);
  }

  /**
   *  internal routine to decode the flags field into its individual items,
   *  setting the relevant class variables. Throws an error if a non-zero
   *  code is found on decode.
   *
   *  @param flags	the flags field to be decoded
   */
  protected void decodeFlags (int flags) throws IOException {
    boolean isResponse = ((flags >> DNS.SHIFT_QUERY) & 1) != 0;
    opcode = (flags >> DNS.SHIFT_OPCODE) & 15;
    authoritative = ((flags >> DNS.SHIFT_AUTHORITATIVE) & 1) != 0;
    truncated = ((flags >> DNS.SHIFT_TRUNCATED) & 1) != 0;
    recurseRequest = ((flags >> DNS.SHIFT_RECURSE_PLEASE) & 1) != 0;
    recursive = ((flags >> DNS.SHIFT_RECURSE_AVAILABLE) & 1) != 0;
    int code = (flags >> DNS.SHIFT_RESPONSE_CODE) & 15;
    if (code != 0)
      throw new IOException (DNS.codeName (code) + " (" + code + ")");
  }

  /**
   *  access value of clientHost. 
   *  @return clientHost value
   */
  public InetAddress getClientHost () {
    return clientHost;
  }

  /**
   *  access value of clientPort. 
   *  @return clientPort value
   */
  public int getClientPort () {
    return clientPort;
  }

  /**
   *  access value of queryHost. 
   *  @return queryHost value
   */
  public String getQueryHost () {
    return queryHost;
  }

  /**
   *  access value of queryType. 
   *  @return queryType value
   */
  public int getQueryType () {
    return queryType;
  }

  /**
   *  access value of queryClass. 
   *  @return queryClass value
   */
  public int getQueryClass () {
    return queryClass;
  }

  /**
   *  access value of queryID. 
   *  @return queryID value
   */
  public int getQueryID () {
    return queryID;
  }

  /**
   *  access value of queryFlags. 
   *  @return queryFlags value
   */
  public int getQueryFlags () {
    return queryFlags;
  }

  /**
   *  access value of numQueries. 
   *  @return numQueries value
   */
  public int getNumQueries () {
    return numQueries;
  }

  /**
   *  access value of numAnswers. 
   *  @return numAnswers value
   */
  public int getNumAnswers () {
    return numAnswers;
  }

  /**
   *  access value of authoritative flag. 
   *  @return authoritative flag value
   */
  public boolean isAuthoritative () {
    return authoritative;
  }

  /**
   *  access value of truncated flag. 
   *  @return truncated flag value
   */
  public boolean isTruncated () {
    return truncated;
  }

  /**
   *  access value of recursive flag. 
   *  @return recursive flag value
   */
  public boolean isRecursive () {
    return recursive;
  }

}
