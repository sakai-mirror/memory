/**
 * CacheConfig.java - memory-api - 2007 Nov 2, 2007 5:05:46 PM - azeckoski
 */

package org.sakaiproject.memory.api;


/**
 * Configuration settings for a cache, use this when creating a new cache<br/>
 * All arguments are optional and may be left null
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class CacheConfig {

   private CacheRefresher loader;
   private DerivedCache notifer;
   private Boolean distributed;
   private Boolean replicated;
   private Boolean blocking;

   public CacheConfig() {
   }

   public CacheConfig(CacheRefresher loader, DerivedCache notifer) {
      this.loader = loader;
      this.notifer = notifer;
   }

   public CacheConfig(CacheRefresher loader, DerivedCache notifer, Boolean distributed,
         Boolean replicated, Boolean blocking) {
      this.loader = loader;
      this.notifer = notifer;
      this.distributed = distributed;
      this.replicated = replicated;
      this.blocking = blocking;
   }


   /**
    * @see #setLoader(CacheRefresher)
    */
   public CacheRefresher getLoader() {
      return loader;
   }

   /**
    * @param loader An object that will handle loading of entries which are not found,
    * this allows the cache to populate itself when items are requested, in other words,
    * when an item is requested from the cache with a certain key, this loader will be
    * called if the item cannot be found and may load an item into the cache at that time
    * (read through cache), leave this null to set no loader
    */
   public void setLoader(CacheRefresher loader) {
      this.loader = loader;
   }
   
   /**
    * @see #setNotifer(DerivedCache)
    */
   public DerivedCache getNotifer() {
      return notifer;
   }
   
   /**
    * @param notifer An object that will handle notifications which are a result of cache changes,
    * this allows the developer to take actions when things change in the cache,
    * leave this null for no notifications
    */
   public void setNotifer(DerivedCache notifer) {
      this.notifer = notifer;
   }
   
   /**
    * @see #setDistributed(Boolean)
    */
   public Boolean getDistributed() {
      return distributed;
   }
   
   /**
    * @param distributed if true this cache will be distributed across the cluster, 
    * otherwise it is local to the current server only,
    * if null then use the default setting for this Sakai system
    */
   public void setDistributed(Boolean distributed) {
      this.distributed = distributed;
   }
   
   /**
    * @see #setReplicated(Boolean)
    */
   public Boolean getReplicated() {
      return replicated;
   }
   
   /**
    * @param replicated if true this cache will replicate cached objects across the cluster nodes, 
    * otherwise the cache will just expire entries in the other caches and they will have to reload on that cluster node,
    * if null then use the default setting for this Sakai system
    */
   public void setReplicated(Boolean replicated) {
      this.replicated = replicated;
   }
   
   /**
    * @see #setBlocking(Boolean)
    */
   public Boolean getBlocking() {
      return blocking;
   }
   
   /**
    * @param blocking if ture then this cache will be a blocking cache, that means that when a call comes into
    * the cache to retrieve an element, the cache will block other calls until the element can be retrieved,
    * if null then use the default setting for this Sakai system
    * <b>WARNING:</b> you could be trapped in lock forever if your cache is not being filled with real values
    * by something, be very careful about using this cache type if you can have cache misses
    */
   public void setBlocking(Boolean blocking) {
      this.blocking = blocking;
   }

}
