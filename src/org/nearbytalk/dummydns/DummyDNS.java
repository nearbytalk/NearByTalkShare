package org.nearbytalk.dummydns;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Random;

import org.nearbytalk.runtime.Global;
import org.nearbytalk.util.IpAddrConvert;


/**
 * Implement a very minimal DNS server responding to name lookups only.
 * <p>
 * DummyDNS is a very minimal DNS server which responds to requests to map a
 * supplied name to a corresponding IP address (ie A record lookups). Any other
 * type of request results in an error response code being returned. It is an
 * <b>authorative, non-recursive, non-hierarchical</b> server. It has no concept
 * of the DNS domain name hierarchy, it either knows a name (treated as an
 * unstructured string) has a corresponding address, or assumes it is unknown.
 * <p>
 * It is designed for use on small, isolated, test networks, such as the lab
 * networks for Cisco CCNA or similar, data networks courses. For these it
 * assists by allowing simple DNS name lookups to occur with a very simple and
 * minimalist configuration. If you want anything smarter than that, then this
 * is not the program for you!
 * <p>
 * When run, if no arguments are supplied a GUI interface is launched with
 * buttons to load a configuration and start the server, or quit; plus a log
 * output window.
 * <p>
 * If started with command-line arguments, either a short usage message is
 * displayed if the '-h" flag is given, or the first argument is assumed to be
 * the name of the configuration file, which is loaded, and the server started
 * in command-line mode, with logging to stderr.
 * <p>
 * Note that since the server has to bind to the privileged UDP port 53 which is
 * reserved for DNS use, it must be run with appropriate root/administrator
 * privileges on most respectable O/S's. This also means that it cannot be run
 * on a system with a "real" DNS server running already. That of course is not a
 * problem for its target market of isolated lab networks.
 * <p>
 * The configuration file contains lines of <code>name&lt;TAB&gt;address</code>
 * pairs, whose name is supplied to the constructor. The format is rigid. It
 * accepts lines starting with # as a comment. Any other extraneous characters
 * may cause an error when parsing it. Lastly a default entry is always added
 * for "<code>localhost	127.0.0.1</code>".
 * <p>
 * eg. a minimalist config for a single-router + switch Cisco lab might be:
 * 
 * <pre>
 *  # simple single-router + switch Cisco lab config
 *  router	192.168.1.1
 *  switch	192.168.1.2
 *  pc1	192.168.1.10
 *  pc2	192.168.1.11
 * </pre>
 * 
 * If the above were saved in the file <code>dns.txt</code>, then the server
 * could be run in command-line mode using the command:
 * <code>java DummyDNS dns.txt</code> (or if running out of the jar archive:
 * <code>java -jar DummyDNS.jar dns.txt</code>).
 * <p>
 * The DNS query/response handling (in DNS*.java) uses code adapted from:
 * 
 * <PRE>
 *       Java Network Programming, Second Edition
 *       Merlin Hughes, Michael Shoffner, Derek Hamner
 *       Manning Publications Company; ISBN 188477749X
 * </PRE>
 * <p>
 * That code is Copyright &copy; 1997-1999 Merlin Hughes, Michael Shoffner,
 * Derek Hamner; see jnp-license.txt (in distribution or jarfile) for details of
 * its licensing.
 * <p>
 * The DummyDNS application and GUI were written by Lawrie Brown from ADFA,
 * Canberra Australia.
 * <p>
 * Lawrie's code is Copyright &copy; 2005 by Lawrie Brown. Permission to reuse
 * the code as desired is granted, provided acknowledgement is given of the
 * author and source of the original code.
 * 
 * @author Lawrie Brown &lt;Lawrie.Brown@adfa.edu.au&gt;
 * @version v1.2 - 8 Apr 2005
 * 
 * @see DummyGUI
 * @see <a href="http://www.unsw.adfa.edu.au/~lpb/">Lawrie Brown's ADFA home</a>
 * @see <a href="http://nitric.com/jnp/">Java Network Programming</a>
 */
public class DummyDNS extends Thread {

	/** the datagram socket the server is listening on (usually UDP 53). */
	/** flag indicating whether the server is stopping/stopped. */
	private boolean stopping = false;

	private static Random random = new Random();

	public boolean isStopping() {
		return stopping;
	}

	static {
		random.setSeed(Calendar.getInstance().getTimeInMillis());
	}

	public void setStopping(boolean stopping) {
		this.stopping = stopping;
		if (this.listenSocket!=null) {
			listenSocket.close();
		}
	}

	/** version number of this server code. */
	public static final String version = "1.2";

	private int listenPort = Global.DNSInfo.DEFAULT_LISTEN_PORT;

	public void setHostIp(String hostIp) throws UnknownHostException {
		onlyOne = IpAddrConvert.string2Addr(hostIp);
	}

	public int getListenPort() {
		return listenPort;
	}

	private byte[] onlyOne = null;

	/**
	 * construct a new DummyDNS server with config info in named file.
	 * 
	 * @param name
	 *            name of configuration file.
	 */
	public DummyDNS() {

		try {
			onlyOne = InetAddress.getByName("127.0.0.1").getAddress();
		} catch (UnknownHostException e) {
			// impossible
			e.printStackTrace();
		}

	}

	private int randomPort() {

		return random.nextInt(65536 - 1024) + 1024;

	}

	private DatagramSocket createListen() {
		DatagramSocket socket = null;

		boolean firstTry = true;

		for (int i = 0; i < 10; i++) {
			try {

				listenPort = firstTry ? listenPort : randomPort();

				firstTry = false;

				socket = new DatagramSocket(listenPort);

				break;

			} catch (SocketException e1) {

				log(String.valueOf(listenPort) + " in use, try another");

			}
		}

		if (socket != null) {
			Global.getInstance().dnsInfo.isRunning = true;
			Global.getInstance().dnsInfo.listenPort = socket.getLocalPort();
		}

		return socket;

	}
	
	private DatagramSocket listenSocket;

	/**
	 * main server routine called when server thread is started.
	 * <p>
	 * Loads the configuration information, then loops getting the next query
	 * looking up the name to see if its known, then returning the response.
	 */
	public void run() {

		listenSocket = createListen();

		stopping = stopping && (listenSocket != null);

		log("DummyDNS server started .at " + listenSocket.getLocalPort());

		while (!stopping) { // master server loop read query, lookup, respond

			try {
				DNSQuery query = new DNSQuery(); // get next query
				byte[] addr = null;
				query.getQuery(listenSocket);

				if ((query.getQueryType() == DNS.TYPE_A)
						|| (query.getQueryType() == DNS.TYPE_ANY)) {

					addr = onlyOne; // lookup name to addr

				}
				
				

				query.sendResponse(listenSocket, addr); // send response & log
				log("DummyDNS query from "
						+ query.getClientHost().getHostAddress() + " ("
						+ query.getQueryID() + ") "
						+ DNS.typeName(query.getQueryType()) + " for ["
						+ query.getQueryHost() + "] = " + showAddr(addr));
			} catch (IOException e) {
				log("DummyDNS bad query: " + e);
			}

		}
		
		if (!listenSocket.isClosed()) {
			listenSocket.close();
		}

		Global.getInstance().dnsInfo.isRunning = false;
	}

	/**
	 * return list of all names known to this server.
	 */
	public String toString() {

		return "only one ip: ";
	}

	/**
	 * Convert 4-byte IP address into display string for logging.
	 * 
	 * @param addr
	 *            IP address to be displayed
	 * @return display address in form "A.B.C.D"
	 */
	private String showAddr(byte[] addr) {
		if (addr == null)
			return ("unknown");
		return ((int) addr[0] & 0xff) + "." + ((int) addr[1] & 0xff) + "."
				+ ((int) addr[2] & 0xff) + "." + ((int) addr[3] & 0xff);
	}

	/**
	 * display log message either to stderr or GUI.
	 * 
	 * @param msg
	 *            message to display
	 */
	public synchronized void log(String msg) {
		System.err.println(msg);
	}

	public void setListenPort(int i) {
		this.listenPort = i;

	}

}
