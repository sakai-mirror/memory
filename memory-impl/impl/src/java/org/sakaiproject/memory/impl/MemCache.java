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

package org.sakaiproject.memory.impl;

import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.Vector;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.CacheConfig;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.DerivedCache;

/**
 * An abstraction of a cache for the caching system for Sakai<br/>
 * This allows the developer to access a cache created for their use,
 * create this cache using the {@link MemoryService}
 */
public class MemCache implements Cache, CacheEventListener {

   /** Our logger. */
   private static Log log = LogFactory.getLog(MemCache.class);

   /** Underlying cache implementation */
   protected Ehcache cache;

   /**
    * The configuration for this cache
    */
   protected CacheConfig config = new CacheConfig();


   /**
    * Create a MemCache object which gives us access to the cache and applies the
    * refresher (if one is supplied)<br/>
    * We do not recommend creating this manually, you should instead use {@link MemoryService#newCache(String)}
    * @param cache an ehCache (already initialized)
    * @param cacheConfig the configuration for this cache, see {@link CacheConfig} for details
    */
   public MemCache(Ehcache cache, CacheConfig cacheConfig) {
      // setup the cache
      if (cache == null) {
         throw new NullPointerException("cache must be set");
      } else {
         if (cache.getStatus() != Status.STATUS_ALIVE) {
            throw new IllegalArgumentException("Cache must already be initialized and alive");
         }
      }
      this.cache = cache;

      if (cacheConfig != null) {
         config = cacheConfig;
      }

      // register the notifier
      attachDerivedCache(config.getNotifer());
      attachLoader(config.getLoader());

      // set to all false if null to avoid NPEs, this should not really happen because this should be coming in all setup from the MemoryService
      if (config.getDistributed() == null) {
         config.setDistributed(false);
      }
      if (config.getBlocking() == null) {
         config.setBlocking(false);
      }
      if (config.getReplicated() == null) {
         config.setReplicated(false);
      }
   }

   public void put(String key, Object payload) {
      if (log.isDebugEnabled()) log.debug("put(String " + key + ", Object " + payload + ")");

      if (key == null || "".equals(key)) {
         throw new IllegalArgumentException("key cannot be null or empty string");
      }

      if (config.getDistributed() == true || cache.isOverflowToDisk()) {
         if (! (key instanceof Serializable)) {
            log.warn("Using non-serializable key for distributed or disk stored cache (" + cache.getName() +
            ") is bad since things will not work, please make sure your key is serializable");
         } else if (! (payload instanceof Serializable)) {
            log.warn("Using non-serializable payload for distributed or disk stored cache (" + cache.getName() + 
            ") is bad since things will not work, please make sure your payload is serializable");
         }
      }

      cache.put(new Element(key, payload));
   }


   public boolean containsKey(String key) {
      if (log.isDebugEnabled()) log.debug("containsKey(Object " + key + ")");

      if (key == null || "".equals(key)) {
         // this is here for backwards compatibility
         log.warn("key should not be null or empty string, this will always yield false");
      }

      return cache.isKeyInCache(key);
   }


   public Object get(String key) {
      if (log.isDebugEnabled()) log.debug("get(Object " + key + ")");

      if (key == null || "".equals(key)) {
         throw new IllegalArgumentException("key cannot be null or empty string");
      }

      Object payload = null;
      if (containsKey(key)) {
         payload = getCachePayload(key);
      } else {
         if (config.getLoader() != null) {
            // TODO - handle this with a blocking cache before merging -AZ
            payload = config.getLoader().refresh(key, null, null);
            if (payload != null) {
               put(key, payload);
            }
         }
      }
      final Object value = payload;
      return value;
   }

   /**
    * Retrieve a payload from the cache for this key if one can be found
    * @param key the key for this cache element
    * @return the payload or null if none found
    */
   private Object getCachePayload(String key) {
      Object payload = null;
      Element e = cache.get(key);
      if (e != null) {
         // attempt to get the serialized value first
         if (e.isSerializable()) {
            payload = e.getValue();
         } else {
            // not serializable so get the object value
            payload = e.getObjectValue();
         }
      }
      return payload;
   }


   public void remove(String key) {
      if (log.isDebugEnabled()) log.debug("remove(" + key + ")");

      // disabled for backwards compatibility -AZ
//    if (key == null || "".equals(key)) {
//    throw new IllegalArgumentException("key cannot be null or empty string");
//    }

      if (containsKey(key)) {
         cache.remove(key);
      }
   }


   public void clear() {
      resetCache();
   }


   public void attachDerivedCache(DerivedCache cacheEventListener) {
      if (cacheEventListener == null) {
         // unregister this class as a listener if this is null
         cache.getCacheEventNotificationService().unregisterListener(this);
      } else {
         // register this class as a listener for itself if notifier is set
         cache.getCacheEventNotificationService().registerListener(this);
      }
      config.setNotifer(cacheEventListener);
   }


   public void attachLoader(CacheRefresher cacheLoader) {
      config.setLoader(cacheLoader);
   }


   /**********************************************************************************************************************************************************************************************************************************************************
    * Cacher implementation
    *********************************************************************************************************************************************************************************************************************************************************/

   public void resetCache() {
      log.debug("clearing cache");

      cache.removeAll();  //TODO Do we boolean doNotNotifyCacheReplicators? Ian? -I think we do want to notify the replicators unless we are trying to support clearing the cache on one server only (the repicator will refill this cache though) -AZ
      clearCache();
   } // resetCache

   /**
    * clear the stats and send the notification
    */
   private void clearCache() {
      cache.clearStatistics();
      if (config.getNotifer() != null) config.getNotifer().notifyCacheClear();
   }

   public long getSize() {
      log.debug("getSize()");
      return cache.getSize();
   }

   public String getDescription() {
      // just using the method from the memory service for now, probably should be a util -AZ
      return BasicMemoryService.generateCacheStats(cache);
   }

   /********************************************************************
    * EhCache listener (to handle cache notifications)
    */

   public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
      if (config.getNotifer() != null) {
         // get the key and then value to send along to the notifier
         String key = element.getKey().toString();
         config.getNotifer().notifyCachePut(key, getCachePayload(key) );
      }
   }

   public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
      if (config.getNotifer() != null) {
         // get the key and then value to send along to the notifier
         String key = element.getKey().toString();
         config.getNotifer().notifyCacheRemove(key, getCachePayload(key) );
      }
   }

   public void notifyRemoveAll(Ehcache cache) {
      clearCache(); // this calls the notifier if there is one
   }

   public void notifyElementEvicted(Ehcache cache, Element element) {
      // do nothing
   }

   public void notifyElementExpired(Ehcache cache, Element element) {
      // do nothing
   }

   public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
      // do nothing
   }

   public void dispose() {
      // do nothing
   }

   /**
    * @see CacheEventListener#clone()
    */
   @Override
   public Object clone() throws CloneNotSupportedException {
      // Creates a clone of this listener. This method will only be called by ehcache before a cache is initialized.
      throw new CloneNotSupportedException("MemCache does not support clone");
   }




   /**********************************************************************************************************************
    * The next section of methods are deprecated -AZ
    */
   // TODO remove deprecated methods

   /** @deprecated */
   public void destroy() {
      clear();
   }

   /** The string that all resources in this cache will start with. 
    * @deprecated */
   protected String m_resourcePattern = null;

   /** If true, we have all the entries that there are in the cache. 
    * @deprecated */
   protected boolean m_complete = false;

   /** Alternate isComplete, based on patterns. 
    * @deprecated */
   protected Set<String> m_partiallyComplete = new HashSet<String>();

   /** If true, we are going to hold any events we see in the m_heldEvents list for later processing. 
    * @deprecated */
   protected boolean m_holdEventProcessing = false;

   /** The events we are holding for later processing. 
    * @deprecated */
   protected List<Event> m_heldEvents = new Vector<Event>();

   /**
    * @deprecated
    */
   protected EventTrackingService m_eventTrackingService = null;

   /**
    * Construct the Cache. No automatic refresh handling.
    * @deprecated 07/Oct/2007 -AZ
    */
   public MemCache(BasicMemoryService memoryService,
         EventTrackingService eventTrackingService, Ehcache cache)
   {
      this(cache, null);
      log.warn("deprecated MemCache constructor, do not use");
      m_eventTrackingService = eventTrackingService;
   }

   /**
    * Construct the Cache. Attempts to keep complete on Event notification by calling the refresher.
    * 
    * @param refresher
    *        The object that will handle refreshing of event notified modified or added entries.
    * @param pattern
    *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for updates.
    * @deprecated 07/Oct/2007, pattern and event not used anymore -AZ
    */
   public MemCache(BasicMemoryService memoryService,
         EventTrackingService eventTrackingService,
         CacheRefresher refresher, String pattern, Ehcache cache)
   {
      this(cache, null);
      config.setLoader(refresher);
      log.warn("deprecated MemCache constructor, do not use");
      m_eventTrackingService = eventTrackingService;
      m_resourcePattern = pattern;

      // register to get events - first, before others
//    if (pattern != null)
//    {
//    m_eventTrackingService.addPriorityObserver(this);
//    }
   }

   /**
    * Construct the Cache. Automatic refresh handling if refresher is not null.
    * 
    * @param refresher
    *        The object that will handle refreshing of expired entries.
    * @param sleep
    *        The number of seconds to sleep between expiration checks.
    * @deprecated long sleep no longer used with ehcache
    */
   public MemCache(BasicMemoryService memoryService,
         EventTrackingService eventTrackingService,
         CacheRefresher refresher, long sleep, Ehcache cache)
   {
      this(cache, null);
      config.setLoader(refresher);
      log.warn("deprecated MemCache constructor, do not use");
      m_eventTrackingService = eventTrackingService;
   }

   /**
    * Construct the Cache. Automatic refresh handling if refresher is not null.
    * 
    * @param refresher
    *        The object that will handle refreshing of expired entries.
    * @deprecated 07/OCT/2007 event not used anymore
    */
   public MemCache(BasicMemoryService memoryService,
         EventTrackingService eventTrackingService,
         CacheRefresher refresher, Ehcache cache)
   {
      this(cache, null);
      config.setLoader(refresher);
      log.warn("deprecated MemCache constructor, do not use");
      m_eventTrackingService = eventTrackingService;
   }

   /**
    * Construct the Cache. Event scanning if pattern not null - will expire entries.
    * 
    * @param sleep
    *        The number of seconds to sleep between expiration checks.
    * @param pattern
    *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for expiration.
    * @deprecated long sleep no longer used with ehcache
    */
   public MemCache(BasicMemoryService memoryService,
         EventTrackingService eventTrackingService, long sleep,
         String pattern, Ehcache cache)
   {
      this(cache, null);
      log.warn("deprecated MemCache constructor, do not use");
      m_eventTrackingService = eventTrackingService;
      m_resourcePattern = pattern;
   }

   /**
    * Construct the Cache. Event scanning if pattern not null - will expire entries.
    * 
    * @param sleep
    *        The number of seconds to sleep between expiration checks.
    * @param pattern
    *        The "startsWith()" string for all resources that may be in this cache - if null, don't watch events for expiration.
    * @deprecated pattern no longer used with ehcache
    */
   public MemCache(BasicMemoryService memoryService,
         EventTrackingService eventTrackingService, String pattern,
         Ehcache cache)
   {
      this(memoryService, eventTrackingService, cache);
      log.warn("deprecated method, do not use");
      m_resourcePattern = pattern;
   }



   /**
    * Cache an object
    * 
    * @param key
    *        The key with which to find the object.
    * @param payload
    *        The object to cache.
    * @param duration
    *        The time to cache the object (seconds).
    * @deprecated time limit set no longer supported
    */
   public void put(String key, Object payload, int duration)
   {
      log.warn("deprecated method, do not use: put(String key, Object payload, int duration)");
      put(key, payload);
   }

   /**
    * Test for an entry in the cache - expired or not.
    * 
    * @param key
    *        The cache key.
    * @return true if the key maps to a cache entry, false if not.
    * @deprecated 07/OCT/2007 No longer supported, will die if called
    */
   public boolean containsKeyExpiredOrNot(String key)
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    return containsKey(key);
   } // containsKeyExpiredOrNot

   /**
    * @deprecated
    */
   public void expire(String key)
   {
      log.warn("deprecated method, do not use: expire(String key)");
      // remove it
      remove(key);

   } // expire

   /**
    * Get the entry, or null if not there (expired entries are returned, too).
    * 
    * @param key
    *        The cache key.
    * @return The payload, or null if the payload is null, the key is not found. (Note: use containsKey() to remove this ambiguity).
    * @deprecated 07/OCT/2007 No longer supported, will die if called
    */
   public Object getExpiredOrNot(String key) {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    return get(key);

   } // getExpiredOrNot

   /**
    * Get all the non-expired non-null entries.
    * 
    * @return all the non-expired non-null entries, or an empty list if none.
    * @deprecated
    */
   public List getAll()
   { //TODO Why would you ever getAll objects from cache? -Lance, Seriously -AZ
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    return Collections.emptyList();

//    final List<Object> keys = cache.getKeysWithExpiryCheck();
//    final List<Object> rv = new ArrayList<Object>(keys.size()); // return value
//    for (Object key : keys) {
//    final Object value = cache.get(key).getObjectValue();
//    if (value != null)
//    rv.add(value);
//    }

//    return rv;

   } // getAll

   /**
    * Get all the non-expired non-null entries that are in the specified reference path. Note: only works with String keys.
    * 
    * @param path
    *        The reference path.
    * @return all the non-expired non-null entries, or an empty list if none.
    * @deprecated
    */
   public List getAll(String path)
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    if (M_log.isDebugEnabled()) {
//    M_log.debug("getAll(String " + path + ")");
//    }

//    if (disabled()) return Collections.emptyList();

//    final List<Object> keys = cache.getKeysWithExpiryCheck();
//    final List<Object> rv = new ArrayList<Object>(keys.size()); // return value
//    for (Object key : keys) {
//    // take only if keys start with path, and have no SEPARATOR following other than at the end %%%
//    if (key instanceof String && referencePath((String) key).equals(path)) {
//    rv.add(cache.get(key).getObjectValue());
//    }
//    }
//    return rv;

   } // getAll

   public List getKeys()
   {
      log.warn("deprecated method, do not use: getKeys()");
      return cache.getKeys();

   } // getKeys

   public List getIds() {
      throw new UnsupportedOperationException("deprecated method, no longer functional");

//    M_log.debug("getIds()");

//    if (disabled())
//    return Collections.emptyList();

//    final List<Object> keys = cache.getKeysWithExpiryCheck();
//    final List<Object> rv = new ArrayList<Object>(keys.size()); // return
//    // value
//    for (Object key : keys) {
//    if (key instanceof String) {
//    int i = ((String) key).indexOf(m_resourcePattern);
//    if (i != -1)
//    key = ((String) key).substring(i
//    + m_resourcePattern.length());
//    rv.add(key);
//    }
//    }
//    return rv;

   } // getIds

   public void disable()
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");

//    m_disabled = true;
//    m_eventTrackingService.deleteObserver(this);
//    clear();

   } // disable

   public void enable()
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");

      //		M_log.debug("enable()");

//    m_disabled = false;

//    if (m_resourcePattern != null)
//    {
//    m_eventTrackingService.addPriorityObserver(this);
//    }

   } // enable

   public boolean disabled()
   {
      // never disabled anymore
      log.warn("deprecated method, do not use: disabled()");
      return false;

   } // disabled

   public boolean isComplete()
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
   } // isComplete

   public void setComplete()
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
   } // isComplete

   public boolean isComplete(String path)
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
   } // isComplete

   public void setComplete(String path)
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
   } // setComplete

   public void holdEvents()
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
   } // holdEvents

   public void processEvents()
   {
      throw new UnsupportedOperationException("deprecated method, no longer functional");
   } // holdEvents



   /**********************************************************************************************************************************************************************************************************************************************************
    * Observer implementation
    *********************************************************************************************************************************************************************************************************************************************************/

   /**
    * This method is called whenever the observed object is changed. An application calls an <tt>Observable</tt> object's <code>notifyObservers</code> method to have all the object's observers notified of the change. default implementation is to
    * cause the courier service to deliver to the interface controlled by my controller. Extensions can override.
    * 
    * @param o
    *        the observable object.
    * @param arg
    *        an argument passed to the <code>notifyObservers</code> method.
    * @deprecated Not using events anymore
    */
   public void update(Observable o, Object arg)
   {
      log.warn("deprecated method, do not use: update(Observable o, Object arg)");

      if (disabled()) return;

      // arg is Event
      if (!(arg instanceof Event)) return;
      Event event = (Event) arg;

      // if this is just a read, not a modify event, we can ignore it
      if (!event.getModify()) return;

      String key = event.getResource();

      // if this resource is not in my pattern of resources, we can ignore it
      if (!key.startsWith(m_resourcePattern)) return;

      // if we are holding event processing
      if (m_holdEventProcessing)
      {
         m_heldEvents.add(event);
         return;
      }

      continueUpdate(event);

   } // update

   /**
    * Complete the update, given an event that we know we need to act upon.
    * 
    * @param event
    *        The event to process.
    * @deprecated Not using events anymore
    */
   protected void continueUpdate(Event event)
   {
      log.warn("deprecated method, do not use: continueUpdate(Event event)");

      String key = event.getResource();

      if (log.isDebugEnabled())
         log.debug(this + ".update() [" + m_resourcePattern
               + "] resource: " + key + " event: " + event.getEvent());

      // do we have this in our cache?
      Object oldValue = get(key);
      if (oldValue != null)
      {
         // invalidate our copy
         remove(key);
      }

      // if we are being complete, we need to get this cached.
      if (m_complete)
      {
         // we can only get it cached if we have a refresher
         if (config.getLoader() != null)
         {
            // ask the refresher for the value
            Object value = config.getLoader().refresh(key, oldValue, event);
            if (value != null)
            {
               put(key, value);
            }
         }
         else
         {
            // we can no longer claim to be complete
            m_complete = false;
         }
      }

      // if we are partially complete
      else if (!m_partiallyComplete.isEmpty())
      {
         // what is the reference path that this key lives within?
         String path = referencePath(key);

         // if we are partially complete for this path
         if (m_partiallyComplete.contains(path))
         {
            // we can only get it cached if we have a refresher
            if (config.getLoader() != null)
            {
               // ask the refresher for the value
               Object value = config.getLoader().refresh(key, oldValue, event);
               if (value != null)
               {
                  put(key, value);
               }
            }
            else
            {
               // we can no longer claim to be complete for this path
               m_partiallyComplete.remove(path);
            }
         }
      }

   } // continueUpdate

   /**
    * Compute the reference path (i.e. the container) for a given reference.
    * 
    * @param ref
    *        The reference string.
    * @return The reference root for the given reference.
    * @deprecated Not using events anymore
    */
   protected String referencePath(String ref)
   {
      log.warn("deprecated method, do not use: referencePath(String ref)");
      String path = null;

      // Note: there may be a trailing separator
      int pos = ref.lastIndexOf("/", ref.length() - 2);

      // if no separators are found, place it even before the root!
      if (pos == -1)
      {
         path = "";
      }

      // use the string up to and including that last separator
      else
      {
         path = ref.substring(0, pos + 1);
      }

      return path;

   } // referencePath


   /**
    * The cache entry. Holds a time stamped payload.
    * @deprecated No longer used for main cache, will be removed -AZ
    */
   protected class CacheEntry extends SoftReference
   {
      /** Set if our payload is supposed to be null. */
      protected boolean m_nullPayload = false;

      /**
       * Construct to cache the payload for the duration.
       * 
       * @param payload
       *        The thing to cache.
       * @param duration
       *        The time (seconds) to keep this cached.
       * @deprecated lance speelmon
       */
      public CacheEntry(Object payload, int duration)
      {
         // put the payload into the soft reference
         super(payload);
         log.warn("deprecated CacheEntry constructor, do not use");


         // is it supposed to be null?
         m_nullPayload = (payload == null);

      } // CacheEntry

      /**
       * Get the cached object.
       * 
       * @param key
       *        The key for this entry (if null, we won't try to refresh if missing)
       * @return The cached object.
       * @deprecated No longer used for main cache -AZ
       */
      public Object getPayload(String key)
      {
         log.warn("deprecated CacheEntry method, do not use: getPayload(String key)");

         // if we hold null, this is easy
         if (m_nullPayload)
         {
            return null;
         }

         // get the payload
         Object payload = this.get();

         // if it has been garbage collected, and we can, refresh it
         if (payload == null)
         {
            if ((config.getLoader() != null) && (key != null))
            {
               // ask the refresher for the value
               payload = config.getLoader().refresh(key, null, null);

               // store this new value
               put(key, payload);
            }
         }

         return payload;
      }

   } // CacheEntry

}
