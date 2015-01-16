package cz.darmovzal.yoca.guts;

public class GutsException extends RuntimeException {
	public GutsException(String message){
		super(message);
	}
	
	public GutsException(String message, Throwable t){
		super(message, t);
	}
}

