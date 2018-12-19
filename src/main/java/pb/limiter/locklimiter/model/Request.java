package pb.limiter.locklimiter.model;

import lombok.Data;

@Data
public class Request implements Comparable<Request> {
	private long dataCenterId;

	private ItemType itemType;

	private long itemId;

	private long attachToServerId;

	private Action action;

	private Long created = System.currentTimeMillis();

	@Override
	public String toString() {
		return String.format("[Request:%d:%s:%d:%d:%s:%s]",dataCenterId,itemType,itemId,attachToServerId,action,created);
	}

	@Override
	public int compareTo(Request other) {
		if (created < other.created)
			return -1;
		if (created > other.created)
			return 1;
		return 0;
	}
}