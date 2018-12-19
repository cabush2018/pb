package pb.limiter.locklimiter.model;

public enum ItemType {
	DATACENTER("datacenter"),SERVER("server"),STORAGE("storage"),;
	private ItemType(String type){
		this.type=type;
	}
	private String type;
}