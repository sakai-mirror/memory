/**********************************************************************************
 * $URL$
 * $Id$
 ***********************************************************************************
 *
 * Copyright (c) 2003, 2004, 2005, 2006, 2007 The Sakai Foundation.
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.distribution.ReplicationControl;

import org.sakaiproject.memory.api.MultipleReferenceCache;
import org.sakaiproject.memory.exception.ObjectNotCachedException;
import org.sakaiproject.memory.impl.util.DependentPayload;

/**
 * A cache, where the cached object is depended on by others
 * When this object is invalidated, its own list of references needs to be invalidated
 * 
 * @author ieb
 */
public class ObjectIsDependedOnByOthersCache implements MultipleReferenceCache
{


	private Cache cache;

	private Map<Object, Map<Object, Object>> dependentKeys = new ConcurrentHashMap<Object, Map<Object, Object>>();

	/**
	 * If true, invalidation will navigate the tree
	 */
	private boolean recursive = false;

	/**
	 * If true, all cluster invalidation messages will be sent, if false, only
	 * the root of the invalidation will be sent
	 */
	private boolean propagateTree = false;

	/**
	 * 
	 */
	public ObjectIsDependedOnByOthersCache()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#exists(java.lang.Object)
	 */
	public boolean exists(Object key)
	{
		return cache.isKeyInCache(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#get(java.lang.Object)
	 */
	public Object get(Object key) throws ObjectNotCachedException
	{
		Element e = cache.get(key);
		if (e == null)
		{
			throw new ObjectNotCachedException("Cache miss " + key);
		}
		DependentPayload p = (DependentPayload) e.getObjectValue();

		return p.payLoadObject;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#put(java.lang.Object,
	 *      java.lang.Object[], java.lang.Object)
	 */
	public void put(Object key, Object[] dependentKeys, Object payload)
	{
		Element e = cache.get(key);
		boolean replace = false;
		if (e != null)
		{

			if (payload == null)
			{
				replace = (e.getObjectValue() != null);
			}
			else if (!payload.equals(e.getObjectValue()))
			{
				replace = true;
			}
		}
		// the object needs to be replaced
		if (replace)
		{
			try
			{
				remove(key);
			}
			catch (ObjectNotCachedException e1)
			{

			}
			// save the object, if this needs to replicate it will need the keys
			e = new Element(key, new DependentPayload(payload, dependentKeys));
			cache.put(e);
		} else {
			Object o = e.getObjectValue();
			if ( o instanceof DependentPayload ) {
				DependentPayload p = (DependentPayload) o;
				p.dependsOnThis = dependentKeys;
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.memory.api.MultipleReferenceCache#remove(java.lang.Object)
	 */
	public void remove(Object key) throws ObjectNotCachedException
	{
		internalRemove(key, 0);
	}

	private void internalRemove(Object key, int level)
	{

		Element e = cache.get(key);
		cache.remove(key);
		if (!propagateTree && level == 0)
		{
			ReplicationControl.disableCluster();
		}
		try
		{
			DependentPayload p = (DependentPayload) e.getObjectValue();
			if (p != null && p.dependsOnThis != null)
			{
				// remove everything that depends on this
				for (Object dkey : p.dependsOnThis)
				{
					// if the tree is recursive, recurse down all branches.
					if (recursive)
					{
						internalRemove(key, level + 1);
					}
					else
					{
						cache.remove(dkey);
					}
				}
			}
		}
		finally
		{
			if (!propagateTree && level == 0)
			{
				ReplicationControl.enableCluster();
			}

		}

	}

	/**
	 * @return the cache
	 */
	public Cache getCache()
	{
		return cache;
	}

	/**
	 * @param cache
	 *        the cache to set
	 */
	public void setCache(Cache cache)
	{
		this.cache = cache;
	}

	/**
	 * @return the propagateTree
	 */
	public boolean isPropagateTree()
	{
		return propagateTree;
	}

	/**
	 * @param propagateTree
	 *        the propagateTree to set
	 */
	public void setPropagateTree(boolean propagateTree)
	{
		this.propagateTree = propagateTree;
	}

	/**
	 * @return the recursive
	 */
	public boolean isRecursive()
	{
		return recursive;
	}

	/**
	 * @param recursive
	 *        the recursive to set
	 */
	public void setRecursive(boolean recursive)
	{
		this.recursive = recursive;
	}

}
