/**
 * BasicMemoryServiceTest.java - memory-impl - 2007 Oct 18, 2007 6:25:17 PM - azeckoski
 */

package org.sakaiproject.memory.impl.test;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.event.api.Event;
import org.sakaiproject.memory.api.Cache;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.DerivedCache;
import org.sakaiproject.memory.impl.BasicMemoryService;
import org.springframework.test.AbstractSingleSpringContextTests;


/**
 * Testing the MemoryService
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class BasicMemoryServiceTest extends AbstractSingleSpringContextTests {

   private static Log log = LogFactory.getLog(BasicMemoryServiceTest.class);

   protected BasicMemoryService memoryService = null;
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

      // create and setup the object to be tested
      memoryService = new BasicMemoryService();
      memoryService.setCacheManager(cacheManager);
      memoryService.setApplicationContext(applicationContext);
   }

   /**
    * 
    */
   private void getCachesStatus() {
      String[] caches = cacheManager.getCacheNames();
      StringBuilder sb = new StringBuilder();
      sb.append("Loaded up the cache manager with "+basicCachesCount+" caches:\n");
      for (int i = 0; i < caches.length; i++) {
         sb.append("Cache " + i + ": " + caches[i] + ", size:" + cacheManager.getCache(caches[i]).getSize() + "\n");
      }
      log.info(sb.toString());
   };

   /**
    * ADD unit tests below here, use testMethod as the name of the unit test, Note that if a method
    * is overloaded you should include the arguments in the test name like so: testMethodClassInt
    * (for method(Class, int);
    */



   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#init()}.
    */
   public void testInit() {
      // just make sure this does not cause an exception
      memoryService.init();

      memoryService.setCacheManager(null);

      // test that the cachemanager check works
      try {
         memoryService.init();
         fail("should have thrown exception");
      } catch (IllegalStateException e) {
         assertNotNull(e.getMessage());
      }
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#destroy()}.
    */
   public void testDestroy() {
      // just make sure this does not cause an exception
      memoryService.destroy();
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#getAvailableMemory()}.
    */
   public void testGetAvailableMemory() {
      // just make sure this does not cause an exception
      long mem = memoryService.getAvailableMemory();
      assertTrue(mem > 0);
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#getStatus()}.
    */
   public void testGetStatus() {
      String status = null;

      // check basic status with no caches
      status = memoryService.getStatus();
      assertNotNull(status);
      assertEquals(basicCachesCount, cacheManager.getCacheNames().length);
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#newCache(java.lang.String)}.
    */
   public void testNewCacheString() {

      // add a new cache
      Cache cache1 = memoryService.newCache(CACHENAME1);
      assertNotNull(cache1);
      cache1.put("1.1", "thing");
      assertEquals(1, cache1.getSize());

      // check status after adding a cache
      getCachesStatus();

      // make sure you can add a cache again (which is just fetching the cache)
      Cache cacheOne = memoryService.newCache(CACHENAME1);
      assertNotNull(cache1);
      assertEquals(cache1, cacheOne);

      // make sure creating a cache with no name fails
      try {
         memoryService.newCache("");
         fail("should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
   }


   public void testDestroyCache() {
      // try to destroy a cache
      memoryService.destroyCache(CACHENAME1);

      // try to destroy one that does not exist (should not fail)
      memoryService.destroyCache(CACHENAME2);

      // make sure bad argument causes failure
      try {
         memoryService.destroyCache("");
         fail("should have thrown exception");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }      
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#newCache(java.lang.String, org.sakaiproject.memory.api.CacheRefresher, org.sakaiproject.memory.api.DerivedCache, boolean, boolean)}.
    */
   public void testNewCacheStringCacheRefresherDerivedCacheBooleanBoolean() {

      // test creating the cache with normal options
      Cache cache1 = memoryService.newCache(CACHENAME1, null, null, true, false);
      assertNotNull(cache1);
      cache1.put("2.1", "thing");
      cache1.put("2.2", "thing");

      assertEquals(2, cache1.getSize());

      // test adding in the listeners
      CacheRefresher refresher = new CacheRefresher() {
         public Object refresh(Object key, Object oldValue, Event event) {
            if (key.equals("TESTKEY")) {
               return "UPDATED";
            }
            return null;
         }
      };

      // test creating a cache with a refresher and see if it works
      Cache cache2 = memoryService.newCache(CACHENAME2, refresher, null, true, false);
      assertNotNull(cache2);
      assertEquals(0, cache2.getSize());
      assertNull(cache2.get("INVALID"));
      assertEquals("UPDATED", cache2.get("TESTKEY"));
      assertEquals(1, cache2.getSize());

      // test adding in the notifier
      final String[] status = new String[1];
      DerivedCache eventListener = new DerivedCache() {
         public void notifyCacheClear() {
            status[0] = "CLEAR";
         }
         public void notifyCachePut(String key, Object payload) {
            status[0] = "PUT";
         }
         public void notifyCacheRemove(String key, Object payload) {
            status[0] = "REMOVE";
         }
      };

      Cache cache3 = memoryService.newCache(CACHENAME3, null, eventListener, true, false);
      assertNotNull(cache3);
      assertEquals(0, cache3.getSize());
      assertNull(status[0]);
      cache3.put("3.1", "blah");
      assertEquals("PUT", status[0]);
      cache3.remove("3.1");
      assertEquals("REMOVE", status[0]);
      cache3.clear();
      assertEquals("CLEAR", status[0]);
      assertEquals(0, cache3.getSize());

      getCachesStatus();

      // cleanups
      memoryService.destroyCache(CACHENAME1);
      memoryService.destroyCache(CACHENAME2);
      memoryService.destroyCache(CACHENAME3);

      // TODO check the booleans once they are actually functional
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#doReset()}.
    */
   public void testDoReset() {
      // create a cache and put some stuff in it
      Cache cache1 = memoryService.newCache(CACHENAME1);
      assertNotNull(cache1);
      cache1.put("1.1", "thing");
      cache1.put("1.2", "thing");
      assertEquals(2, cache1.getSize());

      memoryService.doReset();

      assertEquals(0, cache1.getSize());
      memoryService.destroyCache(CACHENAME1);
   }

}
