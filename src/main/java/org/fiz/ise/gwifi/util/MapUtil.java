package org.fiz.ise.gwifi.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

public class MapUtil {
	
	
	public <K, V> K getKey(Map<K, V> map, V value) {
	    for (Entry<K, V> entry : map.entrySet()) {
	        if (entry.getValue().equals(value)) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
	    for (Entry<T, E> entry : map.entrySet()) {
	        if (Objects.equals(value, entry.getValue())) {
	            return entry.getKey();
	        }
	    }
	    return null;
	}
	
	public static <K, V> Entry<K, V> getFirst(Map<K, V> map) {
		  if (map.isEmpty()) return null;
		  return map.entrySet().iterator().next();
		}

		public static <K, V> Entry<K, V> getLast(Map<K, V> map) {
		  try {
		    if (map instanceof LinkedHashMap) return getLastViaReflection(map);
		  } catch (Exception ignore) { }
		  return getLastByIterating(map);
		}

		private static <K, V> Entry<K, V> getLastByIterating(Map<K, V> map) {
		  Entry<K, V> last = null;
		  for (Entry<K, V> e : map.entrySet()) last = e;
		  return last;
		}

		private static <K, V> Entry<K, V> getLastViaReflection(Map<K, V> map) throws NoSuchFieldException, IllegalAccessException {
		  Field tail = map.getClass().getDeclaredField("tail");
		  tail.setAccessible(true);
		  return (Entry<K, V>) tail.get(map);
		}
	
		/**
		 * Sort a Map by value in ascending order
		 ** 
		 * @param map
		 * @return a sorted map
		 */
		public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueAscendingGeneric(Map<K, V> map) {
			List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
				public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
					return (o1.getValue()).compareTo(o2.getValue());
				}
			});

			LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
			for (Map.Entry<K, V> entry : list) {
				result.put(entry.getKey(), entry.getValue());
			}
			return result;
		}	
		
	
	/**
	 * Sort a Map by value in descending order
	 ** 
	 * @param map
	 * @return a sorted map
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		LinkedHashMap<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Sort a Map by key in asscending order
	 ** 
	 * @param roleMap
	 * @return a sorted map
	 */
	public static <K, V extends Comparable<? super V>> Map<String, Set<V>> sortByKeyAscending(
			Map<String, Set<V>> roleMap) {
		List<Map.Entry<String, Set<V>>> list = new LinkedList<Map.Entry<String, Set<V>>>(roleMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Set<V>>>() {
			public int compare(Map.Entry<String, Set<V>> o1, Map.Entry<String, Set<V>> o2) {
				return (o1.getKey().length() - o2.getKey().length());
			}
		});

		LinkedHashMap<String, Set<V>> result = new LinkedHashMap<String, Set<V>>();
		for (Map.Entry<String, Set<V>> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}
	

	/**
	 * Sort a Map by key in descending order
	 ** 
	 * @param map
	 * @return a sorted map
	 */
	public static <K, V extends Comparable<? super V>> Map<String, Set<V>> sortByKeyDescending(
			Map<String, Set<V>> map) {
		List<Map.Entry<String, Set<V>>> list = new LinkedList<Map.Entry<String, Set<V>>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Set<V>>>() {
			public int compare(Map.Entry<String, Set<V>> o1, Map.Entry<String, Set<V>> o2) {
				return (o2.getKey().length() - o1.getKey().length());
			}
		});

		LinkedHashMap<String, Set<V>> result = new LinkedHashMap<String, Set<V>>();
		for (Map.Entry<String, Set<V>> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Sort a Map by value in ascending order
	 ** 
	 * @param unsortMap
	 * @return a sorted map
	 */
	public static Map<Integer, Float> sortByValueAscending(final Map<Integer, Float> unsortMap) {
		// Convert Map to List
		final List<Map.Entry<Integer, Float>> list = new LinkedList<Map.Entry<Integer, Float>>(unsortMap.entrySet());
		// Sort list with comparator, to compare the Map values
		final Comparator<Map.Entry<Integer, Float>> comparator = new Comparator<Map.Entry<Integer, Float>>() {
			public int compare(Map.Entry<Integer, Float> o1, Map.Entry<Integer, Float> o2) {
				if (o1.getValue().floatValue() > o2.getValue().floatValue()) {
					return 1;
				} else if (o1.getValue().floatValue() < o2.getValue().floatValue()) {
					return -1;
				} else {
					return 0;
				}
			}
		};
		Collections.sort(list, comparator);
		// Convert sorted map back to a Map
		Map<Integer, Float> sortedMap = new LinkedHashMap<Integer, Float>();
		for (Iterator<Map.Entry<Integer, Float>> it = list.iterator(); it.hasNext();) {
			Map.Entry<Integer, Float> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	/**
	 * Sort a Map by key in descending order by considering number of words
	 ** 
	 * @param map
	 * @return a sorted map
	 */
	public static <K, V extends Comparable<? super V>> Map<String, Set<V>> sortByKeyDescendingNumberOfWords(
			Map<String, Set<V>> map) {
		List<Map.Entry<String, Set<V>>> list = new LinkedList<Map.Entry<String, Set<V>>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Set<V>>>() {
			public int compare(Map.Entry<String, Set<V>> o1, Map.Entry<String, Set<V>> o2) {
				final int o2Size = o2.getKey().split(" ").length;
				final int o1Size = o1.getKey().split(" ").length;

				return (o2Size - o1Size);
			}
		});

		LinkedHashMap<String, Set<V>> result = new LinkedHashMap<String, Set<V>>();
		for (Map.Entry<String, Set<V>> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Sort a Map by value in ascending order
	 ** 
	 * @param unsortMap
	 * @return a sorted map
	 */
	public static Map<String, Integer> sortByValueAscending2(final Map<String, Integer> unsortMap) {
		// Convert Map to List
		final List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(unsortMap.entrySet());
		// Sort list with comparator, to compare the Map values
		final Comparator<Map.Entry<String, Integer>> comparator = new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				if (o1.getValue().intValue() > o2.getValue().intValue()) {
					return 1;
				} else if (o1.getValue().intValue() < o2.getValue().intValue()) {
					return -1;
				} else {
					return 0;
				}
			}
		};
		Collections.sort(list, comparator);
		// Convert sorted map back to a Map
		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Integer> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}

	public static Map<String, Double> getFirstNElement(Map<String, Double> longMap, int n) {
		Map<String, Double> trimmedMap = new LinkedHashMap<String, Double>();
		int i = 0;
		for (Entry<String, Double> entry : longMap.entrySet()) {
			trimmedMap.put(entry.getKey(), entry.getValue());
			if (i > n) {
				break;
			}
			i++;
		}

		return trimmedMap;
	}
	
	public static List<String> getFirstNElementInList(Map<String, Double> longMap, int n) {
		final List<String> trimmedMap = new ArrayList<>();
		int i = 0;
		if (longMap.size()>n) {
			for (final Entry<String, Double> entry : longMap.entrySet()) {
				trimmedMap.add(entry.getKey());
				if (i > n) {
					break;
				}
				i++;
			}
		}
		else
		{
			for (final Entry<String, Double> entry : longMap.entrySet()) {
				trimmedMap.add(entry.getKey());
			}
		}
		
		return trimmedMap;
	}
}