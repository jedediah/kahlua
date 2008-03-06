package se.krka.kahlua.vm;

public class LuaException extends RuntimeException {
	public Object errorMessage;

	public LuaException(Object errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getMessage() {
		if (errorMessage == null) {
			return "nil";
		}
		return errorMessage.toString();
	}
}
