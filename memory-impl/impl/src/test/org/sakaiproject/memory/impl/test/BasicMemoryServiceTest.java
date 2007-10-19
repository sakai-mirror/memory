/**
 * BasicMemoryServiceTest.java - memory-impl - 2007 Oct 18, 2007 6:25:17 PM - azeckoski
 */

package org.sakaiproject.memory.impl.test;

import net.sf.ehcache.CacheManager;

import org.easymock.MockControl;
import org.sakaiproject.memory.impl.BasicMemoryService;
import org.springframework.test.AbstractTransactionalSpringContextTests;

import junit.framework.TestCase;

/**
 * Testing the MemoryService
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class BasicMemoryServiceTest extends AbstractTransactionalSpringContextTests {

   protected BasicMemoryService memoryService = null;
   protected CacheManager cacheManager = null;

//   private EntityManager entityManager;
//   private MockControl entityManagerControl;

   protected String[] getConfigLocations() {
      // point to the needed spring config files, must be on the classpath
      // (add component/src/webapp/WEB-INF to the build path in Eclipse),
      // they also need to be referenced in the project.xml file
      return new String[] { "ehcache-beans.xml" };
   }

   // run this before each test starts
   protected void onSetUpBeforeTransaction() throws Exception {
      // load the spring created dao class bean from the Spring Application Context
      cacheManager = (CacheManager) applicationContext
            .getBean("org.sakaiproject.memory.api.MemoryService.cacheManager");
      if (cacheManager == null) {
         throw new NullPointerException("CacheManager could not be retrieved from spring context");
      }

      // load up any other needed spring beans

      // setup the mock objects if needed
//      entityManagerControl = MockControl.createControl(EntityManager.class);
//      entityManager = (EntityManager) entityManagerControl.getMock();

      // setup fake internal services

      // create and setup the object to be tested
      memoryService = new BasicMemoryService();
      memoryService.setCacheManager(cacheManager);

   }

   // run this before each test starts and as part of the transaction
   protected void onSetUpInTransaction() {
      // preload additional data if desired
   }

   /**
    * ADD unit tests below here, use testMethod as the name of the unit test, Note that if a method
    * is overloaded you should include the arguments in the test name like so: testMethodClassInt
    * (for method(Class, int);
    */



   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#init()}.
    */
   public void testInit() {
      // this should be fine
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
      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#getAvailableMemory()}.
    */
   public void testGetAvailableMemory() {
      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#getStatus()}.
    */
   public void testGetStatus() {
      fail("Not yet implemented");
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.BasicMemoryService#newCache(java.lang.String)}.
    */
   public void testNewCacheString() {
      fail("Not yet implemented");
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
