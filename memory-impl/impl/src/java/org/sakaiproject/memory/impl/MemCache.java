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

import java.lang.ref.SoftReference;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.Vector;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.cover.ComponentManager;
import org.sakaiproject.event.api.Event;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.DerivedCache;
import org.sakaiproject.memory.api.MemoryService;

/**
 * An abstraction of the caching system for Sakai<br/>
 * This allows the developer to access a cache created for their use,
 * create this cache using the {@link MemoryService}
 */
public class MemCache implements Cache, Observer
{
   /** Our logger. */
   private static Log log = LogFactory.getLog(MemCache.class);

   /** Underlying cache implementation */
   protected net.sf.ehcache.Ehcache cache;

   /** The object that will deal with missing entries. */
   protected CacheRefresher m_refresher = null;

   /** If true, we are disabled. */
   protected boolean m_disabled = false;

   /** This is the notifier for this cache (if one is set) */
   protected DerivedCache m_derivedCache = null;


   /**
    * Create a MemCache object which gives us access to the cache and applies the
    * refresher (if one is supplied), <br/>
    * Note that the recommended usage is to simply
    * get a cache from the EhCache service instead
    * @param cache an ehCache (already initialized)
    * @param refresher (optional) an object that implements {@link CacheRefresher} or null
    * @param notifier (optional) an object that implements {@link DerivedCache} or null
    */
   public MemCache(Ehcache cache, CacheRefresher refresher, DerivedCache notifier) {
      // inject our dependencies
      if (cache == null) {
         throw new NullPointerException("cache must be set");
      }
      m_refresher = refresher;
      m_derivedCache = notifier;
      this.cache = cache;
   }


   public void attachDerivedCache(DerivedCache cache) {
      // Note: only one is supported
      if (cache == null) {
         m_derivedCache = null;
      } else {
         // TODO shouldn't we allow someone to attach a new listener? -AZ
         if (m_derivedCache != null) {
            log.warn("attachDerivedCache - already got one and will not override (not sure why we won't though)");
         } else {
            m_derivedCache = cache;
         }
      }
   }


   public void put(String key, Object payload) {
      put(key, payload, null);
   }

   public void put(String key, Object payload, String[] associatedKeys) {
      if (log.isDebugEnabled()) {
         log.debug("put(String " + key + ", Object " + payload + ", assocKeys " + associatedKeys + ")");
      }

      cache.put(new Element(key, payload));
      
      // TODO handle the associated keys
      if (associatedKeys != null) {
         if (log.isDebugEnabled()) {
            log.debug("associating "+associatedKeys.length+" keys with this key: " + key);
         }
         throw new UnsupportedOperationException("not supported yet, functionality is not in place");
      }

      if (m_derivedCache != null) m_derivedCache.notifyCachePut(key, payload);
   }


   public boolean containsKey(String key) {
      if (log.isDebugEnabled()) {
         log.debug("containsKey(Object " + key + ")");
      }

      return cache.isKeyInCache(key);
   } // containsKey


   public Object get(String key) {
      if (log.isDebugEnabled()) {
         log.debug("get(Object " + key + ")");
      }

      final Element e = cache.get(key);
      Object rv = null;
      if (e != null) {
         rv = e.getObjectValue();
      }

      return rv;

   } // get


   public void remove(String key) {
      if (log.isDebugEnabled()) {
         log.debug("remove(" + key + ")");
      }

      final Object value = get(key);
      cache.remove(key);

      if (m_derivedCache != null) m_derivedCache.notifyCacheRemove(key, value);
   } // remove


   public void clear() {
      log.debug("clear()");

      cache.removeAll();  //TODO Do we boolean doNotNotifyCacheReplicators? Ian? -I think we do want to notify the replicators unless we are trying to support clearing the cache on one server only (the repicator will refill this cache though) -AZ
      cache.clearStatistics();

      if (m_derivedCache != null) m_derivedCache.notifyCacheClear();

   } // clear

   public void destroy() {
      clear();
      cache.getCacheManager().removeCache(cache.getName());
   }

   
   /**********************************************************************************************************************************************************************************************************************************************************
    * Cacher implementation
    *********************************************************************************************************************************************************************************************************************************************************/

   public void resetCache() {
      log.debug("resetCache()");
      clear();
   } // resetCache

   public long getSize() {
      log.debug("getSize()");
      return cache.getStatistics().getObjectCount();
   }

   public String getDescription() {
      final StringBuilder buf = new StringBuilder();
      buf.append("MemCache");
      if (m_disabled) {
         buf.append(" disabled");
      }

      // TODO remove this part -AZ
      if (m_resourcePattern != null) {
         buf.append(" " + m_resourcePattern);
      }

      final long hits = cache.getStatistics().getCacheHits();
      final long misses = cache.getStatistics().getCacheMisses();
      final long total = hits + misses;
      buf.append("  hits:" + hits + "  misses:" + misses + "  hit%:"
            + ((total > 0) ? "" + ((100l * hits) / total) : "n/a"));

      return buf.toString();
   }

   


   /**********************************************************************************************************************
    * The next section of methods are deprecated -AZ
    */
   // TODO remove deprecated methods


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
      // inject our dependencies
// This was not actually needed and required a very odd constructor of sending the memory service along -AZ
//      m_memoryService = memoryService;
      m_eventTrackingService = eventTrackingService;
      this.cache = cache;
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
      this(memoryService, eventTrackingService, cache);
      log.warn("deprecated method, do not use");
      m_resourcePattern = pattern;
      if (refresher != null)
      {
         m_refresher = refresher;
      }

      // register to get events - first, before others
      if (pattern != null)
      {
         m_eventTrackingService.addPriorityObserver(this);
      }
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
      this(memoryService, eventTrackingService, cache);
      log.warn("deprecated method, do not use");
      if (refresher != null)
      {
         m_refresher = refresher;
      }
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
      this(memoryService, eventTrackingService, cache);
      log.warn("deprecated method, do not use");
      if (refresher != null)
      {
         m_refresher = refresher;
      }
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
      this(memoryService, eventTrackingService, pattern, cache);
      log.warn("deprecated method, do not use");
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

      // register to get events - first, before others
      if (pattern != null)
      {
         m_eventTrackingService.addPriorityObserver(this);
      }
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
      log.warn("deprecated method, do not use");
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
      log.warn("deprecated method, do not use");
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
      log.warn("deprecated method, do not use");
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
      log.warn("deprecated method, do not use");
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
         if (m_refresher != null)
         {
            // ask the refresher for the value
            Object value = m_refresher.refresh(key, oldValue, event);
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
            if (m_refresher != null)
            {
               // ask the refresher for the value
               Object value = m_refresher.refresh(key, oldValue, event);
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
            if ((m_refresher != null) && (key != null))
            {
               // ask the refresher for the value
               payload = m_refresher.refresh(key, null, null);

               // store this new value
               put(key, payload);
            }
         }

         return payload;
      }

   } // CacheEntry

}
