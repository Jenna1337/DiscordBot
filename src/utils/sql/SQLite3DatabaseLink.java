package utils.sql;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import sun.awt.OSInfo;
import utils.Utils;

public class SQLite3DatabaseLink
{
	private static final String sqlexecfilepath;
	static{
		String ostypepath;
		switch(OSInfo.getOSType()){
			case WINDOWS:
				ostypepath = "win";
				break;
			case MACOSX:
				ostypepath = "osx";
				break;
			case LINUX:
			case SOLARIS:
			case UNKNOWN:
			default:
				ostypepath = "linux";
				break;
				
		}
		sqlexecfilepath = java.util.Arrays.stream(new File("sqlite").listFiles())
				.filter(f->f.getName().contains(ostypepath))
				.findAny().get().toPath().resolve("sqlite3").toAbsolutePath().toString();
	}
	private final String dblocation, cmdstrcache;
	private String args = "";
	public SQLite3DatabaseLink(String databaselocation){
		this.dblocation = databaselocation.replaceAll("[^A-Za-z0-9.!@#$%^&()_+\\-={}\\[\\],.';\\u0060~]", "");
		cmdstrcache="\""+sqlexecfilepath+"\" "+dblocation;
	}
	public String getDatabaseLocation(){
		return dblocation;
	}
	public void setArguments(String args){
		this.args = args!=null ? " "+args+" " : "";
	}
	public SQLQueryResponse[] sendCommands(String[] commands) throws IOException{
		SQLQueryResponse[] responses = new SQLQueryResponse[commands.length];
		for(int i=0;i<commands.length;++i)
			responses[i]=sendCommand(commands[i]);
		return responses;
	}
	private String randstr(int len){
		return ' '+Utils.generateRandomAsciiString(len-2)+' ';
	}
	private String linesplitstr = randstr(16); 
	private String entrysplitstr = randstr(16); 
	public SQLQueryResponse sendCommand(String command) throws IOException{
		{
			int q=command.indexOf('\"'), a=command.indexOf('\'');
			if(q>=0 && a<0)
				command = command.replace('\"', '\'');
			else if(a>=0 && q<0)
				;//command = command.replace('\'', '\'');
			else if(q>=0 && a>=0)
				throw new IllegalArgumentException("Command must not contain both apostrophies and double quotes.");
			command = Utils.escapeNonAscii(command);
		}
		while(command.contains(linesplitstr))
			linesplitstr = randstr(linesplitstr.length()+1);
		while(command.contains(entrysplitstr) || linesplitstr.contains(entrysplitstr) || entrysplitstr.contains(linesplitstr))
			entrysplitstr = randstr(entrysplitstr.length()+1);
		Process p = Runtime.getRuntime().exec(cmdstrcache+args+" -newline \""+linesplitstr+"\" -separator \""+entrysplitstr+"\" \""+command+"\"");
		int ch;
		try{
			p.waitFor();
		}catch(InterruptedException e){
		}
		InputStream is = p.getInputStream();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while((ch=is.read())!=-1)
			baos.write(ch);
		is.close();
		String output = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		is = p.getErrorStream();
		baos = new ByteArrayOutputStream();
		while((ch=is.read())!=-1)
			baos.write(ch);
		is.close();
		String errors = new String(baos.toByteArray(), StandardCharsets.UTF_8);
		
		String[][] vars = Utils.unescapeNonAscii(Arrays.stream(output.split(linesplitstr))
				.map(line->line.split(entrysplitstr)).toArray(String[][]::new));
		
		return new SQLQueryResponse(p.exitValue(), vars, errors);
	}
	public SQLQueryResponse sendCommandNoFail(String command){
		try{
			return sendCommand(command);
		}catch(Exception e){
			throw new InternalError(e);
		}
	}
	private static final String _internalUseIdentifierPrefix = "sqlite_";
	public static String fixTableName(String originalName){
		String name = originalName;
		name = name.replace(' ', '_').replaceAll("[^A-Za-z0-9_]", "");
		if(name.startsWith(_internalUseIdentifierPrefix))
			name = name.substring(_internalUseIdentifierPrefix.length());
		name = name.replaceFirst("^\\d*", "");
		if(name.length()<=0)
			throw new IllegalArgumentException();
		return name;
	}
	public static void test() throws IOException, InterruptedException
	{
		// TODO Auto-generated method stub
		String[] testcmds = {//"CREATE TABLE IF NOT EXISTS TestTable(Name nvarchar(255), Id int PRIMARY KEY)",
				//"INSERT OR REPLACE INTO TestTable (Name, Id) VALUES ('\u00ff😉', 6)",
				//"INSERT OR REPLACE INTO TestTable (Name, Id) VALUES ('nt❤', -54)",
				"SELECT * FROM TestTable",
				//"DROP TABLE TestTable"
		};
		String dblocation = "test.sqlite3";
		SQLite3DatabaseLink db = new SQLite3DatabaseLink(dblocation);
		//db.setArguments("-header");
		Arrays.stream(db.sendCommands(testcmds))
		.map(SQLQueryResponse::getOutput)
		.filter(rows->{
			return rows.length>0 && rows[0].length>0 && rows[0][0].length()>0;
		})
		.forEach(cmdresponse->{
			System.out.println('<');
			Arrays.stream(cmdresponse)
			.forEach(row->{
				System.out.print("\t{");
				Arrays.stream(row)
				.forEach(col->{
					System.out.print("["+col+"],\t");
				});
				System.out.print("}\n");
			});
			System.out.println('>');
		});
	}
}
