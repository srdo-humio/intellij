/*
 * Copyright 2022 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.blaze.base.sync;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.idea.blaze.base.logging.utils.SyncStats;
import com.google.idea.blaze.base.model.primitives.LanguageClass;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.model.primitives.WorkspaceType;
import com.google.idea.blaze.base.scope.BlazeContext;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ShouldForceFullSyncTest extends BlazeSyncIntegrationTestCase {

  private BlazeContext context;
  private static final String DEFAULT_PROJECT_VIEW =
      "directories:\n  src/main/java/com/google\nworkspace_type: android";

  @Before
  public void setup() {
    registerExtension(BlazeSyncPlugin.EP_NAME, new MockBlazeAndroidSyncPlugin());
    workspace.createFile(
        new WorkspacePath("src/main/java/com/google/Source.java"),
        "package com.google;",
        "public class Source {}");
    context = BlazeContext.create();
  }

  @After
  public void wrapup() {
    context.endScope();
  }

  @Test
  public void testShouldForceFullSync_noNewLanguageAdded() {
    setProjectView(DEFAULT_PROJECT_VIEW);
    runFullSync();
    assertThat(shouldForceFullSync(SyncMode.INCREMENTAL, context)).isFalse();
  }

  @Test
  public void testShouldForceFullSync_newLanguageAdded() {
    setProjectView(DEFAULT_PROJECT_VIEW);
    runFullSync();
    setProjectView(DEFAULT_PROJECT_VIEW, "additional_languages:", "  kotlin");
    assertThat(shouldForceFullSync(SyncMode.INCREMENTAL, context)).isTrue();
    assertThat(shouldForceFullSync(SyncMode.NO_BUILD, context)).isFalse();
  }

  @Test
  public void testShouldForceFullSync_additionalLanguageRemoved() {
    setProjectView(DEFAULT_PROJECT_VIEW, "additional_languages:", "  kotlin");
    runFullSync();
    setProjectView(DEFAULT_PROJECT_VIEW);
    assertThat(shouldForceFullSync(SyncMode.INCREMENTAL, context)).isFalse();
    assertThat(shouldForceFullSync(SyncMode.NO_BUILD, context)).isFalse();
  }

  private void runFullSync() {
    BlazeSyncParams syncParams =
        BlazeSyncParams.builder()
            .setTitle("Full Sync")
            .setSyncMode(SyncMode.FULL)
            .setSyncOrigin("test")
            .build();
    runBlazeSync(syncParams);
    errorCollector.assertNoIssues();
    SyncStats syncStats = Iterables.getLast(getSyncStats());
    assertThat(syncStats.syncMode()).isEqualTo(SyncMode.FULL);
    assertThat(syncStats.syncResult()).isEqualTo(SyncResult.SUCCESS);
  }

  private static class MockBlazeAndroidSyncPlugin implements BlazeSyncPlugin {
    @Override
    public ImmutableList<WorkspaceType> getSupportedWorkspaceTypes() {
      return ImmutableList.of(WorkspaceType.ANDROID);
    }

    @Override
    public Set<LanguageClass> getSupportedLanguagesInWorkspace(WorkspaceType workspaceType) {
      return ImmutableSet.of(LanguageClass.ANDROID, LanguageClass.JAVA, LanguageClass.KOTLIN);
    }
  }
}
