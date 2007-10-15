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

package org.sakaiproject.memory.api;

/**
 * <p>
 * Cacher is an interface for any object that uses memory caches.
 * </p>
 * <p>
 * Cachers may be asked to clear their caches to free up memory or re-sync with external stores.
 * </p>
 */
public interface Cacher {

   /**
	 * Clear out all entries from this cache
	 */
	void resetCache();

	/**
	 * Return the size of the cacher - indicating how much memory in use.
	 * 
	 * @return Number of objects in this cache
	 */
	long getSize();

	/**
	 * Return a description of the cacher.
	 * 
	 * @return The cacher's description.
	 */
	String getDescription();

}
