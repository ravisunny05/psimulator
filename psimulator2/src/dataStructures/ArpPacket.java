/*
 * created 1.2.2012
 */

package dataStructures;

import dataStructures.ipAddresses.IpAddress;

/**
 * Represents ARP packet.
 *
 * @author Stanislav Rehak <rehaksta@fit.cvut.cz>
 */
public class ArpPacket extends L3Packet {

	public enum ArpOperation { // only theese 2 options exists
		ARP_REQUEST,
		ARP_REPLY;
	}

	/**
	 * Inquirer IP address.
	 */
	public final IpAddress senderIpAddress;
	/**
	 * Inquirer MAC address.
	 */
	public final MacAddress senderMacAddress;
	/**
	 * Target IP address.
	 */
	public final IpAddress targetIpAddress;
	/**
	 * Target MAC address.
	 */
	public final MacAddress targetMacAddress;
	/**
	 * ARP request || ARP reply
	 */
	public final ArpOperation operation;

	/**
	 * Constructor for creating ARP reply.
	 *
	 * @param senderIpAddress inquirer IP address
	 * @param senderMacAddress inquirer MAC zdroje
	 * @param targetIpAddress my IP address
	 * @param targetMacAddress my MAC address (searched address)
	 */
	public ArpPacket(IpAddress senderIpAddress, MacAddress senderMacAddress, IpAddress targetIpAddress, MacAddress targetMacAddress) {
		this.senderIpAddress = senderIpAddress;
		this.senderMacAddress = senderMacAddress;
		this.targetIpAddress = targetIpAddress;
		this.targetMacAddress = targetMacAddress;
		this.operation = ArpOperation.ARP_REPLY;
	}

	/**
	 * Constructor for creating ARP request.
	 * Sends iff target MAC address is unknown.
	 *
	 * @param senderIpAddress sending interface's IP address
	 * @param senderMacAddress sending interface's MAC address
	 * @param targetIpAddress IP adresa pro kterou hledam MAC adresu
	 */
	public ArpPacket(IpAddress senderIpAddress, MacAddress senderMacAddress, IpAddress targetIpAddress) {
		this.senderIpAddress = senderIpAddress;
		this.senderMacAddress = senderMacAddress;
		this.targetIpAddress = targetIpAddress;
		this.targetMacAddress = new MacAddress("00:00:00:00:00:00");
		this.operation = ArpOperation.ARP_REQUEST;
	}

	/**
	 * Constructor for creating ARP request.
	 * Sends iff IP address on network interface was changed in order to notify other devices.
	 *
	 * @param senderIpAddress IP address of changed interface
	 * @param senderMacAddress MAC address of changed interface
	 */
	public ArpPacket(IpAddress senderIpAddress, MacAddress senderMacAddress) {
		this.senderIpAddress = senderIpAddress;
		this.senderMacAddress = senderMacAddress;
		this.targetIpAddress = new IpAddress("0.0.0.0");
		this.targetMacAddress = new MacAddress("00:00:00:00:00:00");
		this.operation = ArpOperation.ARP_REQUEST;
	}

	@Override
	public L3PacketType getType() {
		return L3PacketType.ARP;
	}

}