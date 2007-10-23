/**
 * MemCacheTest.java - memory-impl - 2007 Oct 18, 2007 6:28:30 PM - azeckoski
 */

package org.sakaiproject.memory.impl.test;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import org.sakaiproject.event.api.Event;
import org.sakaiproject.memory.api.CacheRefresher;
import org.sakaiproject.memory.api.DerivedCache;
import org.sakaiproject.memory.impl.MemCache;
import org.springframework.test.AbstractSingleSpringContextTests;


/**
 * Test case for MemCache
 * 
 * @author Aaron Zeckoski (aaron@caret.cam.ac.uk)
 */
public class MemCacheTest extends AbstractSingleSpringContextTests {

// private static Log log = LogFactory.getLog(MemCacheTest.class);

   protected MemCache testCache = null;
   protected CacheManager cacheManager = null;
   protected int basicCachesCount = 0;

   final String CACHENAME1 = "cache 1";
   final String CACHENAME2 = "cache 2";
   final String CACHENAME3 = "cache 3";


// private EntityManager entityManager;
// private MockControl entityManagerControl;

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
//    entityManagerControl = MockControl.createControl(EntityManager.class);
//    entityManager = (EntityManager) entityManagerControl.getMock();

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
      // try putting a few things
      testCache.put("key1", "THING");

      testCache.put("key2", new Long(2));

      // try replacing the value (should work fine)
      assertEquals("THING", testCache.get("key1"));
      testCache.put("key1", "NEWTHING");
      assertEquals("NEWTHING", testCache.get("key1"));

      // test that size is increasing
      assertEquals(2, testCache.getSize());
      testCache.put("key3", "THREE");
      assertEquals("THREE", testCache.get("key3"));
      assertEquals(3, testCache.getSize());

      // test storing a null works ok
      testCache.put("key3", null);
      assertEquals(null, testCache.get("key3"));
      assertEquals(3, testCache.getSize());

      // test if key is invalid causes failure
      try {
         testCache.put(null, "THING");
         fail("Should not have gotten here");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }

      try {
         testCache.put("", "THING");
         fail("Should not have gotten here");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#containsKey(java.lang.String)}.
    */
   public void testContainsKey() {
      testCache.put("key1", "THING1");
      testCache.put("key2", null);

      assertTrue(testCache.containsKey("key1"));
      assertTrue(testCache.containsKey("key2"));
      assertFalse(testCache.containsKey("key3"));

      // these are ok but discouraged with log warning
      assertFalse(testCache.containsKey(""));
      assertFalse(testCache.containsKey(null));

      // this is disabled for now since it is only in for backwards compatibility
//    // test using invalid key causes failure
//    try {
//    assertFalse(testCache.containsKey(null));
//    fail("Should not have gotten here");
//    } catch (IllegalArgumentException e) {
//    assertNotNull(e.getMessage());
//    }
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#get(java.lang.String)}.
    */
   public void testGet() {
      Object payload = null;

      // load up test data
      Object testPayload = new Long(123);
      testCache.put("key1", testPayload);
      testCache.put("key2", null);

      // testing getting normal object
      payload = testCache.get("key1");
      assertNotNull(payload);
      assertEquals(testPayload, payload);

      // test getting a null
      payload = testCache.get("key2");
      assertNull(payload);

      // test getting invalid
      payload = testCache.get("key3");
      assertNull(payload);

      // test using invalid key causes failure
      try {
         payload = testCache.get(null);
         fail("Should not have gotten here");
      } catch (IllegalArgumentException e) {
         assertNotNull(e.getMessage());
      }
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#remove(java.lang.String)}.
    */
   public void testRemove() {
      // load up some data
      testCache.put("key1", "THING1");
      testCache.put("key2", null);

      // test removing normal stuff
      assertTrue(testCache.containsKey("key1"));
      testCache.remove("key1");
      assertFalse(testCache.containsKey("key1"));

      // test removing null cached item
      assertTrue(testCache.containsKey("key2"));
      testCache.remove("key2");
      assertFalse(testCache.containsKey("key2"));

      // test removing invalid item (should be ok)
      assertFalse(testCache.containsKey("key3"));
      testCache.remove("key3");

      // this is disabled for now since it is only in for backwards compatibility
//    // test using invalid key causes failure
//    try {
//    testCache.remove(null);
//    fail("Should not have gotten here");
//    } catch (IllegalArgumentException e) {
//    assertNotNull(e.getMessage());
//    }
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#attachDerivedCache(org.sakaiproject.memory.api.DerivedCache)}.
    */
   public void testAttachDerivedCache() {
      // generate a cache event listener class for testing
      final String[] status = new String[1];
      DerivedCache cacheEventListener = new DerivedCache() {
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

      // attach the listener
      testCache.attachDerivedCache(cacheEventListener);

      // check the listener works
      assertEquals(0, testCache.getSize());
      assertNull(status[0]);
      testCache.put("3.1", "blah");
      assertEquals("PUT", status[0]);
      testCache.remove("3.1");
      assertEquals("REMOVE", status[0]);
      testCache.clear();
      assertEquals("CLEAR", status[0]);
      assertEquals(0, testCache.getSize());

      // clear the listener
      testCache.attachDerivedCache(null);

      // test it is not listening anymore
      status[0] = null;
      assertNull(status[0]);
      testCache.put("3.2", "blah");
      assertNull(status[0]);
      testCache.remove("3.2");
      assertNull(status[0]);
      testCache.clear();
      assertNull(status[0]);
      assertEquals(0, testCache.getSize());
   }

   /**
    * Test method for {@link MemCache#attachLoader(org.sakaiproject.memory.api.CacheRefresher)}
    */
   public void testAttachLoader() {
      // generate a cache loader class for testing
      CacheRefresher cacheLoader = new CacheRefresher() {
         public Object refresh(Object key, Object oldValue, Event event) {
            if (key.equals("TESTKEY")) {
               return "UPDATED";
            }
            return null;
         }
      };

      // attach the loader
      testCache.attachLoader(cacheLoader);

      // test that the loader works
      assertEquals(0, testCache.getSize());
      assertNull(testCache.get("INVALID"));
      assertEquals(0, testCache.getSize());
      assertEquals("UPDATED", testCache.get("TESTKEY"));
      assertEquals(1, testCache.getSize());

      // reset cache
      testCache.resetCache();

      // clear the loader
      testCache.attachLoader(null);

      // test that the loader does not activate
      assertEquals(0, testCache.getSize());
      assertEquals(null, testCache.get("TESTKEY"));
      assertEquals(0, testCache.getSize());
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#resetCache()}.
    */
   public void testResetCache() {
      assertEquals(0, testCache.getSize());

      // try it on an empty cache
      testCache.resetCache();

      // load up some data
      testCache.put("key1", "THING1");
      testCache.put("key2", null);

      assertEquals(2, testCache.getSize());

      // now on one with items
      testCache.resetCache();
      assertEquals(0, testCache.getSize());
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#getSize()}.
    */
   public void testGetSize() {
      assertEquals(0, testCache.getSize());

      // load up some data
      testCache.put("key1", "THING1");
      testCache.put("key2", null);

      assertEquals(2, testCache.getSize());

      // not much to test here
   }

   /**
    * Test method for {@link org.sakaiproject.memory.impl.MemCache#getDescription()}.
    */
   public void testGetDescription() {
      // just see if it works
      assertNotNull(testCache.getDescription());
   }

}
