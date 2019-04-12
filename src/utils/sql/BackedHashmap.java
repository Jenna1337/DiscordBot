package utils.sql;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A {@link HashMap} backed by a SQLite table.
 * 
 * @author Jenna Sloan
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class BackedHashmap<K extends Serializable, V extends Serializable> extends HashMap<K,V>
{
	private final String tablename;
	private final SQLite3DatabaseLink db;
	@SuppressWarnings("unchecked")
	public BackedHashmap(String dbFileLocation, String tablename0, int initialCapacity, float loadFactor){
		super(initialCapacity, loadFactor);
		this.db = new SQLite3DatabaseLink(dbFileLocation);
		this.tablename = SQLite3DatabaseLink.fixTableName(tablename0);
		db.sendCommandNoFail("CREATE TABLE IF NOT EXISTS "+tablename+"(K varchar PRIMARY KEY , V varchar)");
		
		//TODO load table contents from db
		
		
		Arrays.stream(db.sendCommandNoFail("SELECT * FROM "+tablename).getOutput())
		.forEach(row->{
			try{
				K key = (K)deserialize(row[0]);
				V value = (V)deserialize(row[1]);
				this.put(key, value);
			}catch(Exception e){
				throw new InternalError(e);
			}
		});
		
		
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	public BackedHashmap(String dbFileLocation, String tablename0, int initialCapacity){
		this(dbFileLocation, tablename0, initialCapacity, 0.75f);
	}
	public BackedHashmap(String dbFileLocation, String tablename0){
		this(dbFileLocation, tablename0, 16, 0.75f);
	}
	public BackedHashmap(String dbFileLocation, String tablename0, Map<? extends K, ? extends V> m){
		this(dbFileLocation, tablename0);
		//TODO
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	static final int hash(Object key) {
		int h;
		return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
	}
	/** Read the object from Base64 string. */
	private static Object deserialize(String s) throws IOException, ClassNotFoundException
	{
		byte[] data = Base64.getDecoder().decode(s);
		ObjectInputStream ois = new ObjectInputStream(
				new ByteArrayInputStream(data));
		Object o = ois.readObject();
		ois.close();
		return o;
	}
	
	/** Write the object to a Base64 string. */
	private static final String serialize(Serializable o)
	{
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.close();
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
		catch(IOException e){
			throw new InternalError(e);
		}
	}
	@Override
	public boolean isEmpty(){
		return super.isEmpty();
	}
	@Override
	public V get(Object key){
		return super.get(key);
	}
	@Override
	public boolean containsKey(Object key){
		return super.containsKey(key);
	}
	@Override
	public V put(K key, V value){
		V result = super.put(key, value);
		db.sendCommandNoFail("INSERT OR REPLACE INTO "+tablename+" (K, V) VALUES ('"+serialize(key)+"', '"+serialize(value)+"')");
		return result;
	}
	@Override
	public void putAll(Map<? extends K, ? extends V> m){
		m.keySet().forEach(key->{
			this.put(key, m.get(key));
		});
	}
	@SuppressWarnings("unchecked")
	@Override
	public V remove(Object key){
		V result = super.remove(key);
		db.sendCommandNoFail("DELETE FROM "+tablename+" WHERE K = '"+serialize((K)key)+"'");
		return result;
	}
	@Override
	public void clear(){
		super.clear();
		db.sendCommandNoFail("DROP TABLE "+tablename);
	}
	@Override
	public boolean containsValue(Object key){
		return super.containsValue(key);
	}
	@Override
	public Set<K> keySet(){
		return super.keySet();
	}
	@Override
	public Collection<V> values(){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@Override
	public Set<Map.Entry<K,V>> entrySet(){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@Override
	public V getOrDefault(Object key, V defaultValue){
		return super.getOrDefault(key, defaultValue);
	}
	@Override
	public V putIfAbsent(K key, V value){
		V currentval = get(key);
		if(currentval == null){
			put(key, value);
			return null;
		}
		else
			return currentval;
	}
	@Override
	public boolean remove(Object key, Object value){
		V currentValue = get(key);
		if(currentValue==null ? value==null : currentValue.equals(value)){
			remove(key);
			return true;
		}
		return false;
	}
	@Override
	public boolean replace(K key, V oldValue, V newValue){
		V currentValue = get(key);
		if(currentValue==null ? oldValue==null : currentValue.equals(oldValue)){
			put(key, newValue);
			return true;
		}
		return false;
	}
	@Override
	public V replace(K key, V value){
		if(containsKey(key))
			return put(key, value);
		return null;
	}
	@SuppressWarnings("unused")
	@Override
	public V computeIfAbsent(K key,
			Function<? super K, ? extends V> mappingFunction){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@SuppressWarnings("unused")
	@Override
	public V computeIfPresent(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@SuppressWarnings("unused")
	@Override
	public V compute(K key,
			BiFunction<? super K, ? super V, ? extends V> remappingFunction){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@SuppressWarnings("unused")
	@Override
	public V merge(K key, V value,
			BiFunction<? super V, ? super V, ? extends V> remappingFunction){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@SuppressWarnings("unused")
	@Override
	public void forEach(BiConsumer<? super K, ? super V> action){
		throw new UnsupportedOperationException("This method is not yet implemented.");
	}
	@Override
	public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function){
		this.keySet().forEach(key->{
			this.put(key, function.apply(key, this.get(key)));
		});
	}
	@Override
	public Object clone(){
		throw new UnsupportedOperationException();
	}
}
