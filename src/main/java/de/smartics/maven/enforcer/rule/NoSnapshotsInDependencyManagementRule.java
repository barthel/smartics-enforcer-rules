/*
 * Copyright 2011-2016 smartics, Kronseder & Reiner GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.smartics.maven.enforcer.rule;

import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Checks that the dependency management block in a POM contains no snapshot
 * dependencies.
 */
public class NoSnapshotsInDependencyManagementRule implements EnforcerRule
{

  // ********************************* Fields *********************************

  // --- constants ------------------------------------------------------------

  // --- members --------------------------------------------------------------

  /**
   * Usually this rule should only be enforced on projects with a release
   * version.
   */
  private boolean onlyWhenRelease = true;

  // ****************************** Initializer *******************************

  // ****************************** Constructors ******************************

  // ****************************** Inner Classes *****************************

  // ********************************* Methods ********************************

  // --- init -----------------------------------------------------------------

  // --- get&set --------------------------------------------------------------

  // --- business -------------------------------------------------------------

  /**
   * {@inheritDoc}
   */
  public void execute(final EnforcerRuleHelper helper)
    throws EnforcerRuleException
  {
    final Log log = helper.getLog();

    try
    {
      final MavenProject project = (MavenProject) helper.evaluate("${project}");

      final boolean isSnapshot = project.getArtifact().isSnapshot();
      if (onlyWhenRelease && isSnapshot)
      {
        log.info(getCacheId() + ": Skipping since not a release.");
        return;
      }

      final DependencyManagement dependencyManagement =
          project.getModel().getDependencyManagement();
      if (dependencyManagement == null)
      {
        log.debug(getCacheId() + ": No dependency management block found.");
        return;
      }

      final List<Dependency> dependencies =
          dependencyManagement.getDependencies();
      if (dependencies == null || dependencies.isEmpty())
      {
        log.debug(getCacheId()
                  + ": No dependencies in dependency management block found.");
        return;
      }

      checkDependenciesForSnapshots(log, dependencies);
    }
    catch (final ExpressionEvaluationException e)
    {
      throw new EnforcerRuleException("Unable to evaluate expression '"
                                      + e.getLocalizedMessage() + "'.", e);
    }
  }

  private void checkDependenciesForSnapshots(final Log log,
      final List<Dependency> dependencies) throws EnforcerRuleException
  {
    final StringBuilder buffer = new StringBuilder();
    for (final Dependency dependency : dependencies)
    {
      final String version = dependency.getVersion();

      if (isSnapshot(version))
      {
        buffer.append("\n  ").append(dependency);
      }
      else
      {
        log.debug("  Not a SNAPSHOT: " + dependency);
      }
    }

    if (buffer.length() > 0)
    {
      throw new EnforcerRuleException(
          "Dependency Management contains SNAPSHOTS:" + buffer.toString()
              + "\n Please remove all SNAPSHOT dependencies!");
    }
  }

  private static boolean isSnapshot(final String version)
  {
    return StringUtils.isNotBlank(version) && version.endsWith("-SNAPSHOT");
  }

  /**
   * {@inheritDoc}
   */
  public boolean isCacheable()
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public boolean isResultValid(final EnforcerRule cachedRule)
  {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  public String getCacheId()
  {
    return "noSnapshotsInDependencyManagement";
  }

  // --- object basics --------------------------------------------------------

}
