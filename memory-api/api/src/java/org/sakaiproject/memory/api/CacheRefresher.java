/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006 The Sakai Foundation.
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

import org.sakaiproject.event.api.Event;

/**
 * This defines a kind of listener method which will be called whenever a cache miss occurs.
 * In other words, if the cache is asked to retrieve an object by a key which does not exist
 * in the cache then this method will be called if defined for that cache. Then the returned
 * value will be returned from the lookup (or a null of no new value was found) and
 * also inserted into the cache (unless the value was null)<br/>
 * <b>WARNING:</b> This can make your cache misses very costly so you will want to be careful
 * about what you make this method actually do
 * <br/>
 * Original comment:<br/>
 * Utility API for classes that will refresh a cache entry when expired.
 */
public interface CacheRefresher {

   /**
    * Attempt to retrieve a value for this key from the cache user when none can be found in the cache
	 * 
	 * @param key
    *        The cache key whose value was not found in the cache
	 * @param oldValue (ALWAYS NULL)
	 *        The old exipred value of the key.
	 * @param event (ALWAYS NULL)
	 *        The event which triggered this refresh.
    * @return a new value for use in the cache for this key or null if no value exists for this item
	 * @deprecated this method will eventually drop the oldValue and event params and be replaced
	 * by one with a signature like: Object refresh(Object key); 07/Oct/2007
	 */
	public Object refresh(Object key, Object oldValue, Event event);
//   public Object refresh(Object key);

}
