package utils.sql;

import java.util.Arrays;

public class SQLQueryResponse
{
	private final int exitValue;
	private final String[][] output;
	private final String errors;
	public SQLQueryResponse(int exitValue, String[][] strings, String errors)
	{
		this.exitValue=exitValue;
		this.output=strings;
		this.errors=errors;
		
		// TODO Auto-generated constructor stub
	}
	public int getErrorCode(){
		return exitValue;
	}
	public String[][] getOutput(){
		return Arrays.copyOf(output, output.length);
	}
	public String getErrors(){
		return errors;
	}
}
