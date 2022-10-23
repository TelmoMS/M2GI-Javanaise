/***
 * Sentence class : used for keeping the text exchanged between users
 * during a chat application
 * Contact: 
 *
 * Authors: 
 */

package irc;

public class Sentence implements java.io.Serializable, InterfaceSentence {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	String data;

	public Sentence() {
		data = new String("");
	}

	@Override
	public void write(String text) {
		data = text;
	}

	@Override
	public String read() {
		return data;
	}

}