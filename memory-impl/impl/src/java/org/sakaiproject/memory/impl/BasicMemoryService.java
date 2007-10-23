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

//      try {
//          get notified of events to watch for a reset
//          eventTrackingService().addObserver(this); // No longer using event tracking with cache
//      } catch (Throwable t) {
//         M_log.warn("init(): ", t);
//      }
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
      final StringBuilder buf = new StringBuilder();
      buf.append("** Memory report\n");
      buf.append("freeMemory: " + Runtime.getRuntime().freeMemory());
      buf.append(" totalMemory: ");
      buf.append(Runtime.getRuntime().totalMemory());
      buf.append(" maxMemory: ");
      buf.append(Runtime.getRuntime().maxMemory());
      buf.append("\n\n");

      List<Ehcache> allCaches = getAllCaches(true);

      // summary
      for (Ehcache cache : allCaches) {
         final long hits = cache.getStatistics().getCacheHits();
         final long misses = cache.getStatistics().getCacheMisses();
         final long total = hits + misses;
         final long hitRatio = ((total > 0) ? ((100l * hits) / total) : 0);
         buf.append(cache.getName() + ": " + " count:" + cache.getStatistics().getObjectCount()
               + " hits:" + hits + " misses:" + misses + " hit%:" + hitRatio);
         buf.append("\n");
      }

      // extended report
      buf.append("\n** Extended Cache Report\n");
      for (Object ehcache : allCaches) {
         buf.append(ehcache.toString());
         buf.append("\n");
      }

      // Iterator<Cacher> it = m_cachers.iterator();
      // while (it.hasNext())
      // {
      // Cacher cacher = (Cacher) it.next();
      // buf.append(cacher.getSize() + " in " + cacher.getDescription() + "\n");
      // }

      final String rv = buf.toString();
      M_log.info(rv);

      return rv;
   }


   public Cache newCache(String cacheName) {
      return newCache(cacheName, null, null, true, false);
   }

   public Cache newCache(String cacheName, CacheRefresher refresher, DerivedCache notifer,
         boolean distributed, boolean replicated) {
      // TODO - handle the distributed and replicated settings
      if (cacheName == null || "".equals(cacheName)) {
         throw new IllegalArgumentException("cacheName cannot be null or empty string");
      }

      M_log.warn("boolean distributed, boolean replicated not being handled yet");
      Cache c = cachesRecord.get(cacheName);
      if (c == null) {
         c = new MemCache(instantiateCache(cacheName, true), refresher, notifer);
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
    * Create a cache or get the cache out of ComponentManager
    * 
    * @param cacheName
    * @param legacyMode
    *           If true always create a new Cache. If false, cache must be defined in bean factory.
    * @return a cache instance
    */
   private Ehcache instantiateCache(String cacheName, boolean legacyMode) {
      if (M_log.isDebugEnabled())
         M_log.debug("createNewCache(String " + cacheName + ")");

      String name = cacheName;
      if (name == null || "".equals(name)) {
         throw new IllegalArgumentException("String cacheName must not be null or empty!");
      }

      // try to locate a named cache in the bean factory
      Ehcache cache = null;
      try {
         cache = (Ehcache) applicationContext.getBean(name);
      } catch (BeansException e) {
         M_log.debug("Error occurred when trying to load cache from bean factory!", e);
         cache = null;
      }

      // try to locate the cache in the cacheManager by name
      if (cache == null && cacheManager.cacheExists(name)) {
         // if this cache name is already in use but is not loaded from spring
         // then we have to assume this is a mistake -AZ
         throw new IllegalArgumentException("Tried to create a new cache with a name that is already used: " + name);
      }

      if (cache != null) {
         // found the cache
         M_log.info("Loaded Named Cache: " + cache);
         return cache;
      } else {
         // did not find the cache
         if (legacyMode) {
            cacheManager.addCache(name); // create a new cache
            cache = cacheManager.getEhcache(name);
            M_log.info("Loaded newly created Cache (from default): " + cache);
         } else {
            throw new IllegalStateException("Could not find named cache in the bean factory (and will not create one): " + name);
         }

         return cache;
      }
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
      return new MemCache(this, eventTrackingService, refresher, pattern, instantiateCache(
            cacheName, true));
   }

   /**
    * {@inheritDoc}
    * (SakaiSecurity)
    * @deprecated no longer supported (used by SecurityService impl which is SakaiSecurity)
    */
   public MultiRefCache newMultiRefCache(String cacheName) {
      M_log.warn("deprecated method, do NOT use");
      return new MultiRefCacheImpl(this, eventTrackingService, instantiateCache(cacheName, true),
            instantiateCache(ORG_SAKAIPROJECT_MEMORY_MEMORY_SERVICE_MREF_MAP, false));
   }

   /**
    * {@inheritDoc}
    * (BaseAliasService, BaseUserDirectoryService, SiteCacheImpl)
    * @deprecated no longer supported
    */
   public Cache newCache(String cacheName, String pattern) {
      return new MemCache(this, eventTrackingService, pattern, instantiateCache(cacheName, true));
   }

   /**
    * Register as a cache user
    * 
    * @deprecated not needed with ehcache
    */
   synchronized public void registerCacher(Cacher cacher) {
      // not needed with ehcache
      M_log.warn("deprecated method that does nothing");

   } // registerCacher

   /**
    * Unregister as a cache user
    * 
    * @deprecated not needed with ehcache
    */
   synchronized public void unregisterCacher(Cacher cacher) {
      // not needed with ehcache
      M_log.warn("deprecated method that does nothing");

   } // unregisterCacher

   /**
    * {@inheritDoc}
    * 
    * @deprecated no longer supported
    */
   public Cache newCache(CacheRefresher refresher, String pattern) {
      M_log.warn("deprecated method, do NOT use");
      throw new UnsupportedOperationException("deprecated method, no longer functional");
//      return new MemCache(this, eventTrackingService, refresher, pattern, instantiateCache(
//            "MemCache", true));
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
//      return new MemCache(this, eventTrackingService, refresher, sleep, instantiateCache(
//            "MemCache", true));
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
//      return new MemCache(instantiateCache("MemCache", true), null, null);
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
