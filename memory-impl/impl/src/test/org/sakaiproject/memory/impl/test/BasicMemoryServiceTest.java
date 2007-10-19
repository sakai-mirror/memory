/**
 * BasicMemoryServiceTest.java - memory-impl - 2007 Oct 18, 2007 6:25:17 PM - azeckoski
 */

package org.sakaiproject.memory.impl.test;

import net.sf.ehcache.CacheManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.easymock.MockControl;
import org.sakaiproject.memory.api.Cache;
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

      getCachesStatus();

      // check status after adding a cache
//      status = memoryService.getStatus();
//      assertNotNull(status);
//      assertEquals(basicCachesCount + 1, cacheManager.getCacheNames().length);
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#newCache(java.lang.String, org.sakaiproject.memory.api.CacheRefresher, org.sakaiproject.memory.api.DerivedCache, boolean, boolean)}.
    */
   public void testNewCacheStringCacheRefresherDerivedCacheBooleanBoolean() {
      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#resetCachers()}.
    */
   public void testResetCachers() {
      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#doReset()}.
    */
   public void testDoReset() {
      fail("Not yet implemented");
   }

}
