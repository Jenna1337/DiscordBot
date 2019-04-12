package utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import discordbot.TextLocation;
import javafx.util.Pair;
import utils.sql.SQLite3DatabaseLink;
import static utils.WebRequest.*;

public class DomainNameBlocker
{
	private static final String runAhkScript(String ahkfile, Predicate<String> lineconsumer, String[]... arguments) throws IOException, InterruptedException{
		Process p=Runtime.getRuntime().exec("runAhkScript.cmd "+ahkfile+" "+Arrays.stream(arguments).map(a->String.join("=", a)).collect(Collectors.joining(" ")));
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = null;
		while((line= input.readLine()) != null){
			if(!lineconsumer.test(line))
				break;
		}
		input.close();
		p.waitFor();
		return null;
	}
	private static final String updateProviderTableScript = "MakeBlacklistProviderTable.ahk";
	private static final String[][] updateProviderTableScriptArguments = new String[][]{
		{"inputfile","blacklists.ini"},
		{"outputfile","blacklistproviders.tsv"},
		{"debug","false"},
		{"verbose","true"},
	};
	private static final String blacklistpath = "blacklists";
	private static final long maxlastcheckedtimedifference = 7L*24L*60L*60L*1000L;
	private static long globallastcheckedtime = 0;
	
	private static Map<String,Set<String>> blacklists = new HashMap<>();
	private static Map<String,SQLite3SimpleList<String>> customblacklists = new HashMap<>();
	
	private DomainNameBlocker(){}
	
	private static Path providerlistpath = null;
	public static synchronized void updateBlacklists() throws IOException, InterruptedException
	{
		if(System.currentTimeMillis() - globallastcheckedtime < maxlastcheckedtimedifference){
			totalentries = 0;
			{
				runAhkScript(updateProviderTableScript,
						(line)->{
							if(line.startsWith("#"))
								return true;
							providerlistpath = Paths.get(line);
							return false;
						}, updateProviderTableScriptArguments);
			}
			if(providerlistpath==null)
				throw new InternalError(new NullPointerException());
			System.out.println(providerlistpath);
			Files.lines(providerlistpath, StandardCharsets.UTF_8).filter(line->(!line.startsWith("#"))).forEach(line->{
				String[] entry = line.split("\t");
				updateList(entry[0], entry[1], entry[2]);
			});
			globallastcheckedtime = System.currentTimeMillis();
			System.out.println("Loaded "+totalentries+" blacklist entries.");
		}
	}
	private static void updateList(String listname, String filetype,
			String filelocation)
	{
		if(!blacklists.containsKey(listname))
			blacklists.put(listname, new HashSet<String>());
		File localfile = new File(blacklistpath+"/"+listname+".txt"),
				localETagFile = new File(blacklistpath+"/etags/"+listname+"_latest_etag.txt");
		Path localETagFilePath = localETagFile.toPath();
		localETagFilePath.toFile().getParentFile().mkdirs();
		boolean localfileexists = localfile.exists(),
				localetagfileexits = localETagFile.exists(),
				ismodified = false;
		long lastcheckedtimedifference = System.currentTimeMillis() - localfile.lastModified(), lastcheckedtime = 0;
		try
		{
			if(lastcheckedtimedifference > maxlastcheckedtimedifference){
				if(localfileexists){
					long lastmodified_local = localfile.lastModified();
					String etag_local = localetagfileexits ? new String(Files.readAllBytes(localETagFilePath), StandardCharsets.UTF_8) : "";
					
					Map<String, List<String>> responseheaders = HEAD(filelocation,
							new String[]{"If-Modified-Since", Utils.getDateTime_RFC_5322(lastmodified_local)},
							localetagfileexits ? new String[]{"If-None-Match", etag_local} : null
							);
					System.out.println(listname+" "+responseheaders);
					List<String> etagresponseheader = responseheaders.get("ETag");
					if(etagresponseheader!=null){
						String etag_remote = etagresponseheader.get(0);
						ismodified = !etag_local.equals(etag_remote);
						if(ismodified)
							Files.write(localETagFilePath, etag_remote.getBytes(StandardCharsets.UTF_8));
					}
					else{
						List<String> lastmodifiedresponseheader = responseheaders.get("Last-Modified");
						if(lastmodifiedresponseheader!=null)
							ismodified = lastmodified_local <= Utils.parseDateTime_RFC_5322(lastmodifiedresponseheader.get(0));
						else{
							if(responseheaders.get(null).get(0).contains(Integer.toString(HttpURLConnection.HTTP_NOT_MODIFIED)))
								ismodified = true;
							else{
								//TODO implement more options?
								System.err.println(filelocation + "\t"+responseheaders);
								throw new InternalError("No valid response headers received from URL \""+filelocation+"\"");
							}
						}
					}
					List<String> dateresponseheader = responseheaders.get("Date");
					if(dateresponseheader !=null && dateresponseheader.size()>0)
						lastcheckedtime = Utils.parseDateTime_RFC_5322(dateresponseheader.get(0));
				}
				if(ismodified || !localfileexists){
					WebResponse response = getResponse("GET", new URL(filelocation));
					Map<String, List<String>> responseheaders = response.getResponseHeaders();
					List<String> etagresponseheader = responseheaders.get("ETag");
					if(etagresponseheader!=null)
						Files.write(localETagFilePath, etagresponseheader.get(0).getBytes(StandardCharsets.UTF_8));
					byte[] responsebytes = response.getBodyAsBytes();
					//connection.disconnect();
					if(filetype.matches("(?i).*\\bzip\\b.*"))
						responsebytes = getZipEntryContentsForFilenameRegex(responsebytes, "(?i).*\\bhosts\\b.*");
					Files.write(localfile.toPath(), Utils.rawBytesToString(responsebytes).getBytes(StandardCharsets.UTF_8));
					
					List<String> dateresponseheader = responseheaders.get("Date");
					if(dateresponseheader !=null && dateresponseheader.size()>0)
						lastcheckedtime = Utils.parseDateTime_RFC_5322(dateresponseheader.get(0));
				}
				else if(lastcheckedtime!=0)
					localfile.setLastModified(lastcheckedtime);
			}
			try{
				loadList(listname, filetype, Files.readAllLines(localfile.toPath()));
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	static int totalentries = 0;
	private static void loadList(String listname, String filetype,
			List<String> list)
	{
		Set<String> targetlist = blacklists.get(listname);
		String wildcard = ".*", boundif = (filetype.matches(".*\bpartial\b.*") ? "" : "\\b"),
				rbl=wildcard+boundif,rbr=boundif+wildcard;
		if(filetype.matches("(?i).*\\bhosts\\b.*"))
			list.stream().filter(line->(!line.startsWith("#"))).forEach(line->{
				if(line.replaceAll("\\s+", "").isEmpty()) return;
				//System.out.println(line.replaceFirst("^[\\d:\\.]+\\s+", "").replaceFirst("#.*$", "").replaceAll("\\.", "\\\\."));
				targetlist.add(rbl+line.replaceFirst("^[\\d:\\.]+\\s+", "").replaceFirst("#.*$", "").replaceAll("\\.", "\\\\.")+rbr);
				++totalentries;
			});
		else if(filetype.matches("(?i).*\\bip_address\\b.*"))
			list.stream().filter(line->(!line.startsWith("#"))).forEach(line->{
				if(line.replaceAll("\\s+", "").isEmpty()) return;
				targetlist.add(rbl+line.replaceFirst("#.*$", "")+rbr);
				++totalentries;
			});
		else if(filetype.matches("(?i).*\\bpcre\\b.*"))
			list.stream().forEach(line->{
				if(line.replaceAll("\\s+", "").isEmpty()) return;
				targetlist.add(rbl+line.replaceAll("\\(\\?\\#(?:[^\\\\\\)]|\\\\.)*\\)", "").replaceAll("(?<!\\\\)\\/", "\\\\/")+rbr);
				++totalentries;
			});
		else
			throw new RuntimeException(new UnsupportedOperationException("Failed to handle type "+filetype));
	}
	private static byte[] getZipEntryContentsForFilenameRegex(byte[] zipFileBytes, String regex) 
	{
		try
		{
			ZipInputStream zipstream = new ZipInputStream(new ByteArrayInputStream(zipFileBytes));
			
			ZipEntry entry;
			while((entry = zipstream.getNextEntry())!=null){
				if(!entry.isDirectory() && entry.getName().matches(regex)){
					byte[] bytes = new byte[(int)entry.getSize()];
					for(int i=0;i<bytes.length;++i)
						bytes[i]=(byte)zipstream.read();
					//zipstream.read(bytes);
					return bytes;
				}
			}
			zipstream.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static Map<String, Set<String>> getBlacklistedEntriesInText(String message, byte type)
	{
		final String lowercasemessage = message.toLowerCase();
		
		//map (listname => set( 
		
		return blacklists.entrySet()
				.stream().map(
						listlistentry->{
							String listname = listlistentry.getKey();
							
							Set<String> filteredset = listlistentry.getValue().stream().filter(
									listentry->lowercasemessage.matches(listentry)).collect(HashSet<String>::new, HashSet<String>::add,
											HashSet<String>::addAll);
							
							return filteredset.isEmpty() ? null : new Pair<String, Set<String>>(listname, filteredset);
							
							
						})
				.filter(a->a!=null)
				.collect(Collectors.toMap(a->a.getKey(), a->a.getValue()));
	}
	
	static{
		try
		{
			updateBlacklists();
		}
		catch(Exception e)
		{
			throw new InternalError(e);
		}
	}
}
class SQLite3SimpleList<E extends Serializable> implements Set<E>{
	private static final String dbname = "CustomAccessLists.sqlite3";
	private HashSet<E> set = new HashSet<>();
	private SQLite3DatabaseLink db;
	String tablename;
	
	public int size(){
		synchronized(this){
			return set.size();
		}
	}
	public boolean isEmpty(){
		synchronized(this){
			return set.isEmpty();
		}
	}
	public boolean contains(Object o){
		synchronized(this){
			return set.contains(o);
		}
	}
	public Iterator<E> iterator(){
		synchronized(this){
			throw new RuntimeException();
			//return set.iterator();
		}
	}
	public Object[] toArray(){
		synchronized(this){
			return set.toArray();
		}
	}
	public <T> T[] toArray(T[] a){
		synchronized(this){
			return set.toArray(a);
		}
	}
	public boolean add(E e){
		synchronized(this){
			db.sendCommandNoFail("INSERT INTO "+tablename+" (vals) VALUES ("+e.toString()+")");
			// TODO Auto-generated method stub
			return false;
		}
	}
	public boolean remove(Object o){
		synchronized(this){
			// TODO Auto-generated method stub
			return false;
		}
	}
	public boolean containsAll(Collection<?> c){
		synchronized(this){
			return set.containsAll(c);
		}
	}
	public boolean addAll(Collection<? extends E> c){
		synchronized(this){
			boolean modified = false;
			for (E s : c)
				if (add(s))
					modified = true;
			return modified;
		}
	}
	public boolean retainAll(Collection<?> c){
		synchronized(this){
			Objects.requireNonNull(c);
			boolean modified = false;
			Iterator<E> it = iterator();
			E n;
			while (it.hasNext()) {
				if (!c.contains(n=it.next())) {
					//it.remove();
					remove(n);
					modified = true;
				}
			}
			return modified;
		}
	}
	public boolean removeAll(Collection<?> c){
		synchronized(this){
			Objects.requireNonNull(c);
			boolean modified = false;
			
			if (size() > c.size()) {
				for (Iterator<?> i = c.iterator(); i.hasNext(); )
					modified |= remove(i.next());
			} else {
				E n;
				for (Iterator<E> i = iterator(); i.hasNext(); ) {
					if (c.contains(n=i.next())) {
						//i.remove();
						remove(n);
						modified = true;
					}
				}
			}
			return modified;
		}
	}
	public void clear(){
		synchronized(this){
			db.sendCommandNoFail("DROP TABLE "+tablename);
			set.clear();
		}
	}
}
