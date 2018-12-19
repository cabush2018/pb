package pb.limiter.locklimiter.model;

public enum Action {
	CREATE("create"),UPDATE("update"),DELETE("delete"),MAKE_SNAPSHOT("make-snapshot");
	private Action(String value){
		this.value=value;
	}
	private String value;
}