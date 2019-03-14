package discordbot;

public enum TextLocation{
	username(0b001),
	nickname(0b010),
	name(0b011),
	message(0b100),
	// _unused0(0b101),
	// _unused1(0b110),
	all(-1);
	
	private final byte mask;
	
	private TextLocation(int v){
		this.mask = (byte)v;
	}
	public byte getMask(){
		return mask;
	}
}
