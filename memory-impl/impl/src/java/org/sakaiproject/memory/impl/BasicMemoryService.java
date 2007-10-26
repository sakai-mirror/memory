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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.SecurityService;
import org.sakaiproject.event.api.EventTrackingService;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.Cacher;
import org.sakaiproject.memory.api.DerivedCache;
import org.sakaiproject.memory.api.MemoryService;
import org.sakaiproject.memory.api.MultiRefCache;
import org.sakaiproject.memory.exception.MemoryPermissionException;
import org.sakaiproject.tool.api.SessionManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>
 * BasicMemoryService is an implementation for the MemoryService
 * </p>
 * Major deprecation and refactoring by AZ
 */
public class BasicMemoryService implements MemoryService, ApplicationContextAware {

   /** Our logger. */
   private static Log M_log = LogFactory.getLog(BasicMemoryService.class);

   // using this instead of the ComponentManager so we can test -AZ
   private ApplicationContext applicationContext;
   public void setApplicationContext(ApplicationContext applicationContext) {
      this.applicationContext = applicationContext;
   }

   /**
    * Stores the record of all the created caches
    */
   protected Map<String, Cache> cachesRecord = new ConcurrentHashMap<String, Cache>();

   /** Event for the memory reset. */
   protected static final String EVENT_RESET = "memory.reset";

   /** The underlying cache manager; injected */
   protected net.sf.ehcache.CacheManager cacheManager;
   public void setCacheManager(net.sf.ehcache.CacheManager cacheManager) {
      this.cacheManager = cacheManager;
   }

   protected SecurityService securityService;
   public void setSecurityService(SecurityService securityService) {
      this.securityService = securityService;
   }

   protected SessionManager sessionManager;
   public void setSessionManager(SessionManager sessionManager) {
      this.sessionManager = sessionManager;
   }

   protected EventTrackingService eventTrackingService;
   public void setEventTrackingService(EventTrackingService eventTrackingService) {
      this.eventTrackingService = eventTrackingService;
   }


   public void init() {
      M_log.info("init()");

      if (cacheManager == null)
         throw new IllegalStateException("CacheManager was not injected properly!");

//    try {
//    get notified of events to watch for a reset
//    eventTrackingService().addObserver(this); // No longer using event tracking with cache
//    } catch (Throwable t) {
//    M_log.warn("init(): ", t);
//    }
   } // init

   /**
    * Returns to uninitialized state.
    */
   public void destroy() {
      M_log.info("destroy()");

      // if we are not in a global shutdown, remove my event notification registration
      // if (!ComponentManager.hasBeenClosed()) {
      // eventTrackingService().deleteObserver(this);
      // }

      cacheManager.clearAll();
      cachesRecord.clear();
   }


   public long getAvailableMemory() {
      return Runtime.getRuntime().freeMemory();
   } // getAvailableMemory

   public String getStatus() {
      final StringBuilder sb = new StringBuilder();

      sb.append("** Memory report\n");
      sb.append("freeMemory: " + Runtime.getRuntime().freeMemory());
      sb.append("\n");
      sb.append(" totalMemory: " + Runtime.getRuntime().totalMemory());
      sb.append("\n");
      sb.append(" maxMemory: " + Runtime.getRuntime().maxMemory());
      sb.append("\n");


      // caches summary
      sb.append("\n** Cache descriptions for registered caches ("+cachesRecord.size()+"):\n");
      for (Cache cache : cachesRecord.values()) {
         sb.append(" * " + cache.getDescription() );
         sb.append("\n");
      }

      // extended report
      List<Ehcache> allCaches = getAllCaches(true);
      sb.append("\n** Full report of all known caches ("+allCaches.size()+"):\n");
      for (Ehcache ehcache : allCaches) {
         sb.append(" * " + generateCacheStats(ehcache));
         sb.append("\n");
         sb.append(ehcache.toString());
         sb.append("\n");
      }

      final String rv = sb.toString();
      M_log.info(rv);

      return rv;
   }


   /**
    * Generate some stats for this cache
    */
   protected static String generateCacheStats(Ehcache cache) {
      StringBuilder sb = new StringBuilder();
      sb.append(cache.getName() + ":");
      final long hits = cache.getStatistics().getCacheHits();
      final long misses = cache.getStatistics().getCacheMisses();
      final String hitPercentage = ((hits+misses) > 0) ? ((100l * hits) / (hits + misses)) + "%" : "N/A";
      final String missPercentage = ((hits+misses) > 0) ? ((100l * misses) / (hits + misses)) + "%" : "N/A";
      sb.append("  Size: " + cache.getSize() + " [memory:" + cache.getMemoryStoreSize() + 
            ", disk:" + cache.getDiskStoreSize() + "]");
      sb.append(",  Hits: " + hits + " [memory:" + cache.getStatistics().getInMemoryHits() + 
            ", disk:" + cache.getStatistics().getOnDiskHits() + "] (" + hitPercentage + ")");
      sb.append(",  Misses: " + misses + " (" + missPercentage + ")");
      return sb.toString();
   }


   public Cache newCache(String cacheName) {
      return newCache(cacheName, null, null, true, false);
   }

   public Cache newCache(String cacheName, CacheRefresher refresher, DerivedCache notifer,
         boolean distributed, boolean replicated) {
      if (cacheName == null || "".equals(cacheName)) {
         throw new IllegalArgumentException("cacheName cannot be null or empty string");
      }

      // TODO - handle the distributed and replicated settings
      if (distributed || replicated) {
         M_log.warn("boolean distributed, boolean replicated not being handled yet");
      }

      Cache c = cachesRecord.get(cacheName);
      if (c == null) {
         c = new MemCache(instantiateCache(cacheName), refresher, notifer);
      }
      cachesRecord.put(cacheName, c);

      return c;
   }



   public void resetCachers() throws MemoryPermissionException {
      // check that this is a "super" user with the security service
      if (!securityService.isSuperUser()) {
         throw new MemoryPermissionException(sessionManager.getCurrentSessionUserId(), EVENT_RESET, "");
      }

      // call reset which should clear all distributed caches as well
      doReset();

      // post the event so this and any other app servers in the cluster will reset
      // eventTrackingService().post(eventTrackingService().newEvent(EVENT_RESET, "", true));

   } // resetMemory


   /**
    * Return all caches from the CacheManager
    * 
    * @param sorted
    *           Should the caches be sorted by name?
    * @return
    */
   private List<Ehcache> getAllCaches(boolean sorted) {
      M_log.debug("getAllCaches()");

      final String[] cacheNames = cacheManager.getCacheNames();
      if (sorted)
         Arrays.sort(cacheNames);
      final List<Ehcache> caches = new ArrayList<Ehcache>(cacheNames.length);
      for (String cacheName : cacheNames) {
         caches.add(cacheManager.getEhcache(cacheName));
      }
      return caches;
   }

   /**
    * Do a reset of all caches
    */
   public void doReset() {
      M_log.debug("doReset()");

      final List<Ehcache> allCaches = getAllCaches(false);
      for (Ehcache ehcache : allCaches) {
         ehcache.removeAll(); // TODO should we doNotNotifyCacheReplicators? Ian?
         ehcache.clearStatistics();
      }

      // run the garbage collector now
      System.runFinalization();
      // System.gc(); // DO NOT CALL THIS! It is a bad idea AND ehcache explicitly requires this to
      // be disabled to work correctly -AZ

      M_log.info("doReset():  Low Memory Recovery to: " + Runtime.getRuntime().freeMemory());

   } // doReset

   /**
    * Create a cache using the supplied name (with default settings) 
    * or get the cache out of spring or the current configured cache
    * Will proceed in this order:
    * 1) Attempt to load a bean with the name of the cache
    * 2) Attempt to load cache from caching system
    * 3) Create a new cache by this name
    * 
    * @param cacheName the name of the cache
    * @return a cache instance
    */
   private Ehcache instantiateCache(String cacheName) {
      if (M_log.isDebugEnabled())
         M_log.debug("createNewCache(String " + cacheName + ")");

      if (cacheName == null || "".equals(cacheName)) {
         throw new IllegalArgumentException("String cacheName must not be null or empty!");
      }

      // try to locate a named cache in the bean factory
      Ehcache cache = null;
      try {
         cache = (Ehcache) applicationContext.getBean(cacheName);
      } catch (BeansException e) {
         M_log.debug("Error occurred when trying to load cache from bean factory!", e);
      }

      // try to locate the cache in the cacheManager by name
      if (cache == null) {
         // if this cache name is created or already in use then we just get it
         if (!cacheManager.cacheExists(cacheName)) {
            // did not find the cache
            cacheManager.addCache(cacheName); // create a new cache
            M_log.info("Created new Cache (from default settings): " + cache);
         }
         cache = cacheManager.getEhcache(cacheName);
      }
      return cache;
   }

   public void destroyCache(String cacheName) {
      if (cacheName == null || "".equals(cacheName)) {
         throw new IllegalArgumentException("cacheName cannot be null or empty string");
      }

      cacheManager.removeCache(cacheName);
      cachesRecord.remove(cacheName);
   }




   // DEPRECATED METHODS BELOW -AZ

   /** name of mref cache bean 
    * @deprecated */
   private static final String ORG_SAKAIPROJECT_MEMORY_MEMORY_SERVICE_MREF_MAP = "org.sakaiproject.memory.MemoryService.mref_map";

   /**
    * {@inheritDoc}
    * (NotificationCache)
    * @deprecated no longer supported
    */
   public Cache newCache(String cacheName, CacheRefresher refresher, String pattern) {
      M_log.warn("deprecated method, do NOT use");
      return newCache(cacheName, refresher, null, true, false);
//      return new MemCache(this, eventTrackingService, refresher, pattern, instantiateCache(cacheName));
   }

   /**
    * {@inheritDoc}
    * (SakaiSecurity)
    * @deprecated no longer supported (used by SecurityService impl which is SakaiSecurity)
    */
   public MultiRefCache newMultiRefCache(String cacheName) {
      M_log.warn("deprecated method, do NOT use: newMultiRefCache(String cacheName)");

      MultiRefCache mrc = (MultiRefCache) cachesRecord.get(cacheName);
      if (mrc == null) {
         mrc = new MultiRefCacheImpl(this, 
               eventTrackingService, 
               instantiateCache(cacheName),
               instantiateCache(ORG_SAKAIPROJECT_MEMORY_MEMORY_SERVICE_MREF_MAP));
      }
      cachesRecord.put(cacheName, mrc);

      return mrc;
   }

   /**
    * {@inheritDoc}
    * (BaseAliasService, BaseUserDirectoryService, SiteCacheImpl)
    * @deprecated no longer supported
    */
   public Cache newCache(String cacheName, String pattern) {
      M_log.warn("deprecated method, do NOT use: newCache(String cacheName, String pattern)");
      return newCache(cacheName, null, null, true, false);
//      return new MemCache(this, eventTrackingService, pattern, instantiateCache(cacheName));
   }

   /**
    * Register as a cache user
    * 
    * @deprecated not needed with ehcache
    */
   synchronized public void registerCacher(Cacher cacher) {
      // not needed with ehcache
      M_log.warn("deprecated method that does nothing: registerCacher(Cacher cacher)");

   } // registerCacher

   /**
    * Unregister as a cache user
    * 
    * @deprecated not needed with ehcache
    */
   synchronized public void unregisterCacher(Cacher cacher) {
      // not needed with ehcache
      M_log.warn("deprecated method that does nothing: unregisterCacher(Cacher cacher)");

   } // unregisterCacher

   /**
    * {@inheritDoc}
    * 
    * @deprecated no longer supported
    */
   public Cache newCache(CacheRefresher refresher, String pattern) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    return new MemCache(this, eventTrackingService, refresher, pattern, instantiateCache(
//    "MemCache", true));
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public Cache newHardCache(CacheRefresher refresher, String pattern) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
      // return new HardCache(this, eventTrackingService(), refresher, pattern,
      // instantiateCache("HardCache", true));
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public Cache newHardCache(long sleep, String pattern) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
      // return new HardCache(this, eventTrackingService(), sleep, pattern,
      // instantiateCache("HardCache", true));
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public Cache newCache(CacheRefresher refresher, long sleep) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    return new MemCache(this, eventTrackingService, refresher, sleep, instantiateCache(
//    "MemCache", true));
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public Cache newHardCache(CacheRefresher refresher, long sleep) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
      // return new MemCache(this, eventTrackingService(), refresher, sleep,
      // instantiateCache("HardCache", true));
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public Cache newCache() {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//    return new MemCache(instantiateCache("MemCache", true), null, null);
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public Cache newHardCache() {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
      // return new HardCache(this, eventTrackingService(),
      // instantiateCache("HardCache", true));
   }

   /**
    * {@inheritDoc}
    * 
    * @deprecated
    */
   public MultiRefCache newMultiRefCache(long sleep) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
      // return new MultiRefCacheImpl(
      // this,
      // eventTrackingService(),
      // instantiateCache("MultiRefCache", true),
      // instantiateCache(
      // ORG_SAKAIPROJECT_MEMORY_MEMORY_SERVICE_MREF_MAP, false));
   }

}
