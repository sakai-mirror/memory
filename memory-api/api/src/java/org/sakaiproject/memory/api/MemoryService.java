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

import org.sakaiproject.memory.exception.MemoryPermissionException;

/**
 * MemoryService is the primary interface for the Sakai Memory service<br/>
 * This provides programmatic access to centralized caching in Sakai and methods for tracking memory use
 * 
 * @author Glenn Golden (ggolden@umich.edu)
 * @author Lance Speelmon (lance@iupui.edu)
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public interface MemoryService {

   /**
	 * Report the amount of unused and available memory for the JVM
	 * @return free memory in bytes
	 */
   public long getAvailableMemory();

   /**
    * Get a status report of memory cache usage
    * @return A string representing the current status of all caches
    */
   public String getStatus();

	/**
	 * Cause less memory to be used by clearing all caches,
	 * note that this used to cause a GC but that is no longer happening, 
	 * now only the caches are being cleared
	 * @exception PermissionException if the current user does not have super user permissions
	 */
   public void resetCachers() throws MemoryPermissionException;

   /**
    * Construct a Cache with the given name (often this is the fully qualified classpath of the api 
    * for the service that is being cached or the class if there is no api) or retrieve the one
    * that already exists with this name,
    * this will operate on system defaults and will be a distributed cache without replication
    * @param cacheName Load a defined bean from the application context with this name or create a default cache with this name
    * @return a cache which can be used to store objects
    */
	public Cache newCache(String cacheName);

   /**
    * Construct a Cache with the given name (often this is the fully qualified classpath of the api 
    * for the service that is being cached or the class if there is no api) or retrieve the one
    * that already exists with this name,
    * Automatic refresh handling if refresher is not null and notification of cache changes
    * if notifier is not null.
    * @param cacheName Load a defined bean from the application context with this name or create a default cache with this name
    * @param refresher The object that will handle refreshing of entries which are not found
    * @param notifer The object that will handle notifications which are a result of cache changes
    * @param distributed if true this cache will be distributed across the cluster, otherwise it is local to the current server only
    * @param replicated if true this cache will replicate cached objects across the cluster nodes, 
    * otherwise the cache will just expire entries in the other caches and they will have to reload on that cluster node
    * @return a cache which can be used to store objects
    */
   public Cache newCache(String cacheName, CacheRefresher refresher, DerivedCache notifer, boolean distributed, boolean replicated);

   /**
    * Flushes and destroys the cache with this name<br/>
    * @param cacheName unique name for this cache
    */
   public void destroyCache(String cacheName);


   // TODO - DEPRECATED METHODS BELOW - remove in next release (07/Oct/2007)

   /**
    * Construct a Cache. Attempts to keep complete on Event notification by calling the refresher.
    * 
    * @param cacheName Load a defined bean from ComponentManager or create a default cache with this name.
    * @param pattern
    *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for updates.
    * @deprecated pattern matching no longer needed or supported, 07/Oct/2007 -AZ
    */
   Cache newCache(String cacheName, String pattern);

  /**
    * Construct a Cache. Attempts to keep complete on Event notification by calling the refresher.
    * 
    * @param cacheName Load a defined bean from ComponentManager or create a default cache with this name.
    * @param refresher
    *        The object that will handle refreshing of event notified modified or added entries.
    * @param pattern
    *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for updates
    * @deprecated pattern matching no longer needed or supported, 07/Oct/2007 -AZ
    */
   Cache newCache(String cacheName, CacheRefresher refresher, String pattern);
	
	/**
	 * Register as a cache user
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 */
	void registerCacher(Cacher cacher);

	/**
	 * Unregister as a cache user
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 */
	void unregisterCacher(Cacher cacher);

	/**
	 * Construct a Cache. Attempts to keep complete on Event notification by calling the refresher.
	 * 
	 * @param refresher
	 *        The object that will handle refreshing of event notified modified or added entries.
	 * @param pattern
	 *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for updates.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String, CacheRefresher, String)
	 */
	Cache newCache(CacheRefresher refresher, String pattern);


	/**
	 * Construct a special Cache that uses hard references. Attempts to keep complete on Event notification by calling the refresher.
	 * 
	 * @param refresher
	 *        The object that will handle refreshing of event notified modified or added entries.
	 * @param pattern
	 *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for updates.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String, CacheRefresher, String)
	 */
	Cache newHardCache(CacheRefresher refresher, String pattern);

	/**
	 * Construct a Cache. Automatic refresh handling if refresher is not null.
	 * 
	 * @param refresher
	 *        The object that will handle refreshing of expired entries.
	 * @param sleep
	 *        The number of seconds to sleep between expiration checks.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String, CacheRefresher)
	 */
	Cache newCache(CacheRefresher refresher, long sleep);

	/**
	 * Construct a Cache. Automatic refresh handling if refresher is not null.
	 * 
	 * @param refresher
	 *        The object that will handle refreshing of expired entries.
	 * @param sleep
	 *        The number of seconds to sleep between expiration checks.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String, CacheRefresher)
	 */
	Cache newHardCache(CacheRefresher refresher, long sleep);

	/**
	 * Construct a Cache. No automatic refresh: expire only, from time and events.
	 * 
	 * @param sleep
	 *        The number of seconds to sleep between expiration checks.
	 * @param pattern
	 *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for expiration.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String, String)
	 */
	Cache newHardCache(long sleep, String pattern);

	/**
	 * Construct a Cache. No automatic refresh handling.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String)
	 */
	Cache newCache();

	/**
	 * Construct a Cache. No automatic refresh handling.
	 * @deprecated Since Sakai 2.5.0 (Sept 2007)
	 * @see MemoryService#newCache(String)
	 */
	Cache newHardCache();

	/**
	 * Construct a multi-ref Cache. No automatic refresh: expire only, from time and events.
	 * 
	 * @param sleep
	 *        The number of seconds to sleep between expiration checks.
	 * @deprecated Since Sakai 2.5.0
	 * @see MemoryService#newMultiRefCache(String)
	 */
	MultiRefCache newMultiRefCache(long sleep);

	/**
	 * Construct a multi-ref Cache. No automatic refresh: expire only, from time and events.
	 * 
	 * @param cacheName Load a defined bean from ComponentManager or create a default cache with this name.
	 * @deprecated Since 12/Oct/2007 - no longer supported to get a MRC this way, inject the cache directly
	 */
	MultiRefCache newMultiRefCache(String cacheName);

}
