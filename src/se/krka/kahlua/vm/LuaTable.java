/*
Copyright (c) 2007 Kristofer Karlsson <kristofer.karlsson@gmail.com>
Portions of this code Copyright (c) 2007 Andre Bogus <andre@m3n.de>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package se.krka.kahlua.vm;

import java.lang.ref.WeakReference;

import se.krka.kahlua.stdlib.BaseLib;


public final class LuaTable {
	Object[] keys;
	Object[] values;
	private int[] next;

	private int freeIndex;
	public boolean weakKeys, weakValues;

	private static int nearestPowerOfTwo(int x) {
		x--;
		x |= (x >> 1);
		x |= (x >> 2);
		x |= (x >> 4);
		x |= (x >> 8);
		x |= (x >> 16);
		return(x+1);
	}

	public LuaTable(int capacity) {
		capacity = nearestPowerOfTwo(capacity);

		keys = new Object[capacity];
		values = new Object[capacity];
		next = new int[capacity];

		freeIndex = capacity;
	}

	public LuaTable() {
		this(16);
	}

	private int getMP(Object key) {
		// assert key != null
		int capacity = keys.length;
		return luaHashcode(key) & (capacity - 1);
	}
	
	private final Object unref(Object o) {
		if (o == null) return null;
		if (o instanceof WeakReference)
			return ((WeakReference) o).get();
		return o;
	}

	public final int hash_primitiveFindKey(Object key, int index) {
		Object currentKey = unref(keys[index]);

		if (currentKey == null) {
			return -1;
		}		
		/*
		 * Doubles need special treatment due to how
		 * java implements equals and hashcode for Double
		 */
		if (key instanceof Double) {
			double dkey = LuaState.fromDouble(key);
			while (true) {
				if (currentKey instanceof Double) {
					double dCurrentKey = LuaState.fromDouble(currentKey);
					if (dkey == dCurrentKey) {
						return index;
					}
				}
				
				index = next[index];
				if (index == -1) {
					return -1;
				}
				currentKey = keys[index];
			}
			
		}
		
		
		// Assume equality == identity for all types except for doubles?
		while (true) {
			if (key == currentKey) {
				return index;
			}
			index = next[index];
			if (index == -1) {
				return -1;
			}
			currentKey = unref(keys[index]);
		}		
	}
	
	public final int hash_primitiveNewKey(Object key, int mp) {		
		// assert key not in table
		// Assert key != null

		Object key2 = keys[mp];

		// mainPosition is unoccupied
		if (key2 == null) {
			if (weakKeys) {
				keys[mp] = new WeakReference(key);
			} else {
				keys[mp] = key;
			}
			next[mp] = -1;

			return mp;
		}

		// need to find a free index, either for key, or for the conflicting key
		// since java checks bounds all the time, using try-catch may be faster than manually
		// checking
		try {
			while (keys[--freeIndex] != null);
		} catch (ArrayIndexOutOfBoundsException e) {
			hash_rehash();
			// different mp, since it's been rehashed
			mp = getMP(key);
			return hash_primitiveNewKey(key, mp);
		}

		int mp2 = getMP(key2);
		// index is occupied by something with the same main index
		if (mp2 == mp) {
			if (weakKeys) {
				keys[freeIndex] = new WeakReference(key);
			} else {
				keys[freeIndex] = key;
			}
			next[freeIndex] = next[mp];

			next[mp] = freeIndex;
			return freeIndex;
		}

		// old key is not in its main position
		// move old key to free index
		keys[freeIndex] = keys[mp];
		values[freeIndex] = values[mp];
		next[freeIndex] = next[mp];

		keys[mp] = key;
		// unnecessary to set value - the main set method will do this.
		// values[mp] = null;
		next[mp] = -1;

		// fix next link for the moved key
		int prev = mp2;
		while (true) {
			int tmp = next[prev];
			if (tmp == mp) {
				next[prev] = freeIndex;
				break;
			}
			prev = tmp;
		}

		return mp;
	}

	private void hash_rehash() {
		Object[] oldKeys = keys;
		Object[] oldValues = values;
		int n = oldKeys.length;

		int capacity = 0;
		int i = n;
		while (i-- > 0) {
			if (oldKeys[i] != null && oldValues[i] != null) {
				capacity++;
			}
		}
		capacity = 2 * nearestPowerOfTwo(capacity);


		keys = new Object[capacity];
		values = new Object[capacity];
		next = new int[capacity];

		freeIndex = capacity;

		i = n;
		while (i-- > 0) {
			Object key = oldKeys[i];
			if (key != null) {
				Object value = oldValues[i];
				if (value != null) {
					int mp = luaHashcode(key) & (capacity - 1);
					int index = hash_primitiveNewKey(key, mp);
					values[index] = value;
				}
			}
		}
	}

	public LuaTable metatable;

	public final void rawset(Object key, Object value) {
    	checkKey(key);

    	int mp = getMP(key);
   		int index = hash_primitiveFindKey(key, mp);
   		if (index < 0) {
			index = hash_primitiveNewKey(key, mp);
   		}
    	if (weakValues) {
    		values[index] = new WeakReference(value);
    	} else {
    		values[index] = value;
    	}
	}

	public final Object rawget(Object key) {
    	checkKey(key);
    	
    	int mp = getMP(key);
  		int index = hash_primitiveFindKey(key, mp);
  		if (index >= 0) {
  			return unref(values[index]);
  		}

  		return null;
	}

	public static void checkKey(Object key) {
		BaseLib.luaAssert(key != null, "table index is nil");

    	if (key instanceof Double) {
    		BaseLib.luaAssert(!((Double) key).isNaN(), "table index is NaN");
    	}
	}

	public final Object next(Object key) {
		int index = 0;
		if (key != null) {
			int mp = luaHashcode(key) & (keys.length - 1);
			index = 1 + hash_primitiveFindKey(key, mp);
			if (index == 0) {
				return null;
			}
		}
	
		while (true) {
			if (index == keys.length) {
				return null;
			}
			Object next = unref(keys[index]);
			if (next != null && unref(values[index]) != null) {
				return next;
			}
			index++;
		}
	}

	public final int len() {
		int high = keys.length;
		int low = 0;
		while (low < high) {
			int middle = (high + low + 1) >> 1;
			Double key = LuaState.toDouble(middle);
			int mp = getMP(key);
			int index = hash_primitiveFindKey(key, mp);
			if (index == -1) {
				high = middle - 1;
			} else {
				low = middle;
			}
		}
		return low;
	}
	
	public static boolean luaEquals(Object a, Object b) {
		if (a == null || b == null) {
			return a == b;
		}
		if (a instanceof Double && b instanceof Double) {
			Double ad = (Double) a;
			Double bd = (Double) b;
			return ad.doubleValue() == bd.doubleValue();
		}
		return a == b;
	}
	
	
	public static int luaHashcode(Object a) {
		if (a instanceof Double) {
			Double ad = (Double) a;

			double ad_primitive = LuaState.fromDouble(ad); 
			if (ad_primitive == 0) {
				return 0;
			}
			return ad.hashCode();
		}
		return System.identityHashCode(a);
	}

	public void updateWeakSettings(boolean k, boolean v) {
		if (k != weakKeys) {
			fixWeakRefs(keys, k);
			weakKeys = k;
		}
		if (v != weakValues) {
			fixWeakRefs(values, v);
			weakValues = v;
		}
	}
	
	public void fixWeakRefs(Object[] entries, boolean weak) {
		if (entries == null) return;
		for (int i = entries.length - 1; i >= 0; i--) {
			Object o = entries[i];
			if (o == null) continue;
			if (weak) {
				if (!(o instanceof WeakReference || o instanceof String
						|| o instanceof Double || o instanceof Boolean)) {
					entries[i] = new WeakReference(o);
				}
			} else {
				if ((o instanceof WeakReference)) {
					entries[i] = ((WeakReference)o).get();
				}
			}
		}
	}
	
}
