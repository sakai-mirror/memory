/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2006 The Sakai Foundation.
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
 * This allows a developer to track changes that are happening in a cache. This is basically a listener
 * for cache events which is attached to the cache via methods in the {@link Cache} or the {@link MemoryService}. 
 * Recommend that you use the ehcache CacheEventListener instead if you are getting an
 * ehcache directly.<br/>
 * <b>NOTE:</b> This is named in a confusing way and should be renamed
 * Here is the original comment for this API:
 * <p>
 * A DerivedCache provides some additional caching derived from the primary data in a Cache. 
 * It is directly accessed by the client for this derived data. 
 * It is notified by the primary cache it is attached to when the cache contents change.
 * </p>
 * @author Glenn Golden (ggolden@umich.edu)
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public interface DerivedCache {
   // TODO - rename this to CacheNotificationHandler

   /**
	 * This method is called when an object is added to the associated cache
	 * (it is not called when there is an update)
	 * 
	 * @param key the key for the object being placed in the cache
	 * @param payload the object being placed in the cache
	 */
	void notifyCachePut(String key, Object payload);

	/**
	 * This method is called when all cache items are cleared from the associated cache
	 */
	void notifyCacheClear();

	/**
	 * This method is called when an object is removed from the associated cache
	 * (this is only called when the object is explicitly removed and not when 
	 * it expires, if you want to know when objects expire you should look at
	 * {@link CacheRefresher})
	 * 
    * @param key the key for the object being placed in the cache
    * @param payload the object being placed in the cache
	 */
	void notifyCacheRemove(String key, Object payload);
}
