package edu.kit.aifb.gwifi.db;

import gnu.trove.map.hash.TIntObjectHashMap;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.IntegerBinding;
import com.sleepycat.je.DatabaseEntry;


/**
 * A {@link WDatabase} for associating Integer keys with some generic value type.
 *
 * @param <V> the type of object to store as values
 */
public abstract class IntObjectDatabase<V> extends WDatabase<Integer,V> {
	
	private TIntObjectHashMap<V> fastCache = null ;
	private TIntObjectHashMap<byte[]> compactCache = null ;
	
	/**
	 * Creates or connects to a database, whose name will match the given {@link WDatabase.DatabaseType}
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param valueBinding a binding for serialising and de-serialising values
	 */
	public IntObjectDatabase(WEnvironment env, DatabaseType type, EntryBinding<V> valueBinding) {
		super(env, type, new IntegerBinding(), valueBinding) ;
	}
	
	
	/**
	 * Creates or connects to a database with the given name.
	 * 
	 * @param env the WEnvironment surrounding this database
	 * @param type the type of database
	 * @param name the name of the database 
	 * @param valueBinding a binding for serialising and deserialising values 
	 */
	public IntObjectDatabase(WEnvironment env, DatabaseType type, String name, EntryBinding<V> valueBinding) {
		super(env, type, name, new IntegerBinding(), valueBinding) ;
	}
	
	@Override
	public long getCacheSize() {
		if (!isCached())
			return 0 ;
		
		if (getCachePriority() == CachePriority.space)
			return fastCache.size();
		else
			return compactCache.size();
	}
		
	@Override
	protected V retrieveFromCache(Integer key) {
		
		if (getCachePriority() == CachePriority.speed)
			return fastCache.get(key) ;
		else {
			byte[] cachedData = compactCache.get(key) ;
			
			if (cachedData == null)
				return null ;
			
			DatabaseEntry dbValue = new DatabaseEntry(cachedData) ;
			return valueBinding.entryToObject(dbValue) ;
		}
	}
	
	@Override
	protected void initializeCache() {
		
		System.out.println("Initializing cache for " + this.getName() + ": " + getCachePriority()) ;
		
		if (getCachePriority() == CachePriority.speed)
			fastCache = new TIntObjectHashMap<V>() ;
		else
			compactCache = new TIntObjectHashMap<byte[]>() ;
	}
	
	@Override
	protected void addToCache(WEntry<Integer,V> entry) {
		
		if (getCachePriority() == CachePriority.speed) {
			fastCache.put(entry.getKey(), entry.getValue()) ;
		} else {
			DatabaseEntry cacheValue = new DatabaseEntry() ;
			valueBinding.objectToEntry(entry.getValue(), cacheValue) ;
	
			compactCache.put(entry.getKey(), cacheValue.getData()) ;
		}
	}
}
