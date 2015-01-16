package cz.darmovzal.yoca.guts;

public class PassphraseException extends GutsException {
	public PassphraseException(String message, Throwable t){
		super(message, t);
	}
	
	public PassphraseException(String message){
		super(message);
	}
}

