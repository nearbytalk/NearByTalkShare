package org.nearbytalk.identity;

import org.nearbytalk.util.DigestUtility;
import org.nearbytalk.util.Utility;

public abstract class BaseIdentifiable implements Identifiable {
	
	public static final String ID_BYTES_KEY="idBytes";

	private String idBytes;

	@Override
	final public String getIdBytes() {
		return idBytes;
	}
	
	/**
	 * 
	 * construct a empty instance,
	 * must call {@link#setIdBytes(byte[])} later
	 */
	public BaseIdentifiable() {
	}
	
	public BaseIdentifiable(String idBytes){
		setIdBytes(idBytes);
	}

	protected void setIdBytes(String idBytes) {

		assert DigestUtility.isValidSHA1(idBytes);

		this.idBytes = idBytes;
	}

	protected void setIdBytes(byte[] idBytes) {

		assert DigestUtility.isValidSHA1(idBytes);

		this.idBytes = DigestUtility.byteArrayToHexString(idBytes);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((idBytes == null) ? 0 : idBytes.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseIdentifiable other = (BaseIdentifiable) obj;
		if (idBytes == null) {
			if (other.idBytes != null)
				return false;
		} else if (!idBytes.equals(other.idBytes))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BaseIdentifiable [idBytes=" + Utility.idBytesToString(idBytes)+ "]";
	}
	
	
}
