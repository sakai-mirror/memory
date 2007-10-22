/**
 * MemCacheTest.java - memory-impl - 2007 Oct 18, 2007 6:28:30 PM - azeckoski
 */

package org.sakaiproject.memory.impl.test;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.memory.impl.MemCache;
import org.springframework.test.AbstractSingleSpringContextTests;


/**
 * Test case for MemCache
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class MemCacheTest extends AbstractSingleSpringContextTests {

   private static Log log = LogFactory.getLog(MemCacheTest.class);

   protected MemCache testCache = null;
   protected CacheManager cacheManager = null;
   protected int basicCachesCount = 0;

   final String CACHENAME1 = "cache 1";
   final String CACHENAME2 = "cache 2";
   final String CACHENAME3 = "cache 3";


//   private EntityManager entityManager;
//   private MockControl entityManagerControl;

   protected String[] getConfigLocations() {
      // point to the needed spring config files, must be on the classpath
      // (add component/src/webapp/WEB-INF to the build path in Eclipse),
      // they also need to be referenced in the project.xml file
      return new String[] { "ehcache-beans.xml" };
   }
   
   @Override
   protected void onSetUp() throws Exception {
      // load the spring created dao class bean from the Spring Application Context
      cacheManager = (CacheManager) applicationContext
            .getBean("org.sakaiproject.memory.api.MemoryService.cacheManager");
      if (cacheManager == null) {
         throw new NullPointerException("CacheManager could not be retrieved from spring context");
      }
      basicCachesCount = cacheManager.getCacheNames().length;

      //getCachesStatus();

      // load up any other needed spring beans

      // setup the mock objects if needed
//      entityManagerControl = MockControl.createControl(EntityManager.class);
//      entityManager = (EntityManager) entityManagerControl.getMock();

      // setup fake internal services

      // setup the test objects
      cacheManager.addCache(CACHENAME1);
      testCache = new MemCache(cacheManager.getEhcache(CACHENAME1), null, null);
   }
   
   @Override 
   protected void onTearDown() throws Exception {
      // cleanup the added cache
      cacheManager.removeCache(CACHENAME1);
   };


   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#MemCache(net.sf.ehcache.Ehcache, org.sakaiproject.memory.api.CacheRefresher, org.sakaiproject.memory.api.DerivedCache)}.
    */
   public void testMemCacheEhcacheCacheRefresherDerivedCache() {
      // make sure general creation works
      cacheManager.addCache(CACHENAME2);
      MemCache cache = new MemCache(cacheManager.getEhcache(CACHENAME2), null, null);
      assertNotNull(cache);

      // test that a cache which is not active fails
      Ehcache cache3 = new Cache(CACHENAME3, 1000, false, false, 1000, 1000);
      try {
         cache = new MemCache(cache3, null, null);
         fail("Should not have gotten here");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
      
      // test if including null causes failure
      try {
         cache = new MemCache((Ehcache)null, null, null);
         fail("Should not have gotten here");
      } catch (NullPointerException e) {
         assertNotNull(e.getMessage());
      }
      
      // cleanup
      cacheManager.removeCache(CACHENAME2);
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#put(java.lang.String, java.lang.Object)}.
    */
   public void testPutStringObject() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#containsKey(java.lang.String)}.
    */
   public void testContainsKey() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#get(java.lang.String)}.
    */
   public void testGet() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#remove(java.lang.String)}.
    */
   public void testRemove() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#clear()}.
    */
   public void testClear() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#attachDerivedCache(org.sakaiproject.memory.api.DerivedCache)}.
    */
   public void testAttachDerivedCache() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#resetCache()}.
    */
   public void testResetCache() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#getSize()}.
    */
   public void testGetSize() {
   // TODO      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#getDescription()}.
    */
   public void testGetDescription() {
   // TODO      fail("Not yet implemented");
   }

}
