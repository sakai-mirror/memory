/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
 *
 * Licensed under the Educational Community License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/ecl1.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/

package org.sakaiproject.memory.api;

/**
 * <pre>
 * This is a simplified multiple reference cache.
 * Objects on the cache can either have forward or
 * reverse dependencies. 
 * When an item is added its dependencies are recorded,
 * when an item is removed the dependencies are removed.
 * when an item is re-put, the dependencies are removed and the object updated
 * and the dependencies updated.
 * 
 * Any cluster implementation of this interfae will automatically perform 
 * cache operations over the whole cache, the consuming service should not
 * have to and should not perform any cluster wide invalidation, only 
 * concerning itself with its own invalidations.
 * 
 * </pre>
 * @author ieb
 *
 */
public interface MultipleReferenceCache
{
	/**
	 * Add an object to the cache, bound to the key, with a set of dependencies
	 * The dependencies may be forward or reverse dependencies.
	 * @param key A key for the object
	 * @param dependentKeys keys for related objects, either this object is dependant on these, or these are dependant on this object
	 * @param payload the payload of save
	 */
	void put(Object key, Object[] dependentKeys, Object payload);
	
	/**
	 * Get the keyed object, if not present an ObjectNotCachedException will be thrown
	 * @param key
	 * @return
	 * @throws ObjectNotCachedException
	 */
	Object get(Object key) throws ObjectNotCachedException;
	
	/**
	 * Remove the object
	 * @param key
	 */
	void remove(Object key) throws ObjectNotCachedException;
	
	/**
	 * Retruns true if the object exists in the cache
	 * @param key the key for the object
	 */
	boolean exists(Object key);

}
