package org.nearbytalk.query;

public class PagedQuery extends AbstractQuery {

	public static class PagedInfo {
		public int size = 20;
		public int index = 1;

		/**
		 * construct a size=20,index=1 pagedInfo
		 * 
		 */
		public PagedInfo() {

		}

		/**
		 * construct a pagedInfo
		 * warning ,the base of index is 1,not zero .this is for client convenient 
		 * @param size
		 * @param index
		 */
		public PagedInfo(int size, int index) {
			this.size = size;
			this.index = index;
		}

		public int beginOffset() {
			return size * (index-1);
		}
	}

	public PagedInfo pagedInfo;

}
