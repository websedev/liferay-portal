/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portlet.wiki.model;

import com.liferay.portal.ModelListenerException;
import com.liferay.portal.kernel.test.ExecutionTestListeners;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceTestUtil;
import com.liferay.portal.test.EnvironmentExecutionTestListener;
import com.liferay.portal.test.LiferayIntegrationJUnitTestRunner;
import com.liferay.portal.test.Sync;
import com.liferay.portal.test.SynchronousDestinationExecutionTestListener;
import com.liferay.portal.util.GroupTestUtil;
import com.liferay.portal.util.TestPropsValues;
import com.liferay.portlet.wiki.service.WikiPageLocalServiceUtil;
import com.liferay.portlet.wiki.util.WikiTestUtil;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Tomas Polesovsky
 */
@ExecutionTestListeners(listeners = {
	EnvironmentExecutionTestListener.class,
	SynchronousDestinationExecutionTestListener.class
})
@RunWith(LiferayIntegrationJUnitTestRunner.class)
@Sync
public class CycleDetectorWikiPageModelListenerTest {

	@Before
	public void setUp() throws Exception {
		ServiceTestUtil.setUser(TestPropsValues.getUser());

		_group = GroupTestUtil.addGroup();

		_node = WikiTestUtil.addNode(
			TestPropsValues.getUserId(), _group.getGroupId(),
			ServiceTestUtil.randomString(), ServiceTestUtil.randomString(50));
	}

	@After
	public void tearDown() throws Exception {
		GroupLocalServiceUtil.deleteGroup(_group);
	}

	@Test
	public void testCycle() throws Exception {
		WikiPage wikiPage1 = WikiPageLocalServiceUtil.addPage(
			TestPropsValues.getUserId(), _node.getNodeId(), "Title1",
			StringPool.BLANK, StringPool.BLANK, true, new ServiceContext());

		WikiPage wikiPage2 = WikiPageLocalServiceUtil.addPage(
			TestPropsValues.getUserId(), _node.getNodeId(), "Title2",
			StringPool.BLANK, StringPool.BLANK, true, new ServiceContext());

		WikiPage wikiPage3 = WikiPageLocalServiceUtil.addPage(
			TestPropsValues.getUserId(), _node.getNodeId(), "Title3",
			StringPool.BLANK, StringPool.BLANK, true, new ServiceContext());

		try {
			wikiPage1.setParentTitle("Title2");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage1);

			wikiPage2.setParentTitle("Title3");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage2);

			wikiPage3.setParentTitle("Title1");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage3);

			Assert.fail();
		}
		catch (ModelListenerException mle) {
			String message = mle.getMessage();

			Assert.assertEquals(
				"Unable to update wiki page Title3 because a cycle was " +
					"detected",
				message);
		}

		try {
			wikiPage3.setParentTitle("Other");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage3);

			wikiPage1.setTitle("Other");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage1);

			Assert.fail();
		}
		catch (ModelListenerException mle) {
			String message = mle.getMessage();

			Assert.assertEquals(
				"Unable to update wiki page Other because a cycle was detected",
				message);
		}
	}

	@Test
	public void testSelfReferencingWikiPage() throws Exception {
		String title = "Cycling Page";

		String parentTitle = title;

		try {
			WikiPageLocalServiceUtil.addPage(
				TestPropsValues.getUserId(), _node.getNodeId(), title,
				WikiPageConstants.VERSION_DEFAULT, StringPool.BLANK,
				StringPool.BLANK, false, "creole", false, parentTitle,
				StringPool.BLANK, new ServiceContext());

			Assert.fail();
		}
		catch (ModelListenerException mle) {
			String message = mle.getMessage();

			Assert.assertEquals(
				"Unable to create wiki page " + title +
					" because a cycle was detected",
				message);
		}
	}

	@Test
	public void testSelfReferencingWikiPageDelayedSet() throws Exception {
		WikiPage wikiPage1 = WikiPageLocalServiceUtil.addPage(
			TestPropsValues.getUserId(), _node.getNodeId(), "Title",
			StringPool.BLANK, StringPool.BLANK, true, new ServiceContext());

		try {
			wikiPage1.setParentTitle("Title");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage1);

			Assert.fail();
		}
		catch (ModelListenerException mle) {
			String message = mle.getMessage();

			Assert.assertEquals(
				"Unable to update wiki page Title because a cycle was detected",
				message);
		}

		try {
			wikiPage1.setParentTitle("Other Title");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage1);

			wikiPage1.setTitle("Other Title");

			WikiPageLocalServiceUtil.updateWikiPage(wikiPage1);

			Assert.fail();
		}
		catch (ModelListenerException mle) {
			String message = mle.getMessage();

			Assert.assertEquals(
				"Unable to update wiki page Other Title because a cycle was " +
					"detected",
				message);
		}
	}

	private Group _group;

	private WikiNode _node;

}