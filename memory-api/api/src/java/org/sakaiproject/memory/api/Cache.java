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

import java.util.List;

/**
 * A Cache holds objects with keys with a limited lifespan
 * 
 * Major deprecation of methods here -AZ
 * "At one time, the cache represented the entire database.  When there was a change to an object, 
 * the cache called on the refresher to get a new version of the object so it can re-cache and be "complete" 
 * again.  There are a few features, like refresher and complete cache, that I believe are no longer used." -ggolden
 * 
 */
public interface Cache extends Cacher {

	/**
    * Cache an object which will be expired based on the sakai cache configuration
    * (cluster safe)
    * 
    * @param key
    *           The key with which to find the object.
    * @param payload
    *           The object to cache.
    */
   void put(Object key, Object payload);

   /**
    * Get the non expired entry, or null if not there (or expired)
    * (partially cluster safe)
    * 
    * @param key
    *           The cache key.
    * @return The payload, or null if the payload is null, the key is not found, or the entry has
    *         expired (Note: use containsKey() to remove this ambiguity).
    */
   Object get(Object key);

   /**
    * Test if an entry exists in the cache for a key
    * (partially cluster safe)
    * 
    * @param key
    *           The cache key.
    * @return true if the key maps to a non-expired cache entry, false if not.
    */
   boolean containsKey(Object key);

   /**
    * Get all the keys for this cache (cluster safe)
    * (partially cluster safe)
    * 
    * @return The List of key values (Object).
    */
   List<Object> getKeys();

   /**
    * Clear all entries.
    */
   void clear();

   /**
    * Remove this entry from the cache.
    * (cluster safe)
    * 
    * @param key
    *        The cache key.
    */
   void remove(Object key);

   /**
    * Clear all entries and shutdown the cache.
    * Don't attempt to use the cache after this call.
    * This does not really need to be called as the caches are shutdown and cleaned up automatically.
    */
   void destroy();

   /**
    * Attach this DerivedCache to the cache. The DerivedCache is then notified of the cache contents changing events.
    * This is basically a listener method.
    * (used by SiteCacheImpl only)
    * 
    * @param cache
    *        The DerivedCache to attach.
    */
   void attachDerivedCache(DerivedCache cache);


   /*
    * This does some kind of transactional thing but it requires the user to say...
    * transaction started and transaction ending, don't see any way to say, transaction failed though -AZ
    */

   /**
    * Set the cache to hold events for later processing to assure an atomic "complete" load.
    * (Used by BaseContentService and BaseMessageService)
    * @deprecated 07/OCT/2007 No longer supported, do not attempt to load all entries into a cache, it is bad practice
    */
   void holdEvents();

   /**
    * Restore normal event processing in the cache, and process any held events now.
    * (Used by BaseContentService and BaseMessageService)
    * @deprecated 07/OCT/2007 No longer supported, do not attempt to load all entries into a cache, it is bad practice
    */
   void processEvents();


   /*
    * These "complete" methods seems very strange, they are not like any caching I have
    * ever seen, recommend we deprecate and destroy these -AZ
    */

   /**
    * Are we complete?
    * 
    * @return true if we have all the possible entries cached, false if not.
    * (Used by BaseMessageService)
    * @deprecated 07/OCT/2007 No longer supported, do not attempt to load all entries into a cache, it is bad practice
    */
   boolean isComplete();

   /**
    * Set the cache to be complete, containing all possible entries.
    * (Used by BaseMessageService)
    * @deprecated 07/OCT/2007 No longer supported, do not attempt to load all entries into a cache, it is bad practice
    */
   void setComplete();

   /**
    * Are we complete for one level of the reference hierarchy?
    * 
    * @param path
    *        The reference to the completion level.
    * @return true if we have all the possible entries cached, false if not.
    * (Used by BaseContentService)
    * @deprecated 07/OCT/2007 No longer supported, do not attempt to load all entries into a cache, it is bad practice
    */
   boolean isComplete(String path);

   /**
    * Set the cache to be complete for one level of the reference hierarchy.
    * 
    * @param path
    *        The reference to the completion level.
    * (Used by BaseContentService)
    * @deprecated 07/OCT/2007 No longer supported, do not attempt to load all entries into a cache, it is bad practice
    */
   void setComplete(String path);


   /*
    * These are allowing users to enable and disable the cache...
    * why would someone ever want to do this? -AZ
    */
   
   /**
    * Disable the cache.
    * @deprecated 07/OCT/2007 Unused and no longer supported
    */
   void disable();

   /**
    * Enable the cache.
    * @deprecated 07/OCT/2007 Unused and no longer supported
    */
   void enable();

   /**
    * Is the cache disabled?
    * 
    * @return true if the cache is disabled, false if it is enabled.
    * @deprecated 07/OCT/2007 No longer supported, 
    * caches are always enabled when they are alive
    */
   boolean disabled();


   /**
    * Expire a cached object immediately
    * 
    * @param key
    *           The cache key.
    * @deprecated 07/OCT/2007 no longer used, see {@link #remove(Object)}
    */
   void expire(Object key);

   /**
    * Cache an object
    * 
    * @param key
    *        The key with which to find the object.
    * @param payload
    *        The object to cache.
    * @param duration
    *        The time to cache the object (seconds).
    * @deprecated Since Sakai 2.5.0 (Sept 2007)
    * @see Cache#put(Object, Object)
    */
   void put(Object key, Object payload, int duration);

   /**
	 * Test for an entry in the cache - expired or not.
	 * 
	 * @param key
	 *        The cache key.
	 * @return true if the key maps to a cache entry, false if not.
	 * @deprecated Since Sakai 2.5.0
	 * @see Cache#containsKey(Object)
	 */
	boolean containsKeyExpiredOrNot(Object key);


	/**
	 * Get the entry, or null if not there (expired entries are returned, too).
	 * 
	 * @param key
	 *        The cache key.
	 * @return The payload, or null if the payload is null, the key is not found. (Note: use containsKey() to remove this ambiguity).
	 * @deprecated Since Sakai 2.5.0
	 * @see Cache#get(Object)
	 */
	Object getExpiredOrNot(Object key);

	/**
	 * Get all the non-expired non-null entries.
	 * 
	 * @return all the non-expired non-null entries, or an empty list if none.
	 * @deprecated Since Sakai 2.5.0
	 */
	List getAll();

	/**
	 * Get all the non-expired non-null entries that are in the specified reference path. Note: only works with String keys.
	 * 
	 * @param path
	 *        The reference path.
	 * @return all the non-expired non-null entries, or an empty list if none.
	 * @deprecated Since Sakai 2.5.0
	 */
	List getAll(String path);

	/**
	 * Get all the keys, modified from resource references to ids by removing the resource prefix. Note: only works with String keys.
	 * 
	 * @return The List of keys converted from references to ids (String).
	 * @deprecated Since Sakai 2.5.0
	 */
	List getIds();

}
